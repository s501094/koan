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

    enum class Group { Light, LightBlend, Dark, DarkBlend, Mono }

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

    /** What a Space starts on before anyone touches the picker. */
    val default: Preset = all.first { it.name == "Dusk Blend" }
}
