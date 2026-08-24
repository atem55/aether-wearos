package app.aether.wear.data

import kotlinx.serialization.Serializable
import java.util.UUID

const val MAX_POOLS = 10
const val MIN_POOL_MAX = 1
const val MAX_POOL_MAX = 999
const val DEFAULT_POOL_MAX = 10
const val DEFAULT_INTERVAL_MS = 5L * 60_000L
const val REGEN_AMOUNT = 1

@Serializable
data class PowerPool(
    val id: String,
    val name: String,
    val current: Int,
    val max: Int,
    val regenEnabled: Boolean,
    val intervalMs: Long,
    val nextRegenAt: Long? = null,
    val createdAt: Long,
)

data class PoolDraft(
    val name: String,
    val max: Int,
    val regenEnabled: Boolean,
    val intervalMs: Long,
)

val NAME_PRESETS = listOf("Mana", "Stamina", "Faith", "Rage", "Will", "Chi")

val INTERVAL_PRESETS = listOf(
    "10s" to 10_000L,
    "30s" to 30_000L,
    "1m" to 60_000L,
    "5m" to 5 * 60_000L,
    "10m" to 10 * 60_000L,
    "15m" to 15 * 60_000L,
    "30m" to 30 * 60_000L,
    "1h" to 60 * 60_000L,
)

fun newPoolId(): String = UUID.randomUUID().toString()

fun intervalLabel(ms: Long): String {
    INTERVAL_PRESETS.firstOrNull { it.second == ms }?.let { return it.first }
    return when {
        ms < 60_000L -> "${ms / 1000}s"
        ms < 3_600_000L -> "${ms / 60_000}m"
        else -> "${ms / 3_600_000}h"
    }
}

fun formatCountdown(ms: Long): String {
    val total = ((ms + 999) / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}

fun remainingMs(pool: PowerPool, now: Long): Long? {
    if (!pool.regenEnabled || pool.current >= pool.max || pool.nextRegenAt == null) return null
    return (pool.nextRegenAt - now).coerceAtLeast(0)
}
