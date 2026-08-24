package app.aether.wear.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.poolDataStore by preferencesDataStore("aether_pools")
private val KEY_POOLS = stringPreferencesKey("pools_json")

class PoolRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val pools: Flow<List<PowerPool>> = context.poolDataStore.data.map { prefs ->
        val raw = prefs[KEY_POOLS].orEmpty()
        if (raw.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<PowerPool>>(raw) }.getOrElse { emptyList() }
    }

    suspend fun save(pools: List<PowerPool>) {
        context.poolDataStore.edit { it[KEY_POOLS] = json.encodeToString(pools) }
    }
}
