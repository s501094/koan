package com.tyell.koan.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Golden values below were produced by running Zen's own algorithm from
 * `ZenGradientGenerator.mjs` over its own shipped preset coordinates. If one of
 * these ever fails, the port has drifted from the original, not the other way
 * round.
 */
class ZenGradientTest {

    private fun hex(x: Double, y: Double, l: Int, t: DotType = DotType.ExplicitLightness) =
        ZenColor.toHex(ColorWheel.colorFromPosition(x, y, l, t))

    @Test
    fun `light presets match the desktop palette`() {
        assertEquals("#F6EED5", hex(240.0, 240.0, 90))
        assertEquals("#F2A6D7", hex(233.0, 157.0, 80))
        assertEquals("#E8B0E8", hex(236.0, 111.0, 80))
        assertEquals("#EE77A4", hex(234.0, 173.0, 70))
        assertEquals("#F47182", hex(220.0, 187.0, 70))
        assertEquals("#E3D34F", hex(225.0, 237.0, 60))
        assertEquals("#49E9DB", hex(147.0, 195.0, 60))
        assertEquals("#5F70A0", hex(81.0, 84.0, 50))
    }

    @Test
    fun `dark presets match the desktop palette`() {
        assertEquals("#160F24", hex(171.0, 72.0, 10))
        assertEquals("#894385", hex(265.0, 79.0, 40))
        assertEquals("#82303B", hex(301.0, 176.0, 35))
        assertEquals("#863E13", hex(237.0, 210.0, 30))
        assertEquals("#287158", hex(91.0, 228.0, 30))
        assertEquals("#284C58", hex(67.0, 159.0, 25))
        assertEquals("#462D20", hex(314.0, 235.0, 20))
        assertEquals("#135340", hex(118.0, 215.0, 20))
    }

    @Test
    fun `black-white row ignores lightness and ramps on distance`() {
        val bw = DotType.ExplicitBlackWhite
        assertEquals("#BABABA", hex(340.0, 180.0, 0, bw))
        assertEquals("#808080", hex(292.5, 180.0, 0, bw))
        assertEquals("#2B2B2B", hex(225.0, 180.0, 0, bw))
    }

    @Test
    fun `analogous harmony reproduces the desktop triple`() {
        val primary = ColorDot(220.0, 187.0, isPrimary = true)
        val dots = ColorWheel.applyHarmony(primary, Harmony.Analogous)
        assertEquals(3, dots.size)
        val colors = dots.map { ZenColor.toHex(ColorWheel.colorFromPosition(it.x, it.y, 70, it.type)) }
        assertEquals(listOf("#F47182", "#F4D271", "#F471F0"), colors)
    }

    @Test
    fun `harmony angles come straight from Zen's table`() {
        assertEquals(listOf(50.0, 310.0), Harmony.Analogous.angles)
        assertEquals(listOf(120.0, 240.0), Harmony.Triadic.angles)
        assertEquals(listOf(150.0, 210.0), Harmony.SplitComplementary.angles)
        assertTrue(Harmony.Floating.angles.isEmpty())
    }

    @Test
    fun `hsl round trip`() {
        val original = ZenColor.rgb(244, 113, 130)
        val hsl = ZenColor.rgbToHsl(original[0], original[1], original[2])
        val back = ZenColor.hslToRgb(hsl[0] / 360, hsl[1], hsl[2])
        assertEquals(ZenColor.toHex(original), ZenColor.toHex(back))
    }

    @Test
    fun `luminance and contrast follow WCAG`() {
        val white = ZenColor.rgb(255, 255, 255)
        val black = ZenColor.rgb(0, 0, 0)
        assertEquals(1.0, ZenColor.luminance(white), 1e-9)
        assertEquals(0.0, ZenColor.luminance(black), 1e-9)
        assertEquals(21.0, ZenColor.contrastRatio(white, black), 1e-9)
    }

    @Test
    fun `blend at the extremes returns each side untouched`() {
        val a = ZenColor.rgb(10, 20, 30)
        val b = ZenColor.rgb(200, 210, 220)
        assertEquals(ZenColor.toHex(a), ZenColor.toHex(ZenColor.blendColors(a, b, 100.0)))
        assertEquals(ZenColor.toHex(b), ZenColor.toHex(ZenColor.blendColors(a, b, 0.0)))
    }

    @Test
    fun `dark presets choose dark chrome and light presets choose light`() {
        val ink = ThemePresets.all.first { it.name == "Ink" }.toSpec()
        val sand = ThemePresets.all.first { it.name == "Sand" }.toSpec()
        assertTrue(ZenGradient.shouldBeDark(ink, systemDark = true))
        assertFalse(ZenGradient.shouldBeDark(sand, systemDark = false))
    }

    @Test
    fun `layer count follows the dot count, as in getGradient`() {
        fun spec(n: Int) = ZenThemeSpec(
            dots = List(n) { ColorDot(220.0 + it, 187.0) },
            lightness = 70,
        )
        assertEquals(1, ZenGradient.layers(spec(0), true).size)
        assertEquals(1, ZenGradient.layers(spec(1), true).size)
        assertEquals(2, ZenGradient.layers(spec(2), true).size)
        assertEquals(3, ZenGradient.layers(spec(3), true).size)
    }

    @Test
    fun `three dots produce one linear over two radials`() {
        val spec = ThemePresets.all.first { it.name == "Coral Blend" }.toSpec()
        val layers = ZenGradient.layers(spec, isDark = false)
        // bottom-first: radial, radial, linear
        assertTrue(layers[0] is ZenGradient.Layer.Radial)
        assertTrue(layers[1] is ZenGradient.Layer.Radial)
        assertTrue(layers[2] is ZenGradient.Layer.Linear)
        assertEquals(-5.0, (layers[2] as ZenGradient.Layer.Linear).angleDeg, 1e-9)
    }

    @Test
    fun `opacity pulls the resolved colour toward the chrome base`() {
        val dot = ColorDot(220.0, 187.0, isPrimary = true)
        val opaque = ZenGradient.resolveColor(dot, ZenThemeSpec(listOf(dot), 70, opacity = 1.0), true)
        val faded = ZenGradient.resolveColor(dot, ZenThemeSpec(listOf(dot), 70, opacity = 0.0), true)
        assertEquals("#F47182", ZenColor.toHex(opaque))
        assertEquals(ZenColor.toHex(ZenGradient.toolbarBase(true)), ZenColor.toHex(faded))
    }

    @Test
    fun `preset table has the shape the desktop ships`() {
        assertEquals(41, ThemePresets.all.size)
        assertEquals(8, ThemePresets.all.count { it.group == ThemePresets.Group.Light })
        assertEquals(8, ThemePresets.all.count { it.group == ThemePresets.Group.LightBlend })
        assertEquals(8, ThemePresets.all.count { it.group == ThemePresets.Group.Dark })
        assertEquals(8, ThemePresets.all.count { it.group == ThemePresets.Group.DarkBlend })
        assertEquals(9, ThemePresets.all.count { it.group == ThemePresets.Group.Mono })
    }
}
