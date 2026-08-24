package app.aether.wear.data

fun applyRegen(pool: PowerPool, now: Long): PowerPool {
    if (!pool.regenEnabled || pool.current >= pool.max) {
        return if (pool.nextRegenAt != null) {
            pool.copy(current = minOf(pool.current, pool.max), nextRegenAt = null)
        } else {
            pool
        }
    }
    val due = pool.nextRegenAt ?: return pool.copy(nextRegenAt = now + pool.intervalMs)
    if (now < due) return pool

    val elapsed = now - due
    val ticks = (elapsed / pool.intervalMs) + 1
    val current = minOf(pool.max, pool.current + (ticks * regenAmountOf(pool)).toInt())
    return if (current >= pool.max) {
        pool.copy(current = pool.max, nextRegenAt = null)
    } else {
        pool.copy(current = current, nextRegenAt = due + ticks * pool.intervalMs)
    }
}

fun afterSpend(pool: PowerPool, nextCurrent: Int, now: Long): PowerPool {
    val current = nextCurrent.coerceIn(0, pool.max)
    val next = pool.copy(current = current)
    if (!next.regenEnabled || current >= next.max) {
        return next.copy(nextRegenAt = null)
    }
    if (pool.nextRegenAt != null && current < next.max) return next
    return next.copy(nextRegenAt = now + next.intervalMs)
}
