package io.lunartrace.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF8E4A5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7D9E0),
    onPrimaryContainer = Color(0xFF3A0D1B),
    secondary = Color(0xFF73565C),
    secondaryContainer = Color(0xFFEFDDE1),
    onSecondaryContainer = Color(0xFF2B151A),
    tertiary = Color(0xFF7B6E4F),
    background = Color(0xFFFFF8F9),
    surface = Color(0xFFFFF8F9),
    surfaceVariant = Color(0xFFEDE0E3),
    onSurface = Color(0xFF22191B),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE5BBC5),
    onPrimary = Color(0xFF4A2530),
    primaryContainer = Color(0xFF653744),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFFD4C1C5),
    background = Color(0xFF1A1113),
    surface = Color(0xFF1A1113),
    surfaceVariant = Color(0xFF4A4447),
    onSurface = Color(0xFFEFDFE2),
)

@Composable
fun LunartraceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
