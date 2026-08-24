package app.aether.wear.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.aether.wear.data.MAX_POOLS
import app.aether.wear.data.PoolDraft
import app.aether.wear.data.PoolRepository
import app.aether.wear.data.PowerPool
import app.aether.wear.data.applyRegen
import app.aether.wear.data.afterSpend
import app.aether.wear.data.newPoolId
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    val route: Route = Route.Pools,
    val now: Long = System.currentTimeMillis(),
    val ready: Boolean = false,
)

class PoolViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = PoolRepository(application)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var persistJob: Job? = null

    init {
        viewModelScope.launch {
            repo.pools.collectLatest { stored ->
                val now = System.currentTimeMillis()
                val caught = stored.map { applyRegen(it, now) }
                _state.value = _state.value.copy(pools = caught, now = now, ready = true)
                if (caught != stored) persist(caught)
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(250)
                tick()
            }
        }
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        val current = _state.value
        val next = current.pools.map { applyRegen(it, now) }
        val changed = next != current.pools
        _state.value = current.copy(pools = next, now = now)
        if (changed) persist(next)
    }

    private fun persist(pools: List<PowerPool>) {
        persistJob?.cancel()
        persistJob = viewModelScope.launch { repo.save(pools) }
    }

    private fun updatePools(transform: (List<PowerPool>) -> List<PowerPool>) {
        val next = transform(_state.value.pools)
        _state.value = _state.value.copy(pools = next)
        persist(next)
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
        updatePools { pools ->
            if (pools.size >= MAX_POOLS) return@updatePools pools
            val name = draft.name.trim().ifEmpty { "Pool ${pools.size + 1}" }
            val max = draft.max.coerceIn(1, 999)
            val pool = PowerPool(
                id = newPoolId(),
                name = name,
                current = max,
                max = max,
                regenEnabled = draft.regenEnabled,
                intervalMs = draft.intervalMs,
                nextRegenAt = null,
                createdAt = System.currentTimeMillis(),
            )
            pools + pool
        }
        val id = _state.value.pools.lastOrNull()?.id
        _state.value = _state.value.copy(route = if (id != null) Route.Detail(id) else Route.Pools)
    }

    fun remove(id: String) {
        updatePools { it.filterNot { p -> p.id == id } }
        _state.value = _state.value.copy(route = Route.Pools)
    }

    fun clearAll() {
        updatePools { emptyList() }
        _state.value = _state.value.copy(route = Route.Pools)
    }

    fun adjust(id: String, delta: Int) {
        val now = System.currentTimeMillis()
        updatePools { pools ->
            pools.map { p ->
                if (p.id == id) afterSpend(p, p.current + delta, now) else p
            }
        }
    }
}
