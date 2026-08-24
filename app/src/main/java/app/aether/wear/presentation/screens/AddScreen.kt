package app.aether.wear.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import app.aether.wear.data.COLOR_SWATCHES
import app.aether.wear.data.DEFAULT_INTERVAL_MINUTES
import app.aether.wear.data.DEFAULT_POOL_COLOR
import app.aether.wear.data.DEFAULT_POOL_MAX
import app.aether.wear.data.DEFAULT_REGEN_AMOUNT
import app.aether.wear.data.MAX_INTERVAL_MINUTES
import app.aether.wear.data.MAX_POOL_MAX
import app.aether.wear.data.MAX_REGEN_AMOUNT
import app.aether.wear.data.MIN_INTERVAL_MINUTES
import app.aether.wear.data.MIN_POOL_MAX
import app.aether.wear.data.MIN_REGEN_AMOUNT
import app.aether.wear.data.NAME_PRESETS
import app.aether.wear.data.PRESET_COLORS
import app.aether.wear.data.PoolDraft
import app.aether.wear.data.defaultLightText
import app.aether.wear.data.minutesToMs
import app.aether.wear.data.poolColor
import app.aether.wear.data.poolInk

@Composable
fun AddScreen(
    onCancel: () -> Unit,
    onSave: (PoolDraft) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var max by remember { mutableIntStateOf(DEFAULT_POOL_MAX) }
    var startFull by remember { mutableStateOf(false) }
    var regen by remember { mutableStateOf(false) }
    var regenAmount by remember { mutableIntStateOf(DEFAULT_REGEN_AMOUNT) }
    var intervalMin by remember { mutableIntStateOf(DEFAULT_INTERVAL_MINUTES) }
    var colorHex by remember { mutableStateOf(DEFAULT_POOL_COLOR) }
    var lightText by remember { mutableStateOf(true) }

    val listState = rememberScalingLazyListState()
    Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onCancel,
                        modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
                        colors = ButtonDefaults.secondaryButtonColors(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                    Text("New pool", style = MaterialTheme.typography.caption1)
                    Box(Modifier.size(ButtonDefaults.ExtraSmallButtonSize))
                }
            }
            item {
                BasicTextField(
                    value = name,
                    onValueChange = { if (it.length <= 18) name = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.title3.copy(
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
            }
            NAME_PRESETS.chunked(3).forEach { row ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        row.forEach { preset ->
                            val on = name == preset
                            CompactChip(
                                onClick = {
                                    name = preset
                                    val next = PRESET_COLORS[preset] ?: colorHex
                                    colorHex = next
                                    lightText = defaultLightText(next)
                                },
                                modifier = Modifier.weight(1f),
                                colors = if (on) ChipDefaults.primaryChipColors()
                                else ChipDefaults.secondaryChipColors(),
                                label = { Text(preset, style = MaterialTheme.typography.caption2) },
                            )
                        }
                    }
                }
            }
            item {
                StepperRow(
                    label = "Max",
                    value = max,
                    onChange = { max = it },
                    min = MIN_POOL_MAX,
                    maxValue = MAX_POOL_MAX,
                )
            }
            item { Text("Colour", style = MaterialTheme.typography.caption2) }
            COLOR_SWATCHES.chunked(6).forEach { row ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        row.forEach { swatch ->
                            val on = colorHex.equals(swatch.hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(poolColor(swatch.hex))
                                    .border(
                                        width = if (on) 2.dp else 1.dp,
                                        color = if (on) MaterialTheme.colors.onBackground
                                        else MaterialTheme.colors.onSurface.copy(alpha = 0.25f),
                                        shape = CircleShape,
                                    )
                                    .clickable {
                                        colorHex = swatch.hex
                                        lightText = swatch.lightText
                                    },
                            )
                        }
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(poolColor(colorHex))
                        .padding(vertical = 6.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        name.trim().ifEmpty { "Preview" }.uppercase(),
                        color = poolInk(lightText),
                        style = MaterialTheme.typography.caption1,
                    )
                }
            }
            item {
                ToggleChip(
                    checked = lightText,
                    onCheckedChange = { lightText = it },
                    label = { Text("White text?") },
                    toggleControl = {
                        Icon(
                            imageVector = ToggleChipDefaults.switchIcon(checked = lightText),
                            contentDescription = null,
                        )
                    },
                )
            }
            item {
                ToggleChip(
                    checked = startFull,
                    onCheckedChange = { startFull = it },
                    label = { Text("Start full?") },
                    toggleControl = {
                        Icon(
                            imageVector = ToggleChipDefaults.switchIcon(checked = startFull),
                            contentDescription = null,
                        )
                    },
                )
            }
            item {
                ToggleChip(
                    checked = regen,
                    onCheckedChange = { regen = it },
                    label = { Text("Regenning?") },
                    toggleControl = {
                        Icon(
                            imageVector = ToggleChipDefaults.switchIcon(checked = regen),
                            contentDescription = null,
                        )
                    },
                )
            }
            if (regen) {
                item {
                    StepperRow(
                        label = "Amount",
                        value = regenAmount,
                        onChange = { regenAmount = it },
                        min = MIN_REGEN_AMOUNT,
                        maxValue = MAX_REGEN_AMOUNT,
                    )
                }
                item {
                    StepperRow(
                        label = "Every",
                        value = intervalMin,
                        onChange = { intervalMin = it },
                        min = MIN_INTERVAL_MINUTES,
                        maxValue = MAX_INTERVAL_MINUTES,
                        suffix = "min",
                    )
                }
                item {
                    Text(
                        "+$regenAmount every ${intervalMin}m",
                        style = MaterialTheme.typography.caption2,
                        color = MaterialTheme.colors.primary,
                    )
                }
            }
            item {
                CompactChip(
                    onClick = {
                        onSave(
                            PoolDraft(
                                name = name.trim().ifEmpty { "Pool" },
                                max = max,
                                startFull = startFull,
                                regenEnabled = regen,
                                regenAmount = regenAmount,
                                intervalMs = minutesToMs(intervalMin),
                                colorHex = colorHex,
                                lightText = lightText,
                            ),
                        )
                    },
                    enabled = name.trim().isNotEmpty(),
                    icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                    label = { Text("Next") },
                )
            }
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    min: Int,
    maxValue: Int,
    suffix: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.caption1)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { onChange((value - 1).coerceAtLeast(min)) },
                enabled = value > min,
                modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) { Icon(Icons.Filled.Remove, contentDescription = "Decrease $label") }
            Text(
                if (suffix != null) "$value $suffix" else "$value",
                style = MaterialTheme.typography.title3,
            )
            Button(
                onClick = { onChange((value + 1).coerceAtMost(maxValue)) },
                enabled = value < maxValue,
                modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
            ) { Icon(Icons.Filled.Add, contentDescription = "Increase $label") }
        }
    }
}
