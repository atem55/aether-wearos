package app.aether.wear.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import app.aether.wear.presentation.theme.Teal

@Composable
fun PowerRing(
    value: Int,
    max: Int,
    modifier: Modifier = Modifier,
    stroke: Float = 8f,
    track: Color = Color(0xFF2E2E33),
    fill: Color = Teal,
) {
    val pct = if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f)
    Canvas(modifier) {
        val pad = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(pad, pad)
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        if (pct > 0f) {
            drawArc(
                color = fill,
                startAngle = -90f,
                sweepAngle = 360f * pct,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}
