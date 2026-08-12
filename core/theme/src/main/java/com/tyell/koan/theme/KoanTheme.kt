package com.tyell.koan.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tyell.koan.design.KoanShapes

/**
 * Stage 1 theme: Zen's branding neutrals only.
 *
 * The real thing — per-Space accent dots driving a generated mesh gradient —
 * arrives with the gradient engine. These two constants are lifted straight
 * from `zen-theme.css` (`--zen-branding-dark`, `--zen-branding-paper`).
 */
object KoanBranding {
    val Dark = Color(0xFF101010)
    val Paper = Color(0xFFE2E2E2)
}

private val DarkColors = darkColorScheme(
    background = KoanBranding.Dark,
    surface = Color(0xFF181818),
    surfaceVariant = Color(0xFF232323),
    onBackground = Color(0xFFEDEDED),
    onSurface = Color(0xFFEDEDED),
    outline = Color(0xFF4F4F4F),
)

private val LightColors = lightColorScheme(
    background = KoanBranding.Paper,
    surface = Color(0xFFF4F4F4),
    surfaceVariant = Color(0xFFE6E6E6),
    onBackground = Color(0xFF101010),
    onSurface = Color(0xFF101010),
    outline = Color(0xFFB5B5B5),
)

@Composable
fun KoanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = KoanShapes,
        content = content,
    )
}
