package app.aether.wear.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.aether.wear.data.MAX_POOLS
import app.aether.wear.data.PoolDraft
import app.aether.wear.data.PoolRepository
import app.aether.wear.data.PowerPool
import app.aether.wear.data.afterSpend
import app.aether.wear.data.firstRegenId
import app.aether.wear.data.newPoolId
import app.aether.wear.data.syncRegen
import app.aether.wear.regen.RegenController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface Route {
    data object Pools : Route
    data class Detail(val id: String) : Route
    data object Add : Route
    data class ConfirmDelete(val id: String, val fromDetail: Boolean) : Route
    data object ConfirmClear : Route
}

data class UiState(
    val pools: List<PowerPool> = emptyList(),
    val activeRegenId: String? = null,
    val regenHalted: Boolean = false,
    val route: Route = Route.Pools,
    val now: Long = System.currentTimeMillis(),
    val ready: Boolean = false,
)

class PoolViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = PoolRepository(application)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.update { stored ->
                val now = System.currentTimeMillis()
                stored.copy(pools = syncRegen(stored.pools, stored.activeRegenId, now, stored.regenHalted))
            }
            RegenController.sync(getApplication())
        }
        viewModelScope.launch {
            repo.saved.collect { stored ->
                _state.value = _state.value.copy(
                    pools = stored.pools,
                    activeRegenId = stored.activeRegenId,
                    regenHalted = stored.regenHalted,
                    ready = true,
                )
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(250)
                _state.value = _state.value.copy(now = System.currentTimeMillis())
            }
        }
    }

    private fun commit(
        pools: List<PowerPool>,
        activeRegenId: String? = _state.value.activeRegenId,
        regenHalted: Boolean = _state.value.regenHalted,
    ) {
        _state.value = _state.value.copy(pools = pools, activeRegenId = activeRegenId, regenHalted = regenHalted)
        viewModelScope.launch {
            repo.save(pools, activeRegenId, regenHalted)
            RegenController.sync(getApplication())
        }
    }

    fun open(id: String) { _state.value = _state.value.copy(route = Route.Detail(id)) }
    fun openAdd() { _state.value = _state.value.copy(route = Route.Add) }
    fun askDelete(id: String, fromDetail: Boolean) {
        _state.value = _state.value.copy(route = Route.ConfirmDelete(id, fromDetail))
    }
    fun askClear() { _state.value = _state.value.copy(route = Route.ConfirmClear) }

    fun back() {
        val route = _state.value.route
        _state.value = _state.value.copy(
            route = when (route) {
                is Route.ConfirmDelete ->
                    if (route.fromDetail) Route.Detail(route.id) else Route.Pools
                else -> Route.Pools
            },
        )
    }

    fun addPool(draft: PoolDraft) {
        val now = System.currentTimeMillis()
        val pools = _state.value.pools
        if (pools.size >= MAX_POOLS) {
            _state.value = _state.value.copy(route = Route.Pools)
            return
        }
        val name = draft.name.trim().ifEmpty { "Pool ${pools.size + 1}" }
        val max = draft.max.coerceIn(1, 999)
        val current = if (draft.startFull) max else 0
        val pool = PowerPool(
            id = newPoolId(),
            name = name,
            current = current,
            max = max,
            regenEnabled = draft.regenEnabled,
            regenAmount = draft.regenAmount.coerceIn(1, 99),
            intervalMs = draft.intervalMs,
            nextRegenAt = null,
            pausedRemainingMs = if (draft.regenEnabled && current < max) draft.intervalMs else null,
            createdAt = now,
            colorHex = draft.colorHex,
            lightText = draft.lightText,
        )
        val nextPools = pools + pool
        var armed = _state.value.activeRegenId
        if (draft.regenEnabled && armed == null) armed = pool.id
        commit(syncRegen(nextPools, armed, now, _state.value.regenHalted), armed)
        _state.value = _state.value.copy(route = Route.Pools)
    }

    fun remove(id: String) {
        val now = System.currentTimeMillis()
        val pools = _state.value.pools.filterNot { it.id == id }
        var armed = _state.value.activeRegenId
        if (armed == id) armed = firstRegenId(pools)
        commit(syncRegen(pools, armed, now, _state.value.regenHalted), armed)
        _state.value = _state.value.copy(route = Route.Pools)
    }

    fun clearAll() {
        commit(emptyList(), null, false)
        _state.value = _state.value.copy(route = Route.Pools)
    }

    fun setActiveRegen(id: String) {
        val target = _state.value.pools.firstOrNull { it.id == id } ?: return
        if (!target.regenEnabled) return
        if (_state.value.activeRegenId == id) return
        val now = System.currentTimeMillis()
        commit(syncRegen(_state.value.pools, id, now, _state.value.regenHalted), id)
    }

    fun toggleRegenHalt() {
        val now = System.currentTimeMillis()
        val halted = !_state.value.regenHalted
        commit(syncRegen(_state.value.pools, _state.value.activeRegenId, now, halted), regenHalted = halted)
    }

    fun adjust(id: String, delta: Int) {
        val now = System.currentTimeMillis()
        val armed = _state.value.activeRegenId
        val tickingId = if (_state.value.regenHalted) null else armed
        val next = _state.value.pools.map { p ->
            if (p.id != id) p else afterSpend(p, p.current + delta, now, p.id == tickingId)
        }
        commit(next, armed)
    }
}
