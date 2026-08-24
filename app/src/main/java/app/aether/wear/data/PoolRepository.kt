package app.aether.wear.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

class PoolRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val saved: Flow<SavedPools> = context.poolDataStore.data.map { prefs ->
        val raw = prefs[KEY_POOLS].orEmpty()
        val pools = if (raw.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<PowerPool>>(raw) }.getOrElse { emptyList() }
        val active = prefs[KEY_ACTIVE]?.takeIf { id -> pools.any { it.id == id && it.regenEnabled } }
            ?: firstRegenId(pools)
        SavedPools(pools, active, prefs[KEY_HALTED] == true)
    }

    suspend fun save(pools: List<PowerPool>, activeRegenId: String?, regenHalted: Boolean) {
        context.poolDataStore.edit {
            it[KEY_POOLS] = json.encodeToString(pools)
            if (activeRegenId == null) it.remove(KEY_ACTIVE)
            else it[KEY_ACTIVE] = activeRegenId
            it[KEY_HALTED] = regenHalted
        }
    }
}
