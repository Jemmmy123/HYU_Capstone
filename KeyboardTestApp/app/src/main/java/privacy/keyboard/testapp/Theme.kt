package privacy.keyboard.testapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Blue = Color(0xFF2563EB)
val LightBlue = Color(0xFFE8F0FE)
val DarkText = Color(0xFF1F2937)
val GrayText = Color(0xFF6B7280)

// White and blue only, always light.
private val colors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = LightBlue,
    onPrimaryContainer = Blue,
    secondary = Blue,
    onSecondary = Color.White,
    secondaryContainer = LightBlue,
    onSecondaryContainer = Blue,
    background = Color.White,
    onBackground = DarkText,
    surface = Color.White,
    onSurface = DarkText,
    surfaceVariant = LightBlue,
    onSurfaceVariant = GrayText,
    surfaceContainer = Color.White,
    outline = Color(0xFFBFD3FB),
)

@Composable
fun TestAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
