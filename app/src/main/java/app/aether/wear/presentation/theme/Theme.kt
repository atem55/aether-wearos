package app.aether.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

val Teal = Color(0xFF8FD0C8)
val TealDim = Color(0xFF1E3A38)
val Bone = Color(0xFFF2EFE8)
val Muted = Color(0xFF8D8A84)
val Screen = Color(0xFF000000)
val Surface = Color(0xFF141416)
val Elevated = Color(0xFF1C1C20)
val Danger = Color(0xFFC45C4A)

private val AetherColors = Colors(
    primary = Teal,
    primaryVariant = TealDim,
    secondary = Teal,
    secondaryVariant = TealDim,
    background = Screen,
    surface = Surface,
    error = Danger,
    onPrimary = Color(0xFF0A1211),
    onSecondary = Color(0xFF0A1211),
    onBackground = Bone,
    onSurface = Bone,
    onError = Bone,
)

@Composable
fun AetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = AetherColors, content = content)
}
