package app.aether.wear.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import app.aether.wear.presentation.screens.AddScreen
import app.aether.wear.presentation.screens.ConfirmScreen
import app.aether.wear.presentation.screens.DetailScreen
import app.aether.wear.presentation.screens.ListScreen
import app.aether.wear.presentation.theme.AetherTheme
import app.aether.wear.presentation.theme.Screen

@Composable
fun AetherApp(vm: PoolViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    AetherTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Screen),
        ) {
            TimeText()
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 14.dp),
            ) {
                if (!state.ready) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aether", color = MaterialTheme.colors.onBackground)
                    }
                } else {
                    BackHandler(enabled = state.route !is Route.Pools) { vm.back() }
                    when (val dest = state.route) {
                        is Route.Pools -> ListScreen(
                            pools = state.pools,
                            now = state.now,
                            onOpen = vm::open,
                            onDelete = { vm.askDelete(it, fromDetail = false) },
                            onAdd = vm::openAdd,
                            onClear = vm::askClear,
                        )
                        is Route.Detail -> {
                            val pool = state.pools.firstOrNull { it.id == dest.id }
                            if (pool == null) {
                                LaunchedEffect(dest.id) { vm.back() }
                            } else {
                                DetailScreen(
                                    pool = pool,
                                    now = state.now,
                                    onBack = vm::back,
                                    onAdjust = { vm.adjust(pool.id, it) },
                                    onDelete = { vm.askDelete(pool.id, fromDetail = true) },
                                )
                            }
                        }
                        is Route.Add -> AddScreen(onCancel = vm::back, onSave = vm::addPool)
                        is Route.ConfirmDelete -> {
                            val pool = state.pools.firstOrNull { it.id == dest.id }
                            ConfirmScreen(
                                title = "Delete ${pool?.name ?: "this pool"}?",
                                body = "This cannot be undone.",
                                onConfirm = { vm.remove(dest.id) },
                                onCancel = vm::back,
                            )
                        }
                        is Route.ConfirmClear -> ConfirmScreen(
                            title = "Clear every pool?",
                            body = "This cannot be undone.",
                            onConfirm = vm::clearAll,
                            onCancel = vm::back,
                        )
                    }
                }
            }
        }
    }
}
