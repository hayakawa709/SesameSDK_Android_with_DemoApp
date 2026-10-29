package co.candyhouse.app.homewidget

import org.junit.Assert.*
import org.junit.Test

class WidgetCommandGateTest {
    @Test fun rapidTapsAndDuplicateWidgetsShareOneGate() {
        val gate = WidgetCommandGate()
        assertTrue(gate.acquire("a", 100))
        assertFalse(gate.acquire("A", 101))
        assertTrue(gate.acquire("b", 101))
        assertFalse(gate.acquire("a", 30_100))
        gate.complete("A", 30_100)
        assertFalse(gate.acquire("a", 33_099))
        assertTrue(gate.acquire("a", 33_100))
    }
    @Test fun processDeathDoesNotClearInFlightGuard() {
        val disk = mutableMapOf<String, Long>()
        fun gate() = WidgetCommandGate({ disk[it] ?: Long.MIN_VALUE }, { k, v -> disk[k] = v })
        assertTrue(gate().acquire("a", 100))
        assertFalse(gate().acquire("a", 200))
        assertTrue(gate().acquire("a", 35_100))
    }
    @Test fun persistenceFailureNeverAuthorizesACommand() {
        val gate = WidgetCommandGate({ Long.MIN_VALUE }, { _, _ -> error("disk full") })
        assertFalse(runCatching { gate.acquire("a", 100) }.getOrDefault(false))
    }
}
