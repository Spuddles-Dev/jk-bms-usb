package com.horse.jk_bms.monitoring

class AlertDeduplicator(private val cooldownMs: Long = 60_000) {
    private var active = emptySet<String>()
    private val lastNotification = mutableMapOf<String, Long>()

    fun update(conditions: Set<String>, now: Long): Set<String> {
        val newlyActive = conditions - active
        active = conditions
        return newlyActive.filter { key ->
            val previous = lastNotification[key]
            previous == null || now - previous >= cooldownMs
        }.toSet().also { alerts -> alerts.forEach { lastNotification[it] = now } }
    }
}
