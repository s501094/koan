package com.tyell.koan.theme

/**
 * Zen's shipped presets, read out of `browser.xhtml` in the reference build.
 *
 * These are the exact `data-position` / `data-lightness` / `data-algo` /
 * `data-num-dots` tuples from the desktop colour-page grid, so the swatches
 * here produce the same colours the desktop browser does. Names are ours —
 * the original ships them unlabelled.
 */
object ThemePresets {

    data class Preset(
        val name: String,
        val x: Double,
        val y: Double,
        val lightness: Int,
        val harmony: Harmony,
        val type: DotType = DotType.ExplicitLightness,
        val group: Group,
    ) {
        fun toSpec(opacity: Double = 0.5, texture: Double = 0.0): ZenThemeSpec {
            val primary = ColorDot(x, y, isPrimary = true, type = type)
            return ZenThemeSpec(
                dots = ColorWheel.applyHarmony(primary, harmony),
                lightness = lightness,
                harmony = harmony,
                opacity = opacity,
                texture = texture,
            )
        }
    }

    enum class Group { Light, LightBlend, Dark, DarkBlend, Mono, Designed }

    // (x, y, lightness) triples shared between the flat and analogous rows.
    private val LIGHT = listOf(
        Triple(240.0, 240.0, 90) to "Sand",
        Triple(233.0, 157.0, 80) to "Blossom",
        Triple(236.0, 111.0, 80) to "Orchid",
        Triple(234.0, 173.0, 70) to "Rose",
        Triple(220.0, 187.0, 70) to "Coral",
        Triple(225.0, 237.0, 60) to "Citron",
        Triple(147.0, 195.0, 60) to "Lagoon",
        Triple(81.0, 84.0, 50) to "Dusk",
    )

    // The analogous row re-lights two of the eight — 85 and 55 rather than 80 and 50.
    private val LIGHT_BLEND_LIGHTNESS = listOf(90, 85, 80, 70, 70, 60, 60, 55)

    private val DARK = listOf(
        Triple(171.0, 72.0, 10) to "Ink",
        Triple(265.0, 79.0, 40) to "Amethyst",
        Triple(301.0, 176.0, 35) to "Garnet",
        Triple(237.0, 210.0, 30) to "Ember",
        Triple(91.0, 228.0, 30) to "Pine",
        Triple(67.0, 159.0, 25) to "Slate",
        Triple(314.0, 235.0, 20) to "Bark",
        Triple(118.0, 215.0, 20) to "Moss",
    )

    private val MONO_X = listOf(180.0, 202.5, 225.0, 247.5, 270.0, 292.5, 315.0, 337.5, 340.0)

    val all: List<Preset> = buildList {
        LIGHT.forEach { (pos, name) ->
            add(Preset(name, pos.first, pos.second, pos.third, Harmony.Floating, group = Group.Light))
        }
        LIGHT.forEachIndexed { i, (pos, name) ->
            add(
                Preset(
                    "$name Blend", pos.first, pos.second,
                    LIGHT_BLEND_LIGHTNESS[i], Harmony.Analogous, group = Group.LightBlend,
                ),
            )
        }
        DARK.forEach { (pos, name) ->
            add(Preset(name, pos.first, pos.second, pos.third, Harmony.Floating, group = Group.Dark))
        }
        DARK.forEach { (pos, name) ->
            add(
                Preset(
                    "$name Blend", pos.first, pos.second,
                    pos.third, Harmony.Analogous, group = Group.DarkBlend,
                ),
            )
        }
        MONO_X.forEachIndexed { i, x ->
            add(
                Preset(
                    "Mono ${i + 1}", x, 180.0, 0, Harmony.Floating,
                    type = DotType.ExplicitBlackWhite, group = Group.Mono,
                ),
            )
        }
    }

    /**
     * Fifteen themes of our own, alongside Zen's forty-one.
     *
     * Designed the other way round from Zen's: rather than placing a dot and
     * seeing what colour comes out, pick the hue, saturation and lightness you
     * want and invert [ColorWheel.colorFromPosition] to find the position that
     * produces it. `tools/design_themes.py` does the inversion and renders the
     * result; the coordinates below are its output.
     *
     * Harmony is load-bearing rather than decorative. Triadic and
     * SplitComplementary fling their companion dots most of the way round the
     * wheel, which is right for a deliberately polychrome theme and wrong for
     * one named after a colour — the first pass had an "Ember" that rendered
     * teal. Colour-named themes therefore use Floating or the tighter
     * Analogous / SingleAnalogous.
     */
    private fun designed(
        name: String,
        x: Double,
        y: Double,
        lightness: Int,
        harmony: Harmony,
    ) = Preset(name, x, y, lightness, harmony, group = Group.Designed)

    val designed: List<Preset> = listOf(
        designed("Aurora", 134.9, 202.9, 62, Harmony.Analogous),
        designed("Nocturne", 161.6, 110.1, 26, Harmony.SingleAnalogous),
        designed("Sakura", 263.2, 161.8, 82, Harmony.Analogous),
        designed("Ember", 230.4, 202.3, 38, Harmony.SingleAnalogous),
        designed("Moss", 120.3, 281.5, 30, Harmony.SingleAnalogous),
        designed("Vellum", 282.4, 273.3, 88, Harmony.SingleAnalogous),
        designed("Tidepool", 131.9, 174.0, 55, Harmony.Analogous),
        designed("Amber Dusk", 233.0, 223.8, 42, Harmony.SingleAnalogous),
        designed("Iris", 200.0, 105.4, 74, Harmony.Analogous),
        designed("Basalt", 52.8, 75.0, 26, Harmony.Floating),
        designed("Citrus", 211.0, 235.9, 70, Harmony.Analogous),
        designed("Wine", 256.2, 156.4, 30, Harmony.SingleAnalogous),
        designed("Glacier", 89.9, 146.0, 84, Harmony.SingleAnalogous),
        designed("Terracotta", 274.5, 211.8, 46, Harmony.SingleAnalogous),
        designed("Void", 175.6, 81.4, 12, Harmony.Triadic),
    )

    /** Everything the picker offers: ours first, then Zen's. */
    val everything: List<Preset> = designed + all

    /** What a Space starts on before anyone touches the picker. */
    val default: Preset = designed.first { it.name == "Nocturne" }
}
