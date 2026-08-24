package app.aether.wear.data

import kotlinx.serialization.Serializable
import java.util.UUID

const val MAX_POOLS = 10
const val MIN_POOL_MAX = 1
const val MAX_POOL_MAX = 999
const val DEFAULT_POOL_MAX = 10
const val DEFAULT_REGEN_AMOUNT = 1
const val MIN_REGEN_AMOUNT = 1
const val MAX_REGEN_AMOUNT = 99
const val DEFAULT_INTERVAL_MINUTES = 5
const val MIN_INTERVAL_MINUTES = 1
const val MAX_INTERVAL_MINUTES = 180
const val DEFAULT_INTERVAL_MS = DEFAULT_INTERVAL_MINUTES * 60_000L
const val DEFAULT_POOL_COLOR = "#111113"

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
    val regenAmount: Int = DEFAULT_REGEN_AMOUNT,
    val pausedRemainingMs: Long? = null,
    val colorHex: String = DEFAULT_POOL_COLOR,
    val lightText: Boolean = true,
)

data class PoolDraft(
    val name: String,
    val max: Int,
    val startFull: Boolean,
    val regenEnabled: Boolean,
    val regenAmount: Int,
    val intervalMs: Long,
    val colorHex: String = DEFAULT_POOL_COLOR,
    val lightText: Boolean = true,
)

val NAME_PRESETS = listOf("Mana", "Spirits", "EP", "Blood", "Primal", "Ring")

data class ColorSwatch(val name: String, val hex: String, val lightText: Boolean)

val COLOR_SWATCHES = listOf(
    ColorSwatch("Black", "#111113", true),
    ColorSwatch("White", "#F4F1EA", false),
    ColorSwatch("Brown", "#5C3A21", true),
    ColorSwatch("Blue", "#1E4F9E", true),
    ColorSwatch("Red", "#B42A2A", true),
    ColorSwatch("Green", "#1F7A3A", true),
    ColorSwatch("Bronze", "#8A5524", true),
    ColorSwatch("Gold", "#C9A227", false),
    ColorSwatch("Ivory", "#EFE6C8", false),
    ColorSwatch("Grey", "#6B6E74", true),
    ColorSwatch("Silver", "#C5C8CC", false),
    ColorSwatch("Purple", "#6B2FA0", true),
)

val PRESET_COLORS = mapOf(
    "Mana" to "#1E4F9E",
    "Spirits" to "#6B2FA0",
    "EP" to "#C9A227",
    "Blood" to "#B42A2A",
    "Primal" to "#1F7A3A",
    "Ring" to "#C5C8CC",
)

fun defaultLightText(hex: String): Boolean =
    COLOR_SWATCHES.firstOrNull { it.hex.equals(hex, ignoreCase = true) }?.lightText ?: true

fun newPoolId(): String = UUID.randomUUID().toString()

fun minutesToMs(minutes: Int): Long = minutes.coerceAtLeast(MIN_INTERVAL_MINUTES) * 60_000L

fun msToMinutes(ms: Long): Int = (ms / 60_000L).toInt().coerceAtLeast(MIN_INTERVAL_MINUTES)

fun regenAmountOf(pool: PowerPool): Int = pool.regenAmount.coerceAtLeast(MIN_REGEN_AMOUNT)

fun firstRegenId(pools: List<PowerPool>): String? = pools.firstOrNull { it.regenEnabled }?.id

fun intervalLabel(ms: Long): String {
    val minutes = ms / 60_000L
    return when {
        ms % 60_000L == 0L && minutes >= 1L -> "${minutes}m"
        ms < 60_000L -> "${ms / 1000}s"
        ms < 3_600_000L -> "${minutes}m"
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

fun frozenRemainingMs(pool: PowerPool): Long? {
    if (!pool.regenEnabled || pool.current >= pool.max) return null
    if (pool.nextRegenAt != null) return null
    return pool.pausedRemainingMs
}

fun poolColor(hex: String): androidx.compose.ui.graphics.Color {
    val raw = if (hex.startsWith("#")) hex else "#$hex"
    val parsed = runCatching { android.graphics.Color.parseColor(raw) }.getOrElse {
        android.graphics.Color.parseColor(DEFAULT_POOL_COLOR)
    }
    return androidx.compose.ui.graphics.Color(parsed)
}

fun poolInk(lightText: Boolean): androidx.compose.ui.graphics.Color =
    if (lightText) androidx.compose.ui.graphics.Color(0xFFF2EFE8)
    else androidx.compose.ui.graphics.Color(0xFF111113)
