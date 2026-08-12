package com.tyell.koan.theme

/**
 * The gradient composer — a port of `getGradient()` and `shouldBeDarkMode()`.
 *
 * Desktop Zen emits CSS strings here. We emit a description of layers instead,
 * because Compose has no CSS parser and because a layer list is testable
 * without a renderer.
 *
 * One simplification is genuine rather than a shortcut: `#getSingleRGBColor`
 * branches on `canBeTransparent`, which is true only on macOS and Windows Mica
 * where the window itself is translucent. Android has no such mode, so the
 * branch collapses to "blend the dot with the toolbar base at the user's
 * opacity, emit it opaque" and all the rgba juggling downstream disappears.
 */
object ZenGradient {

    /** Zen's fallback chrome colours when a Space has no dots. */
    private val EMPTY_DARK = ZenColor.hexToRgb("#131313")
    private val EMPTY_LIGHT = ZenColor.hexToRgb("#e9e9e9")

    /** `getToolbarModifiedBaseRaw()` with sidebar transparency unavailable. */
    fun toolbarBase(isDark: Boolean): Rgb =
        if (isDark) ZenColor.rgb(23, 23, 26) else ZenColor.rgb(240, 240, 244)

    sealed interface Layer {
        /** CSS `linear-gradient(<angle>deg, ...)`; 0deg points up, clockwise. */
        data class Linear(
            val angleDeg: Double,
            val stops: List<Pair<Float, Rgb>>,
            val transparentFrom: Float,
        ) : Layer

        /** CSS `radial-gradient(circle at <x> <y>, ...)`, farthest-corner sized. */
        data class Radial(
            val centerFracX: Float,
            val centerFracY: Float,
            val stops: List<Pair<Float, Rgb>>,
            val transparentFrom: Float,
        ) : Layer

        data class Solid(val color: Rgb) : Layer
    }

    /**
     * Layers are returned bottom-first, the reverse of CSS's paint order, so a
     * caller can simply draw them in sequence.
     */
    fun layers(spec: ZenThemeSpec, isDark: Boolean): List<Layer> {
        val resolved = spec.dots.map { resolveColor(it, spec, isDark) }

        if (resolved.isEmpty()) {
            return listOf(Layer.Solid(if (isDark) EMPTY_DARK else EMPTY_LIGHT))
        }
        if (resolved.size == 1) {
            return listOf(Layer.Solid(resolved[0]))
        }

        val rotation = -45.0

        // Any custom colour drops the whole theme to an even linear ramp.
        if (spec.dots.any { it.isCustom }) {
            val stops = resolved.mapIndexed { i, c ->
                (i.toFloat() / (resolved.size - 1)) to c
            }
            return listOf(Layer.Linear(rotation, stops, transparentFrom = Float.NaN))
        }

        if (resolved.size == 2) {
            // CSS order is [c0 @135deg, c1 @-45deg], first painted on top.
            return listOf(
                Layer.Linear(rotation, listOf(0f to resolved[1]), transparentFrom = 1f),
                Layer.Linear(rotation + 180, listOf(0f to resolved[0]), transparentFrom = 1f),
            )
        }

        // Three dots: one shallow linear plus two corner-anchored radials.
        val c1 = resolved[2]
        val c2 = resolved[0]
        val c3 = resolved[1]
        return listOf(
            Layer.Radial(0f, 0f, listOf(0.10f to c2), transparentFrom = 0.70f),
            Layer.Radial(0.95f, 0f, listOf(0f to c3), transparentFrom = 0.75f),
            Layer.Linear(-5.0, listOf(0.10f to c1), transparentFrom = 0.80f),
        )
    }

    /**
     * `#getSingleRGBColor`, Android branch. Blends the dot toward the chrome
     * base by the inverse of the user's opacity, then returns it opaque.
     */
    fun resolveColor(dot: ColorDot, spec: ZenThemeSpec, isDark: Boolean): Rgb {
        dot.customRgb?.let { return it }
        val raw = ColorWheel.colorFromPosition(dot.x, dot.y, spec.lightness, dot.type)
        return ZenColor.blendColors(raw, toolbarBase(isDark), spec.opacity * 100)
    }

    /**
     * `shouldBeDarkMode`. Composites 80%-alpha white and 80%-alpha black text
     * over the accent and picks whichever wins on contrast. Zen names the
     * white one `darkText`, which reads backwards — it means "the text used in
     * dark mode".
     */
    fun shouldBeDark(spec: ZenThemeSpec, systemDark: Boolean): Boolean {
        val primary = primaryColor(spec) ?: return systemDark

        val bg = ZenColor.blendColors(
            toolbarBase(systemDark),
            primary,
            (1 - spec.opacity) * 100,
        )

        val whiteOver = ZenColor.blendColors(bg, ZenColor.rgb(255, 255, 255), (1 - 0.8) * 100)
        val blackOver = ZenColor.blendColors(bg, ZenColor.rgb(0, 0, 0), (1 - 0.8) * 100)

        return ZenColor.contrastRatio(bg, whiteOver) > ZenColor.contrastRatio(bg, blackOver)
    }

    /** `getPrimaryColor` — the flagged dot, else the middle one. */
    fun primaryColor(spec: ZenThemeSpec): Rgb? {
        if (spec.dots.isEmpty()) return null
        val dot = spec.dots.firstOrNull { it.isPrimary } ?: spec.dots[spec.dots.size / 2]
        return dot.customRgb
            ?: ColorWheel.colorFromPosition(dot.x, dot.y, spec.lightness, dot.type)
    }

    /**
     * `getAccentColorForUI`. In dark mode the accent is used as-is; in light
     * mode it is pushed toward a usable saturation and lightness so it stays
     * legible against pale chrome.
     */
    fun accentForUi(accent: Rgb, isDark: Boolean): Rgb {
        if (isDark) return accent
        val hsl = ZenColor.rgbToHsl(accent[0], accent[1], accent[2])
        val saturation = minOf(1.0, hsl[1] + 0.3)
        val lightness = hsl[2] * 0.4 + 0.42 * 0.6
        return ZenColor.hslToRgb(hsl[0] / 360, saturation, lightness)
    }
}
