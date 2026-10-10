package io.lunartrace.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 顶部渐变头的两个颜色（参考设计：粉 → 玫红）。 */
val LunartraceHeaderGradient = listOf(Color(0xFFF5A2AE), Color(0xFFDE5B7E))

// 配色承担语义，不只是好看：
//   primary            经期（实心胶囊）/ 预测经期（同色描边，见 MonthGrid）
//   tertiary           排卵日小点
//   tertiaryContainer  易孕窗口底色
//   secondary          有症状记录的小点
private val LightColors = lightColorScheme(
    primary = Color(0xFFD9536F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E1),
    onPrimaryContainer = Color(0xFF3F0D1B),
    secondary = Color(0xFF8A6E75),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3DDE2),
    onSecondaryContainer = Color(0xFF2B151A),
    tertiary = Color(0xFFE08A1E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF0CE),
    onTertiaryContainer = Color(0xFF4A3400),
    background = Color(0xFFFFFBFB),
    surface = Color(0xFFFFFBFB),
    surfaceVariant = Color(0xFFEFE2E5),
    onSurface = Color(0xFF23191B),
    onSurfaceVariant = Color(0xFF6B5A5E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2A2B4),
    onPrimary = Color(0xFF5A1B2A),
    primaryContainer = Color(0xFF7A3345),
    onPrimaryContainer = Color(0xFFFFD9E1),
    secondary = Color(0xFFD8C2C8),
    onSecondary = Color(0xFF3A2A2F),
    secondaryContainer = Color(0xFF514347),
    onSecondaryContainer = Color(0xFFEFDFE2),
    tertiary = Color(0xFFFFC46B),
    onTertiary = Color(0xFF4A3400),
    tertiaryContainer = Color(0xFF4A3A12),
    onTertiaryContainer = Color(0xFFFFE2A8),
    background = Color(0xFF1A1113),
    surface = Color(0xFF1A1113),
    surfaceVariant = Color(0xFF4A4447),
    onSurface = Color(0xFFEFDFE2),
    onSurfaceVariant = Color(0xFFD0C4C7),
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
