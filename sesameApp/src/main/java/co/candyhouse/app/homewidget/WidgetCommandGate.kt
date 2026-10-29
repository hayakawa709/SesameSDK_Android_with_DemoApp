package co.candyhouse.app.homewidget

/** Shared across all instances: two widgets for the same lock cannot race. */
class WidgetCommandGate(private val read: ((String) -> Long)? = null, private val write: ((String, Long) -> Unit)? = null) {
    private val blockedUntil = mutableMapOf<String, Long>()
    @Synchronized fun acquire(uuid: String, now: Long): Boolean {
        val key = uuid.uppercase()
        if (now < (read?.invoke(key) ?: blockedUntil[key] ?: Long.MIN_VALUE)) return false
        write?.invoke(key, now + 35_000)
        blockedUntil[key] = now + 35_000
        return true
    }
    @Synchronized fun complete(uuid: String, now: Long) {
        write?.invoke(uuid.uppercase(), now + 3_000)
        blockedUntil[uuid.uppercase()] = now + 3_000
    }
}
