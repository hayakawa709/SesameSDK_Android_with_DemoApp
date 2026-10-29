package co.candyhouse.app.homewidget

import co.candyhouse.sesame.ble.CHDeviceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeWidgetLockStateTest {
    @Test fun explicitSdkLockStatesAreDisplayed() {
        assertEquals(HomeWidgetLockState.LOCKED, HomeWidgetLockState.from(CHDeviceStatus.Locked))
        assertEquals(HomeWidgetLockState.UNLOCKED, HomeWidgetLockState.from(CHDeviceStatus.Unlocked))
    }
    @Test fun disconnectIntermediateAndUnconfiguredStatesNeverImplyLockState() {
        CHDeviceStatus.entries.filter { it != CHDeviceStatus.Locked && it != CHDeviceStatus.Unlocked }.forEach {
            assertEquals(HomeWidgetLockState.UNKNOWN, HomeWidgetLockState.from(it))
        }
        assertEquals(HomeWidgetLockState.UNKNOWN, HomeWidgetLockState.from(null))
    }
}
