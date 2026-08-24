package app.aether.wear.presentation.screens

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import app.aether.wear.data.PowerPool
import app.aether.wear.data.formatCountdown
import app.aether.wear.data.intervalLabel
import app.aether.wear.data.remainingMs
import app.aether.wear.presentation.components.PowerRing
import app.aether.wear.presentation.components.holdRepeat
import app.aether.wear.presentation.theme.Teal

@Composable
fun DetailScreen(
    pool: PowerPool,
    now: Long,
    onBack: () -> Unit,
    onAdjust: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    val remain = remainingMs(pool, now)
    val context = LocalContext.current
    fun haptic() {
        context.getSystemService<Vibrator>()?.vibrate(
            VibrationEffect.createOneShot(14, VibrationEffect.DEFAULT_AMPLITUDE),
        )
    }

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
                onClick = onBack,
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                pool.name.uppercase(),
                style = MaterialTheme.typography.caption1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onDelete,
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                colors = ButtonDefaults.secondaryButtonColors(),
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${pool.name}")
            }
        }

        Box(contentAlignment = Alignment.Center) {
            PowerRing(pool.current, pool.max, Modifier.size(112.dp), stroke = 11f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${pool.current}", style = MaterialTheme.typography.display1)
                Text(
                    "of ${pool.max}",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Button(
                onClick = {},
                enabled = pool.current > 0,
                modifier = Modifier
                    .size(ButtonDefaults.DefaultButtonSize)
                    .holdRepeat(pool.current > 0) {
                        haptic()
                        onAdjust(-1)
                    },
                colors = ButtonDefaults.secondaryButtonColors(),
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
            }
            Button(
                onClick = {},
                enabled = pool.current < pool.max,
                modifier = Modifier
                    .size(ButtonDefaults.DefaultButtonSize)
                    .holdRepeat(pool.current < pool.max) {
                        haptic()
                        onAdjust(1)
                    },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase")
            }
        }

        val caption = when {
            remain != null -> "NEXT  ${formatCountdown(remain)}"
            pool.regenEnabled -> "Full · regen ${intervalLabel(pool.intervalMs)}"
            else -> "Static pool"
        }
        Text(
            caption,
            style = MaterialTheme.typography.caption1,
            color = if (remain != null) Teal else MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(2.dp))
    }
}
