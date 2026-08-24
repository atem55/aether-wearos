package app.aether.wear.presentation.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

fun Modifier.holdRepeat(
    enabled: Boolean,
    onTick: () -> Unit,
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(pressed, enabled) {
        if (!pressed || !enabled) return@LaunchedEffect
        onTick()
        delay(380)
        while (pressed && enabled) {
            onTick()
            delay(90)
        }
    }
    pointerInput(enabled) {
        awaitEachGesture {
            awaitFirstDown()
            if (enabled) pressed = true
            waitForUpOrCancellation()
            pressed = false
        }
    }
}
