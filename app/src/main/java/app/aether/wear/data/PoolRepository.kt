package app.aether.wear.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.poolDataStore by preferencesDataStore("aether_pools")
private val KEY_POOLS = stringPreferencesKey("pools_json")
private val KEY_ACTIVE = stringPreferencesKey("active_regen_id")
private val KEY_HALTED = booleanPreferencesKey("regen_halted")

data class SavedPools(
    val pools: List<PowerPool>,
    val activeRegenId: String?,
    val regenHalted: Boolean = false,
)

data class TickResult(
    val saved: SavedPools,
    val gained: Boolean,
    val keepRunning: Boolean,
    val nextRegenAt: Long?,
)

class PoolRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()

    val saved: Flow<SavedPools> = context.poolDataStore.data.map { decode(it) }

    suspend fun current(): SavedPools = saved.first()

    suspend fun save(pools: List<PowerPool>, activeRegenId: String?, regenHalted: Boolean) {
        update { SavedPools(pools, activeRegenId, regenHalted) }
    }

    suspend fun update(transform: (SavedPools) -> SavedPools): SavedPools = mutex.withLock {
        var next = SavedPools(emptyList(), null, false)
        context.poolDataStore.edit { prefs ->
            next = transform(decode(prefs))
            write(prefs, next)
        }
        next
    }

    suspend fun applyTick(now: Long): TickResult = mutex.withLock {
        var gained = false
        var next = SavedPools(emptyList(), null, false)
        context.poolDataStore.edit { prefs ->
            val current = decode(prefs)
            val pools = current.pools.map { p ->
                if (!current.regenHalted && p.id == current.activeRegenId) applyRegen(p, now)
                else pauseRegen(p, now)
            }
            gained = pools.zip(current.pools).any { (after, before) -> after.current > before.current }
            next = current.copy(pools = pools)
            write(prefs, next)
        }
        TickResult(
            saved = next,
            gained = gained,
            keepRunning = next.isTicking(),
            nextRegenAt = next.tickingPool()?.nextRegenAt,
        )
    }

    private fun decode(prefs: Preferences): SavedPools {
        val raw = prefs[KEY_POOLS].orEmpty()
        val pools = if (raw.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<PowerPool>>(raw) }.getOrElse { emptyList() }
        val active = prefs[KEY_ACTIVE]?.takeIf { id -> pools.any { it.id == id && it.regenEnabled } }
            ?: firstRegenId(pools)
        return SavedPools(pools, active, prefs[KEY_HALTED] == true)
    }

    private fun write(prefs: MutablePreferences, saved: SavedPools) {
        prefs[KEY_POOLS] = json.encodeToString(saved.pools)
        if (saved.activeRegenId == null) prefs.remove(KEY_ACTIVE)
        else prefs[KEY_ACTIVE] = saved.activeRegenId
        prefs[KEY_HALTED] = saved.regenHalted
    }
}
