package app.aether.wear.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import app.aether.wear.data.DEFAULT_INTERVAL_MS
import app.aether.wear.data.DEFAULT_POOL_MAX
import app.aether.wear.data.INTERVAL_PRESETS
import app.aether.wear.data.MAX_POOL_MAX
import app.aether.wear.data.MIN_POOL_MAX
import app.aether.wear.data.NAME_PRESETS
import app.aether.wear.data.PoolDraft
import app.aether.wear.data.intervalLabel
import app.aether.wear.presentation.components.holdRepeat

@Composable
fun AddScreen(
    onCancel: () -> Unit,
    onSave: (PoolDraft) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf(false) }
    var max by remember { mutableIntStateOf(DEFAULT_POOL_MAX) }
    var regen by remember { mutableStateOf(false) }
    var intervalMs by remember { mutableLongStateOf(DEFAULT_INTERVAL_MS) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { if (step == 0) onCancel() else step -= 1 },
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                when (step) {
                    0 -> "Name"
                    1 -> "Maximum"
                    else -> "Regen"
                },
                style = MaterialTheme.typography.caption1,
            )
            Text("${step + 1}/3", style = MaterialTheme.typography.caption2)
        }

        when (step) {
            0 -> NameStep(
                name = name,
                custom = custom,
                onPick = { name = it; custom = false },
                onCustom = { custom = true; name = "" },
                onChange = { name = it },
            )
            1 -> MaxStep(max = max, onChange = { max = it })
            else -> RegenStep(
                regen = regen,
                intervalMs = intervalMs,
                onToggle = { regen = !regen },
                onCycle = { dir ->
                    val idx = INTERVAL_PRESETS.indexOfFirst { it.second == intervalMs }.coerceAtLeast(0)
                    val next = (idx + dir + INTERVAL_PRESETS.size) % INTERVAL_PRESETS.size
                    intervalMs = INTERVAL_PRESETS[next].second
                },
            )
        }

        if (step < 2) {
            CompactChip(
                onClick = { step += 1 },
                enabled = step != 0 || name.trim().isNotEmpty(),
                label = { Text("Next") },
            )
        } else {
            CompactChip(
                onClick = {
                    onSave(PoolDraft(name.trim().ifEmpty { "Pool" }, max, regen, intervalMs))
                },
                icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                label = { Text("Create") },
            )
        }
    }
}

@Composable
private fun NameStep(
    name: String,
    custom: Boolean,
    onPick: (String) -> Unit,
    onCustom: () -> Unit,
    onChange: (String) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (custom) {
            BasicTextField(
                value = name,
                onValueChange = { if (it.length <= 18) onChange(it) },
                singleLine = true,
                textStyle = MaterialTheme.typography.title2.copy(
                    color = MaterialTheme.colors.onBackground,
                    textAlign = TextAlign.Center,
                ),
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (name.isEmpty()) {
                            Text("Pool name", color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f))
                        }
                        inner()
                    }
                },
            )
        } else {
            NAME_PRESETS.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    row.forEach { preset ->
                        val on = name == preset
                        Chip(
                            onClick = { onPick(preset) },
                            modifier = Modifier.weight(1f),
                            colors = if (on) ChipDefaults.primaryChipColors()
                            else ChipDefaults.secondaryChipColors(),
                            label = { Text(preset) },
                        )
                    }
                }
            }
        }
        CompactChip(
            onClick = { if (custom) onPick("") else onCustom() },
            colors = ChipDefaults.secondaryChipColors(),
            label = { Text(if (custom) "Use a preset" else "Custom name") },
        )
    }
}

@Composable
private fun MaxStep(max: Int, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$max", style = MaterialTheme.typography.display1)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = {},
                enabled = max > MIN_POOL_MAX,
                modifier = Modifier
                    .size(ButtonDefaults.DefaultButtonSize)
                    .holdRepeat(max > MIN_POOL_MAX) { onChange(max - 1) },
                colors = ButtonDefaults.secondaryButtonColors(),
            ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease maximum") }
            Button(
                onClick = {},
                enabled = max < MAX_POOL_MAX,
                modifier = Modifier
                    .size(ButtonDefaults.DefaultButtonSize)
                    .holdRepeat(max < MAX_POOL_MAX) { onChange(max + 1) },
            ) { Icon(Icons.Filled.Add, contentDescription = "Increase maximum") }
        }
    }
}

@Composable
private fun RegenStep(
    regen: Boolean,
    intervalMs: Long,
    onToggle: () -> Unit,
    onCycle: (Int) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompactChip(
            onClick = onToggle,
            colors = if (regen) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
            label = { Text(if (regen) "On" else "Off") },
        )
        if (regen) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onCycle(-1) },
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                    colors = ButtonDefaults.secondaryButtonColors(),
                ) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Slower") }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(intervalLabel(intervalMs), style = MaterialTheme.typography.title2)
                    Text("1 point", style = MaterialTheme.typography.caption2)
                }
                Button(
                    onClick = { onCycle(1) },
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                    colors = ButtonDefaults.secondaryButtonColors(),
                ) { Icon(Icons.Filled.ChevronRight, contentDescription = "Faster") }
            }
        } else {
            Text(
                "Stays where you leave it",
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )
        }
    }
}
