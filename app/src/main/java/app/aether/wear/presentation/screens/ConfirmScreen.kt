package app.aether.wear.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import app.aether.wear.presentation.theme.Danger

@Composable
fun ConfirmScreen(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.title3, textAlign = TextAlign.Center)
            Text(
                body,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.secondaryButtonColors(),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Cancel")
            }
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.primaryButtonColors(
                    backgroundColor = Danger,
                    contentColor = MaterialTheme.colors.onError,
                ),
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Confirm")
            }
        }
    }
}
