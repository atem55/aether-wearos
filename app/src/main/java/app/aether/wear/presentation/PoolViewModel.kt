package app.aether.wear.presentation

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.aether.wear.data.MAX_POOLS
import app.aether.wear.data.PoolDraft
import app.aether.wear.data.PoolRepository
import app.aether.wear.data.PowerPool
import app.aether.wear.data.afterSpend
import app.aether.wear.data.applyRegen
import app.aether.wear.data.armRegen
import app.aether.wear.data.firstRegenId
import app.aether.wear.data.newPoolId
import app.aether.wear.data.pauseRegen
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
    val activeRegenId: String? = null,
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
            repo.saved.collectLatest { stored ->
                val now = System.currentTimeMillis()
                val armed = stored.activeRegenId
                val caught = stored.pools.map { p ->
                    if (p.id == armed) applyRegen(p, now) else pauseRegen(p, now)
                }
                _state.value = _state.value.copy(
                    pools = caught,
                    activeRegenId = armed,
                    now = now,
                    ready = true,
                )
                if (caught != stored.pools || armed != stored.activeRegenId) persist(caught, armed)
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
        val armed = current.activeRegenId
        val next = current.pools.map { p ->
            if (p.id == armed) applyRegen(p, now) else pauseRegen(p, now)
        }
        val changed = next != current.pools
        _state.value = current.copy(pools = next, now = now)
        if (changed) persist(next, armed)
    }

    private fun persist(pools: List<PowerPool>, activeRegenId: String?) {
        persistJob?.cancel()
        persistJob = viewModelScope.launch { repo.save(pools, activeRegenId) }
    }

    private fun commit(pools: List<PowerPool>, activeRegenId: String? = _state.value.activeRegenId) {
        _state.value = _state.value.copy(pools = pools, activeRegenId = activeRegenId)
        persist(pools, activeRegenId)
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
        )
        val nextPools = pools + pool
        var armed = _state.value.activeRegenId
        if (draft.regenEnabled && armed == null) armed = pool.id
        commit(armRegen(nextPools, armed, now), armed)
        _state.value = _state.value.copy(route = Route.Pools)
    }

    fun remove(id: String) {
        val now = System.currentTimeMillis()
        val pools = _state.value.pools.filterNot { it.id == id }
        var armed = _state.value.activeRegenId
        if (armed == id) armed = firstRegenId(pools)
        commit(armRegen(pools, armed, now), armed)
        _state.value = _state.value.copy(route = Route.Pools)
    }

    fun clearAll() {
        commit(emptyList(), null)
        _state.value = _state.value.copy(route = Route.Pools)
    }

    fun setActiveRegen(id: String) {
        val target = _state.value.pools.firstOrNull { it.id == id } ?: return
        if (!target.regenEnabled) return
        val now = System.currentTimeMillis()
        commit(armRegen(_state.value.pools, id, now), id)
    }

    fun adjust(id: String, delta: Int) {
        val now = System.currentTimeMillis()
        val armed = _state.value.activeRegenId
        var emptied = false
        val next = _state.value.pools.map { p ->
            if (p.id != id) return@map p
            val updated = afterSpend(p, p.current + delta, now, p.id == armed)
            if (p.regenEnabled && p.current > 0 && updated.current == 0) emptied = true
            updated
        }
        commit(next, armed)
        if (emptied) depletePulse()
    }

    private fun depletePulse() {
        val app = getApplication<Application>()
        val vibrator = if (Build.VERSION.SDK_INT >= 31) {
            app.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Vibrator::class.java)
        } ?: return
        val pattern = longArrayOf(0, 450, 90, 450, 90, 780)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}
