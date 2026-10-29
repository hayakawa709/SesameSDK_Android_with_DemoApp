package co.candyhouse.app.homewidget

import org.junit.Assert.*
import org.junit.Test

class WidgetCommandPolicyTest {
    @Test fun onlyConfiguredActiveWidgetCanToggleItsOwnDevice() {
        assertTrue(WidgetCommandPolicy.allows(true, "uuid-A", "UUID-a", "toggle"))
        assertFalse(WidgetCommandPolicy.allows(true, "uuid-A", "uuid-B", "toggle"))
        assertFalse(WidgetCommandPolicy.allows(true, "uuid-A", null, "toggle"))
        assertFalse(WidgetCommandPolicy.allows(true, null, null, "toggle"))
        assertFalse(WidgetCommandPolicy.allows(true, "", "", "toggle"))
    }
    @Test fun deletedWidgetAndReplacedMappingRejectOldPendingIntent() {
        assertFalse(WidgetCommandPolicy.allows(false, "uuid-A", "uuid-A", "toggle"))
        assertFalse(WidgetCommandPolicy.allows(true, "uuid-B", "uuid-A", "toggle"))
    }
    @Test fun arbitraryOrMissingOperationIsRejected() {
        for (operation in listOf(null, "lock", "unlock", "", "TOGGLE")) {
            assertFalse(WidgetCommandPolicy.allows(true, "uuid-A", "uuid-A", operation))
        }
    }
}
