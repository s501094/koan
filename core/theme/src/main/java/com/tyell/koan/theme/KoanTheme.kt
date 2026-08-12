package com.tyell.koan.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.tyell.koan.design.KoanShapes

/**
 * Zen's branding neutrals, from `zen-theme.css`.
 * `--zen-branding-dark` and `--zen-branding-paper`.
 */
object KoanBranding {
    val DarkRgb = ZenColor.hexToRgb("#101010")
    val PaperRgb = ZenColor.hexToRgb("#E2E2E2")
    val Dark = DarkRgb.toColor()
    val Paper = PaperRgb.toColor()
}

/**
 * Everything the chrome needs to know about the current theme.
 *
 * The gradient is the background; these are the colours drawn *on* it, derived
 * from the accent exactly as Zen's stylesheet does with `color-mix()`.
 */
@Immutable
data class KoanThemeState(
    val spec: ZenThemeSpec,
    val isDark: Boolean,
    val accent: Color,
)

val LocalKoanTheme = staticCompositionLocalOf {
    KoanThemeState(
        spec = ThemePresets.default.toSpec(),
        isDark = true,
        accent = Color(0xFF7C5CFF),
    )
}

@Composable
fun KoanTheme(
    spec: ZenThemeSpec = ThemePresets.default.toSpec(),
    systemDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val state = remember(spec, systemDark) {
        val isDark = ZenGradient.shouldBeDark(spec, systemDark)
        val primary = ZenGradient.primaryColor(spec) ?: KoanBranding.DarkRgb
        KoanThemeState(
            spec = spec,
            isDark = isDark,
            accent = ZenGradient.accentForUi(primary, isDark).toColor(),
        )
    }

    val scheme = remember(state) { colorScheme(state) }

    CompositionLocalProvider(LocalKoanTheme provides state) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = KoanShapes,
            content = content,
        )
    }
}

/**
 * `color-mix(in srgb, A p%, B q%)` is a straight sRGB lerp, which is what
 * [ZenColor.blendColors] already does — so these read as near-transcriptions
 * of the custom properties in `zen-theme.css`.
 */
private fun colorScheme(state: KoanThemeState) = with(state) {
    val brandingBg = if (isDark) KoanBranding.DarkRgb else KoanBranding.PaperRgb
    val brandingFg = if (isDark) KoanBranding.PaperRgb else KoanBranding.DarkRgb
    val primaryRgb = ZenColor.rgb(
        (accent.red * 255).toInt(),
        (accent.green * 255).toInt(),
        (accent.blue * 255).toInt(),
    )
    val white = ZenColor.rgb(255, 255, 255)
    val black = ZenColor.rgb(0, 0, 0)

    // --zen-colors-primary
    val zenPrimary = if (isDark) {
        ZenColor.blendColors(primaryRgb, brandingBg, 20.0)
    } else {
        ZenColor.blendColors(primaryRgb, black, 50.0)
    }
    // --zen-colors-secondary
    val zenSecondary = if (isDark) {
        ZenColor.blendColors(primaryRgb, brandingBg, 30.0)
    } else {
        ZenColor.blendColors(zenPrimary, white, 20.0)
    }
    // --zen-colors-tertiary
    val zenTertiary = if (isDark) {
        ZenColor.blendColors(primaryRgb, brandingBg, 1.0)
    } else {
        ZenColor.blendColors(primaryRgb, white, 2.0)
    }
    // --zen-colors-border
    val zenBorder = if (isDark) {
        ZenColor.blendColors(zenSecondary, ZenColor.rgb(79, 79, 79), 20.0)
    } else {
        ZenColor.blendColors(zenSecondary, brandingBg, 50.0)
    }

    val base = if (isDark) darkColorScheme() else lightColorScheme()
    base.copy(
        primary = accent,
        onPrimary = brandingBg.toColor(),
        // The gradient paints the real background; keep the scheme's own
        // background transparent-ish so nothing double-paints over it.
        background = ZenGradient.toolbarBase(isDark).toColor(),
        onBackground = brandingFg.toColor(),
        surface = zenTertiary.toColor(),
        onSurface = brandingFg.toColor(),
        surfaceVariant = zenSecondary.toColor(),
        onSurfaceVariant = brandingFg.toColor(),
        secondaryContainer = zenPrimary.toColor(),
        outline = zenBorder.toColor(),
        outlineVariant = zenBorder.toColor(),
    )
}
