package co.candyhouse.app.homewidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import co.candyhouse.app.R
import co.candyhouse.app.SesameApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SesameHomeWidgetConfigureActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(id)
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID || info?.provider != ComponentName(this, SesameHomeWidgetProvider::class.java)) { finish(); return }
        val title = TextView(this).apply { setText(R.string.home_widget_choose); textSize = 18f; setPadding(24, 48, 24, 24) }
        val list = ListView(this)
        setContentView(LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; addView(title); addView(list) })
        scope.launch {
            val app = application as SesameApp
            try {
                app.ble.restoreSaved()
                val devices = app.ble.allDevices().filter(HomeWidgetStore::supports)
                if (devices.isEmpty()) { title.setText(R.string.home_widget_no_keys); return@launch }
                list.adapter = ArrayAdapter(this@SesameHomeWidgetConfigureActivity, android.R.layout.simple_list_item_1,
                    devices.map { "${app.ble.deviceName(it)} (${it.deviceId.toString().takeLast(6)})" })
                list.setOnItemClickListener { _, _, position, _ ->
                    val uuid = devices[position].deviceId.toString()
                    if (app.ble.device(uuid)?.let(HomeWidgetStore::supports) != true) { title.setText(R.string.home_widget_no_keys); return@setOnItemClickListener }
                    if (!HomeWidgetStore(this@SesameHomeWidgetConfigureActivity).save(id, uuid)) { title.setText(R.string.home_widget_failed); return@setOnItemClickListener }
                    SesameHomeWidgetProvider.update(this@SesameHomeWidgetConfigureActivity, id)
                    setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)); finish()
                }
            } catch (_: Exception) { title.setText(R.string.home_widget_failed) }
        }
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
