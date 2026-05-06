package dev.carnager.noterious.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.carnager.noterious.model.ThemeRecord
import dev.carnager.noterious.model.ThemeTokens

private val SystemDarkColors = darkColorScheme(
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

private val SystemLightColors = lightColorScheme(
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
    themeId: String = "system",
    themes: List<ThemeRecord> = emptyList(),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val selectedTheme = themes.firstOrNull { theme ->
        theme.id.equals(themeId.trim(), ignoreCase = true)
    }
    val colorScheme = when {
        themeId.trim().equals("system", ignoreCase = true) || selectedTheme == null ->
            if (darkTheme) SystemDarkColors else SystemLightColors
        selectedTheme.kind.equals("dark", ignoreCase = true) -> darkColorSchemeForTheme(selectedTheme.tokens)
        else -> lightColorSchemeForTheme(selectedTheme.tokens)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

private fun darkColorSchemeForTheme(tokens: ThemeTokens): ColorScheme {
    val accent = parseThemeColor(tokens.accent, SystemDarkColors.primary)
    val primaryContainer = parseThemeColor(tokens.accentSoft, accent.copy(alpha = 0.28f))
    val background = parseThemeColor(tokens.bg, SystemDarkColors.background)
    val surface = parseThemeColor(tokens.panel, SystemDarkColors.surface)
    val surfaceVariant = parseThemeColor(tokens.surface, SystemDarkColors.surfaceVariant)
    val secondary = parseThemeColor(tokens.surfaceSoft, SystemDarkColors.secondary)
    val secondaryContainer = parseThemeColor(tokens.overlaySoft, surfaceVariant)
    val tertiary = parseThemeColor(tokens.tableHeader, accent.copy(alpha = 0.6f))
    val onBackground = parseThemeColor(tokens.ink, SystemDarkColors.onBackground)
    val onSurfaceVariant = parseThemeColor(tokens.muted, SystemDarkColors.onSurfaceVariant)
    val outline = parseThemeColor(tokens.lineStrong, SystemDarkColors.outline)
    val error = parseThemeColor(tokens.warn, Color(0xFFF7768E))

    return darkColorScheme(
        primary = accent,
        onPrimary = preferredContentColor(accent),
        primaryContainer = primaryContainer,
        onPrimaryContainer = onBackground,
        secondary = secondary,
        onSecondary = preferredContentColor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onBackground,
        tertiary = tertiary,
        onTertiary = preferredContentColor(tertiary),
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onBackground,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        error = error,
        onError = preferredContentColor(error),
    )
}

private fun lightColorSchemeForTheme(tokens: ThemeTokens): ColorScheme {
    val accent = parseThemeColor(tokens.accent, SystemLightColors.primary)
    val primaryContainer = parseThemeColor(tokens.accentSoft, accent.copy(alpha = 0.18f))
    val background = parseThemeColor(tokens.bg, SystemLightColors.background)
    val surface = parseThemeColor(tokens.panel, SystemLightColors.surface)
    val surfaceVariant = parseThemeColor(tokens.surface, SystemLightColors.surfaceVariant)
    val secondary = parseThemeColor(tokens.surfaceSoft, SystemLightColors.secondary)
    val secondaryContainer = parseThemeColor(tokens.overlaySoft, surfaceVariant)
    val tertiary = parseThemeColor(tokens.tableHeader, accent.copy(alpha = 0.45f))
    val onBackground = parseThemeColor(tokens.ink, SystemLightColors.onBackground)
    val onSurfaceVariant = parseThemeColor(tokens.muted, SystemLightColors.onSurfaceVariant)
    val outline = parseThemeColor(tokens.lineStrong, SystemLightColors.outline)
    val error = parseThemeColor(tokens.warn, Color(0xFFB3261E))

    return lightColorScheme(
        primary = accent,
        onPrimary = preferredContentColor(accent),
        primaryContainer = primaryContainer,
        onPrimaryContainer = onBackground,
        secondary = secondary,
        onSecondary = preferredContentColor(secondary),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onBackground,
        tertiary = tertiary,
        onTertiary = preferredContentColor(tertiary),
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onBackground,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        error = error,
        onError = preferredContentColor(error),
    )
}

private fun parseThemeColor(value: String, fallback: Color): Color {
    val normalized = value.trim()
    if (normalized.isBlank()) {
        return fallback
    }
    return runCatching {
        when {
            normalized.startsWith("rgba(", ignoreCase = true) && normalized.endsWith(")") -> {
                val parts = normalized.substringAfter('(').substringBeforeLast(')').split(',')
                    .map(String::trim)
                if (parts.size != 4) {
                    fallback
                } else {
                    Color(
                        red = parts[0].toFloat().coerceIn(0f, 255f) / 255f,
                        green = parts[1].toFloat().coerceIn(0f, 255f) / 255f,
                        blue = parts[2].toFloat().coerceIn(0f, 255f) / 255f,
                        alpha = parts[3].toFloat().coerceIn(0f, 1f),
                    )
                }
            }
            normalized.startsWith("rgb(", ignoreCase = true) && normalized.endsWith(")") -> {
                val parts = normalized.substringAfter('(').substringBeforeLast(')').split(',')
                    .map(String::trim)
                if (parts.size != 3) {
                    fallback
                } else {
                    Color(
                        red = parts[0].toFloat().coerceIn(0f, 255f) / 255f,
                        green = parts[1].toFloat().coerceIn(0f, 255f) / 255f,
                        blue = parts[2].toFloat().coerceIn(0f, 255f) / 255f,
                    )
                }
            }
            else -> Color(AndroidColor.parseColor(normalized))
        }
    }.getOrElse { fallback }
}

private fun preferredContentColor(background: Color): Color {
    return if (background.luminance() > 0.45f) {
        Color(0xFF111111)
    } else {
        Color(0xFFF7F7F7)
    }
}
