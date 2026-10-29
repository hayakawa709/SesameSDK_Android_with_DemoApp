package co.candyhouse.app.homewidget

import android.content.Context
import co.candyhouse.sesame.ble.CHDevices
import co.candyhouse.sesame.ble.os2.sesame2.CHSesame2
import co.candyhouse.sesame.ble.os3.sesame5.CHSesame5

/** Stores identifiers only. Keys and credentials remain in the existing key store. */
class HomeWidgetStore(context: Context) {
    private val prefs = context.getSharedPreferences("home_widgets", Context.MODE_PRIVATE)
    fun device(id: Int): String? = prefs.getString("device_$id", null)
    fun save(id: Int, uuid: String): Boolean = prefs.edit().putString("device_$id", uuid).commit()
    fun delete(id: Int) { prefs.edit().remove("device_$id").apply() }
    companion object {
        fun supports(device: CHDevices) = device is CHSesame2 || device is CHSesame5
    }
}
