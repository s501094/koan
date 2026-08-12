package com.tyell.koan.theme

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** RGB triple, each channel 0..255. */
typealias Rgb = IntArray

/**
 * Colour maths ported verbatim from Zen's `ZenGradientGenerator.mjs`.
 *
 * "Verbatim" is deliberate. These functions decide which colours a saved theme
 * produces, so drifting from the original — even by a rounding mode — would
 * make an imported desktop theme render differently here. The unit tests pin
 * the outputs against values computed from the original JavaScript.
 */
object ZenColor {

    fun rgb(r: Int, g: Int, b: Int): Rgb = intArrayOf(r, g, b)

    fun hueToRgb(p: Double, q: Double, tIn: Double): Double {
        var t = tIn
        if (t < 0) t += 1
        if (t > 1) t -= 1
        return when {
            t < 1.0 / 6 -> p + (q - p) * 6 * t
            t < 1.0 / 2 -> q
            t < 2.0 / 3 -> p + (q - p) * (2.0 / 3 - t) * 6
            else -> p
        }
    }

    /** h, s, l all in 0..1. */
    fun hslToRgb(h: Double, s: Double, l: Double): Rgb {
        val r: Double
        val g: Double
        val b: Double
        if (s == 0.0) {
            r = l; g = l; b = l
        } else {
            val q = if (l < 0.5) l * (1 + s) else l + s - l * s
            val p = 2 * l - q
            r = hueToRgb(p, q, h + 1.0 / 3)
            g = hueToRgb(p, q, h)
            b = hueToRgb(p, q, h - 1.0 / 3)
        }
        return rgb(jsRound(r * 255), jsRound(g * 255), jsRound(b * 255))
    }

    /** Returns hue in degrees, saturation and lightness in 0..1. */
    fun rgbToHsl(rIn: Int, gIn: Int, bIn: Int): DoubleArray {
        val r = rIn / 255.0
        val g = gIn / 255.0
        val b = bIn / 255.0
        val mx = maxOf(r, g, b)
        val mn = minOf(r, g, b)
        val d = mx - mn
        val h = when {
            d == 0.0 -> 0.0
            mx == r -> ((g - b) / d) % 6
            mx == g -> (b - r) / d + 2
            else -> (r - g) / d + 4
        }
        val l = (mn + mx) / 2
        val s = if (d == 0.0) 0.0 else d / (1 - abs(2 * l - 1))
        return doubleArrayOf(h * 60, s, l)
    }

    /** Relative luminance, per the Wikipedia definition Zen cites. */
    fun luminance(c: Rgb): Double {
        val a = DoubleArray(3) { i ->
            val v = c[i] / 255.0
            if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return a[0] * 0.2126 + a[1] * 0.7152 + a[2] * 0.0722
    }

    fun contrastRatio(a: Rgb, b: Rgb): Double {
        val l1 = luminance(a)
        val l2 = luminance(b)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    /** [percentage] is how much of [a] survives, 0..100. */
    fun blendColors(a: Rgb, b: Rgb, percentage: Double): Rgb {
        val p = percentage / 100
        return rgb(
            jsRound(a[0] * p + b[0] * (1 - p)),
            jsRound(a[1] * p + b[1] * (1 - p)),
            jsRound(a[2] * p + b[2] * (1 - p)),
        )
    }

    fun hexToRgb(hexIn: String): Rgb {
        var hex = hexIn.removePrefix("#")
        if (hex.length == 3) hex = hex.map { "$it$it" }.joinToString("")
        return rgb(
            hex.substring(0, 2).toInt(16),
            hex.substring(2, 4).toInt(16),
            hex.substring(4, 6).toInt(16),
        )
    }

    fun toHex(c: Rgb): String = "#%02X%02X%02X".format(c[0], c[1], c[2])

    /**
     * JavaScript's Math.round rounds .5 toward +infinity; Kotlin's
     * roundToInt rounds .5 away from zero. They only disagree on negative
     * halves, which colour maths never produces — but the blend and HSL
     * conversions are close enough to boundaries that it's worth being exact.
     */
    private fun jsRound(v: Double): Int = kotlin.math.floor(v + 0.5).toInt()
}
