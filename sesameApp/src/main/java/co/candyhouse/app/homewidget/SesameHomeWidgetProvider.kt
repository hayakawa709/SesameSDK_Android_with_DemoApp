package co.candyhouse.app.homewidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import co.candyhouse.app.R
import co.candyhouse.app.SesameApp
import co.candyhouse.app.connecteddevice.SesameConnectedDeviceService

class SesameHomeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { ids.forEach { update(context, it) } }
    override fun onDeleted(context: Context, ids: IntArray) { ids.forEach { HomeWidgetStore(context).delete(it) } }
    override fun onRestored(context: Context, oldIds: IntArray, newIds: IntArray) {
        val store = HomeWidgetStore(context)
        oldIds.zip(newIds).forEach { (old, new) -> store.device(old)?.let { store.save(new, it) }; store.delete(old) }
        newIds.forEach { update(context, it) }
    }
    companion object {
        const val ACTION = "co.candyhouse.app.HOME_WIDGET_TOGGLE"
        const val EXTRA_ID = "homeWidgetId"
        fun ids(context: Context): IntArray = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, SesameHomeWidgetProvider::class.java))
        fun update(context: Context, id: Int, message: String? = null) {
            val uuid = HomeWidgetStore(context).device(id)
            val app = context.applicationContext as SesameApp
            val device = uuid?.let { app.ble.device(it) }
            val views = RemoteViews(context.packageName, R.layout.sesame_home_widget).apply {
                setTextViewText(R.id.home_widget_name, device?.let { app.ble.deviceName(it) } ?: "SESAME")
                setTextViewText(R.id.home_widget_status, message ?: context.getString(if (uuid == null) R.string.home_widget_unconfigured else R.string.home_widget_idle))
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
