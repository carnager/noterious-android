package dev.carnager.noterious.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE2C26B),
    onPrimary = Color(0xFF2A2110),
    secondary = Color(0xFF8FC7C2),
    onSecondary = Color(0xFF102120),
    tertiary = Color(0xFFB3C7F7),
    background = Color(0xFF111315),
    onBackground = Color(0xFFF1F1EC),
    surface = Color(0xFF1B1E21),
    onSurface = Color(0xFFF1F1EC),
    surfaceVariant = Color(0xFF272B2F),
    onSurfaceVariant = Color(0xFFC8C8C1),
    outline = Color(0xFF686B70),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF8B5E00),
    onPrimary = Color(0xFFFFF8E8),
    secondary = Color(0xFF006B66),
    onSecondary = Color(0xFFF4FFFD),
    tertiary = Color(0xFF305BA8),
    background = Color(0xFFF7F3EB),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFEAE1D2),
    onSurfaceVariant = Color(0xFF4A463F),
    outline = Color(0xFF7B766E),
)

private val AppTypography = Typography()

@Composable
fun NoteriousTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
