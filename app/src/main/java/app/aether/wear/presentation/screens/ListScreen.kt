package app.aether.wear.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import app.aether.wear.data.MAX_POOLS
import app.aether.wear.data.PowerPool
import app.aether.wear.data.formatCountdown
import app.aether.wear.data.intervalLabel
import app.aether.wear.data.remainingMs
import app.aether.wear.presentation.components.PowerRing
import app.aether.wear.presentation.theme.Danger
import app.aether.wear.presentation.theme.Teal

@Composable
fun ListScreen(
    pools: List<PowerPool>,
    now: Long,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit,
    onClear: () -> Unit,
) {
    if (pools.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PowerRing(0, 1, Modifier.size(72.dp), stroke = 10f)
                Spacer(Modifier.height(10.dp))
                Text("AETHER", style = MaterialTheme.typography.title2)
                Text(
                    "Add a power pool",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = "Add pool")
                }
            }
        }
        return
    }

    val listState = rememberScalingLazyListState()
    Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { ListHeader { Text("Aether") } }
            items(pools, key = { it.id }) { pool ->
                PoolChip(
                    pool = pool,
                    now = now,
                    onOpen = { onOpen(pool.id) },
                    onDelete = { onDelete(pool.id) },
                )
            }
            if (pools.size < MAX_POOLS) {
                item {
                    Chip(
                        onClick = onAdd,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ChipDefaults.secondaryChipColors(),
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        label = { Text("Add pool") },
                    )
                }
            } else {
                item {
                    Text(
                        "10 pool limit",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            item {
                CompactChip(
                    onClick = onClear,
                    label = { Text("Clear all", color = Danger) },
                )
            }
        }
    }
}

@Composable
private fun PoolChip(
    pool: PowerPool,
    now: Long,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val remain = remainingMs(pool, now)
    val extra = when {
        remain != null -> formatCountdown(remain)
        pool.regenEnabled -> "Full · ${intervalLabel(pool.intervalMs)}"
        else -> null
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Chip(
            onClick = onOpen,
            modifier = Modifier.weight(1f),
            colors = ChipDefaults.secondaryChipColors(),
            icon = { PowerRing(pool.current, pool.max, Modifier.size(28.dp), stroke = 5f) },
            label = { Text(pool.name.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            secondaryLabel = {
                Text(
                    buildString {
                        append(pool.current)
                        append(" / ")
                        append(pool.max)
                        if (extra != null) {
                            append("  ")
                            append(extra)
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (remain != null) Teal else MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            },
        )
        Button(
            onClick = onDelete,
            modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
            colors = ButtonDefaults.secondaryButtonColors(),
        ) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete ${pool.name}", modifier = Modifier.size(16.dp))
        }
    }
}
