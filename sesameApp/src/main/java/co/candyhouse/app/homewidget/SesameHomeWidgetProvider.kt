package co.candyhouse.app.homewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import co.candyhouse.app.R
import co.candyhouse.app.SesameApp
import co.candyhouse.app.connecteddevice.SesameConnectedDeviceService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SesameHomeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { ids.forEach { update(context, it) } }
    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { HomeWidgetStore(context).delete(it); feedback.remove(it) }
        if (SesameConnectedDeviceService.live != null) SesameConnectedDeviceService.sync(context)
    }
    override fun onRestored(context: Context, oldIds: IntArray, newIds: IntArray) {
        val store = HomeWidgetStore(context)
        oldIds.zip(newIds).forEach { (old, new) -> store.device(old)?.let { store.save(new, it) }; store.delete(old) }
        newIds.forEach { update(context, it) }
    }
    companion object {
        const val ACTION = "co.candyhouse.app.HOME_WIDGET_TOGGLE"
        const val EXTRA_ID = "homeWidgetId"
        private data class Feedback(val text: String, val expires: Long)
        private val feedback = mutableMapOf<Int, Feedback>()
        private var refreshJob: Job? = null
        fun ids(context: Context): IntArray = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, SesameHomeWidgetProvider::class.java))
        fun hasTargets(context: Context): Boolean {
            val app = context.applicationContext as SesameApp
            val store = HomeWidgetStore(context)
            return ids(context).any { id -> store.device(id)?.let { app.ble.device(it) }?.let(HomeWidgetStore::supports) == true }
        }
        /** BLE snapshots include mechanical changes and disconnections. Coalesce radio events. */
        fun refresh(context: Context) {
            if (refreshJob?.isActive == true) return
            val app = context.applicationContext as SesameApp
            refreshJob = app.runtimeScope.launch {
                delay(250)
                ids(app).forEach { update(app, it) }
            }
        }
        fun update(context: Context, id: Int, message: String? = null) {
            val uuid = HomeWidgetStore(context).device(id)
            val app = context.applicationContext as SesameApp
            val device = uuid?.let { app.ble.device(it) }
            if (message != null) {
                val entry = Feedback(message, SystemClock.elapsedRealtime() + if (message == context.getString(R.string.home_widget_busy)) 35_000 else 8_000)
                feedback[id] = entry
                app.runtimeScope.launch {
                    delay((entry.expires - SystemClock.elapsedRealtime()).coerceAtLeast(0))
                    if (feedback[id] === entry) {
                        feedback.remove(id)
                        if (id in ids(app)) update(app, id)
                    }
                }
            }
            val currentFeedback = feedback[id]?.takeIf { SystemClock.elapsedRealtime() < it.expires }?.text
            val state = HomeWidgetLockState.from(device?.deviceStatus)
            val stateText = context.getString(when (state) {
                HomeWidgetLockState.LOCKED -> R.string.home_widget_locked
                HomeWidgetLockState.UNLOCKED -> R.string.home_widget_unlocked
                HomeWidgetLockState.UNKNOWN -> R.string.home_widget_unknown
            })
            val savedName = uuid?.let { app.getSharedPreferences("${app.packageName}_preferences", 0).getString(it.lowercase(), null) }
            val views = RemoteViews(context.packageName, R.layout.sesame_home_widget).apply {
                setTextViewText(R.id.home_widget_name, device?.let { app.ble.deviceName(it) } ?: savedName ?: "SESAME")
                setTextViewText(R.id.home_widget_state, stateText)
                setViewVisibility(R.id.home_widget_state, if (uuid == null) View.GONE else View.VISIBLE)
                setImageViewResource(R.id.home_widget_icon, when (state) {
                    HomeWidgetLockState.LOCKED -> R.drawable.home_widget_locked
                    HomeWidgetLockState.UNLOCKED -> R.drawable.home_widget_unlocked
                    HomeWidgetLockState.UNKNOWN -> R.drawable.home_widget_key
                })
                setTextViewText(R.id.home_widget_status, currentFeedback ?: context.getString(if (uuid == null) R.string.home_widget_unconfigured else R.string.home_widget_idle))
                if (uuid != null) {
                    val intent = Intent(context, SesameConnectedDeviceService::class.java).setAction(ACTION)
                        .setData(Uri.parse("sesame-home-widget://toggle/$id"))
                        .putExtra(EXTRA_ID, id).putExtra("deviceUUID", uuid).putExtra("operation", "toggle")
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    val pending = if (Build.VERSION.SDK_INT >= 26) PendingIntent.getForegroundService(context, id, intent, flags)
                        else PendingIntent.getService(context, id, intent, flags)
                    setOnClickPendingIntent(R.id.home_widget_root, pending)
                }
            }
            AppWidgetManager.getInstance(context).updateAppWidget(id, views)
        }
        fun status(context: Context, uuid: String, text: String) {
            val store = HomeWidgetStore(context)
            ids(context).filter { store.device(it).equals(uuid, true) }.forEach { update(context, it, text) }
        }
    }
}
