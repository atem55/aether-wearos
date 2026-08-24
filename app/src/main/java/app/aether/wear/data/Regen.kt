package app.aether.wear.data

fun applyRegen(pool: PowerPool, now: Long): PowerPool {
    if (!pool.regenEnabled || pool.current >= pool.max) {
        return if (pool.nextRegenAt != null || pool.pausedRemainingMs != null) {
            pool.copy(current = minOf(pool.current, pool.max), nextRegenAt = null, pausedRemainingMs = null)
        } else {
            pool
        }
    }
    val due = pool.nextRegenAt ?: return pool.copy(
        nextRegenAt = now + (pool.pausedRemainingMs ?: pool.intervalMs),
        pausedRemainingMs = null,
    )
    if (now < due) return pool

    val elapsed = now - due
    val ticks = (elapsed / pool.intervalMs) + 1
    val current = minOf(pool.max, pool.current + (ticks * regenAmountOf(pool)).toInt())
    return if (current >= pool.max) {
        pool.copy(current = pool.max, nextRegenAt = null, pausedRemainingMs = null)
    } else {
        pool.copy(current = current, nextRegenAt = due + ticks * pool.intervalMs, pausedRemainingMs = null)
    }
}

fun pauseRegen(pool: PowerPool, now: Long): PowerPool {
    if (!pool.regenEnabled || pool.current >= pool.max) {
        return if (pool.nextRegenAt == null && pool.pausedRemainingMs == null) pool
        else pool.copy(nextRegenAt = null, pausedRemainingMs = null)
    }
    if (pool.nextRegenAt == null) {
        return if (pool.pausedRemainingMs != null) pool
        else pool.copy(pausedRemainingMs = pool.intervalMs)
    }
    return pool.copy(nextRegenAt = null, pausedRemainingMs = (pool.nextRegenAt - now).coerceAtLeast(0))
}

fun resumeRegen(pool: PowerPool, now: Long): PowerPool {
    if (!pool.regenEnabled || pool.current >= pool.max) {
        return pool.copy(nextRegenAt = null, pausedRemainingMs = null)
    }
    if (pool.nextRegenAt != null) return pool.copy(pausedRemainingMs = null)
    val remain = pool.pausedRemainingMs ?: pool.intervalMs
    return pool.copy(nextRegenAt = now + remain, pausedRemainingMs = null)
}

fun armRegen(pools: List<PowerPool>, armedId: String?, now: Long): List<PowerPool> =
    pools.map { p ->
        when {
            !p.regenEnabled -> pauseRegen(p, now)
            p.id == armedId -> resumeRegen(p, now)
            else -> pauseRegen(p, now)
        }
    }

fun syncRegen(pools: List<PowerPool>, armedId: String?, now: Long, halted: Boolean): List<PowerPool> =
    if (halted) pools.map { pauseRegen(it, now) } else armRegen(pools, armedId, now)

fun afterSpend(pool: PowerPool, nextCurrent: Int, now: Long, ticking: Boolean): PowerPool {
    val current = nextCurrent.coerceIn(0, pool.max)
    val next = pool.copy(current = current)
    if (!next.regenEnabled || current >= next.max) {
        return next.copy(nextRegenAt = null, pausedRemainingMs = null)
    }
    if (ticking) {
        if (pool.nextRegenAt != null && current < next.max) {
            return next.copy(pausedRemainingMs = null)
        }
        return next.copy(nextRegenAt = now + next.intervalMs, pausedRemainingMs = null)
    }
    val remain = pool.pausedRemainingMs
        ?: pool.nextRegenAt?.let { (it - now).coerceAtLeast(0) }
        ?: next.intervalMs
    return next.copy(nextRegenAt = null, pausedRemainingMs = remain)
}
