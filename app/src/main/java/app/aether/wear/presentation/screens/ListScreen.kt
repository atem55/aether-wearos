package app.aether.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
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
import app.aether.wear.data.frozenRemainingMs
import app.aether.wear.data.intervalLabel
import app.aether.wear.data.poolColor
import app.aether.wear.data.poolInk
import app.aether.wear.data.regenAmountOf
import app.aether.wear.data.remainingMs
import app.aether.wear.presentation.components.PowerRing
import app.aether.wear.presentation.theme.Danger
import app.aether.wear.presentation.theme.Muted
import app.aether.wear.presentation.theme.Teal

@Composable
fun ListScreen(
    pools: List<PowerPool>,
    now: Long,
    activeRegenId: String?,
    regenHalted: Boolean,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit,
    onClear: () -> Unit,
    onAdjust: (String, Int) -> Unit,
    onArmRegen: (String) -> Unit,
    onToggleHalt: () -> Unit,
) {
    if (pools.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PowerRing(0, 1, Modifier.size(64.dp), stroke = 9f)
                Spacer(Modifier.height(8.dp))
                Text("AETHER", style = MaterialTheme.typography.title2)
                Text(
                    "Add a power pool",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(10.dp))
                CompactChip(
                    onClick = onAdd,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    label = { Text("Add pool") },
                )
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
                    armed = pool.id == activeRegenId,
                    halted = regenHalted,
                    onOpen = { onOpen(pool.id) },
                    onDelete = { onDelete(pool.id) },
                    onAdjust = { onAdjust(pool.id, it) },
                    onArm = { onArmRegen(pool.id) },
                    onToggleHalt = onToggleHalt,
                )
            }
            if (pools.size < MAX_POOLS) {
                item {
                    CompactChip(
                        onClick = onAdd,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
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
    armed: Boolean,
    halted: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onAdjust: (Int) -> Unit,
    onArm: () -> Unit,
    onToggleHalt: () -> Unit,
) {
    val remain = remainingMs(pool, now)
    val frozen = frozenRemainingMs(pool)
    val ticking = armed && !halted
    val extra = when {
        ticking && remain != null -> formatCountdown(remain)
        pool.regenEnabled && pool.current < pool.max ->
            "Paused" + if (frozen != null) " ${formatCountdown(frozen)}" else ""
        pool.regenEnabled -> "Full · +${regenAmountOf(pool)}/${intervalLabel(pool.intervalMs)}"
        else -> "Static"
    }
    val ink = poolInk(pool.lightText)
    val fill = poolColor(pool.colorHex)
    val extraColor = if (ticking && remain != null) ink else ink.copy(alpha = 0.75f)
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(fill)
                    .padding(start = 8.dp, end = 4.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpen),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PowerRing(pool.current, pool.max, Modifier.size(26.dp), stroke = 5f)
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pool.name.uppercase(), maxLines = 1, overflow = TextOverflow.Ellipsis, color = ink)
                        Text(extra, maxLines = 1, overflow = TextOverflow.Ellipsis, color = extraColor, style = MaterialTheme.typography.caption2)
                    }
                }
                if (armed) {
                    Button(
                        onClick = onToggleHalt,
                        modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
                        colors = ButtonDefaults.primaryButtonColors(),
                    ) {
                        Icon(
                            imageVector = if (halted) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (halted) "Resume all regen" else "Pause all regen",
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
            if (pool.regenEnabled) {
                Spacer(Modifier.width(4.dp))
                Button(
                    onClick = onArm,
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                    colors = ButtonDefaults.secondaryButtonColors(),
                ) {
                    Icon(
                        imageVector = if (armed) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = if (armed) "Regen armed" else "Arm regen",
                        tint = if (armed) Teal else Muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { onAdjust(-1) },
                enabled = pool.current > 0,
                modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease ${pool.name}", modifier = Modifier.size(16.dp))
            }
            Text("${pool.current} / ${pool.max}", style = MaterialTheme.typography.title3)
            Button(
                onClick = { onAdjust(1) },
                enabled = pool.current < pool.max,
                modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase ${pool.name}", modifier = Modifier.size(16.dp))
            }
            Button(
                onClick = onDelete,
                modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${pool.name}", modifier = Modifier.size(16.dp))
            }
        }
    }
}
