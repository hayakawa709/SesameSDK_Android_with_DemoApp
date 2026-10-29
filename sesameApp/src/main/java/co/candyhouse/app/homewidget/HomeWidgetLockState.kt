package co.candyhouse.app.homewidget

import co.candyhouse.sesame.ble.CHDeviceStatus

enum class HomeWidgetLockState {
    LOCKED, UNLOCKED, UNKNOWN;

    companion object {
        fun from(status: CHDeviceStatus?): HomeWidgetLockState = when (status) {
            CHDeviceStatus.Locked -> LOCKED
            CHDeviceStatus.Unlocked -> UNLOCKED
            else -> UNKNOWN
        }
    }
}
