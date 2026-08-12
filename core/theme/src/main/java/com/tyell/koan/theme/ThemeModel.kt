package com.tyell.koan.theme

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * A theme in Zen is not a palette — it is up to three dots placed on a colour
 * wheel, plus an opacity and a grain amount. The colours are derived from the
 * positions, which is why a saved theme stores coordinates rather than hex.
 *
 * Field names mirror the desktop format so a future import is a JSON rename
 * rather than a migration.
 */
data class ColorDot(
    val x: Double,
    val y: Double,
    val isPrimary: Boolean = false,
    val type: DotType = DotType.ExplicitLightness,
    /** Set only for dots the user entered as a literal colour. */
    val customRgb: Rgb? = null,
) {
    val isCustom: Boolean get() = customRgb != null
}

enum class DotType(val wireName: String) {
    ExplicitLightness("explicit-lightness"),
    ExplicitBlackWhite("explicit-black-white"),
}

/** Zen's harmony table, from `get colorHarmonies()`. */
enum class Harmony(val wireName: String, val angles: List<Double>) {
    Floating("floating", emptyList()),
    Complementary("complementary", listOf(180.0)),
    SingleAnalogous("singleAnalogous", listOf(310.0)),
    SplitComplementary("splitComplementary", listOf(150.0, 210.0)),
    Analogous("analogous", listOf(50.0, 310.0)),
    Triadic("triadic", listOf(120.0, 240.0)),
    ;

    companion object {
        fun fromWire(name: String): Harmony =
            entries.firstOrNull { it.wireName == name } ?: Floating
    }
}

data class ZenThemeSpec(
    val dots: List<ColorDot>,
    val lightness: Int,
    val harmony: Harmony = Harmony.Floating,
    val opacity: Double = 0.5,
    val texture: Double = 0.0,
)

/**
 * The colour wheel.
 *
 * Zen's picker is a 380x380 square. Two of its functions disagree about the
 * geometry — [colorFromPosition] pads the rect by 30 on each side and offsets
 * by half a dot, while [applyHarmony] uses the unpadded centre. That
 * inconsistency is in the original and is load-bearing: it is what the shipped
 * preset coordinates were tuned against, so both are reproduced as-is.
 */
object ColorWheel {

    const val SIZE = 380.0

    // getColorFromPosition: rect grows by padding*2, radius uses one padding.
    private const val PADDING = 30.0
    private const val DOT_HALF = 29.0
    private val cfpCenter = (SIZE + PADDING * 2) / 2
    private val cfpRadius = (SIZE + PADDING * 2 - PADDING) / 2

    // calculateCompliments: padding 0.
    private val harmCenter = SIZE / 2
    private val harmRadius = SIZE / 2

    fun colorFromPosition(
        xIn: Double,
        yIn: Double,
        lightness: Int,
        type: DotType,
    ): Rgb {
        val x = xIn + DOT_HALF
        val y = yIn + DOT_HALF

        val distance = hypot(x - cfpCenter, y - cfpCenter)
        var angle = Math.toDegrees(atan2(y - cfpCenter, x - cfpCenter))
        if (angle < 0) angle += 360

        val normalized = 1 - min(distance / cfpRadius, 1.0)

        val hue = angle
        var saturation = normalized * 100
        var light = lightness.toDouble()

        if (type != DotType.ExplicitLightness) {
            saturation = 90 + (1 - normalized) * 10
            light = Math.round((1 - normalized) * 100).toDouble()
        }
        if (type == DotType.ExplicitBlackWhite) {
            saturation = 0.0
            light = Math.round((1 - normalized) * 100).toDouble()
        }

        val c = ZenColor.hslToRgb(hue / 360, saturation / 100, light / 100)
        return ZenColor.rgb(
            c[0].coerceIn(0, 255),
            c[1].coerceIn(0, 255),
            c[2].coerceIn(0, 255),
        )
    }

    /**
     * Places companion dots around the primary at the harmony's angle offsets,
     * keeping them the same distance from centre so they share a saturation.
     */
    fun applyHarmony(primary: ColorDot, harmony: Harmony): List<ColorDot> {
        if (harmony.angles.isEmpty()) return listOf(primary)

        val baseAngle = run {
            val a = Math.toDegrees(atan2(primary.y - harmCenter, primary.x - harmCenter))
            (a + 360) % 360
        }
        val distance = min(hypot(primary.x - harmCenter, primary.y - harmCenter), harmRadius)

        return buildList {
            add(primary)
            harmony.angles.forEach { offset ->
                val radians = Math.toRadians((baseAngle + offset) % 360)
                add(
                    primary.copy(
                        x = harmCenter + distance * cos(radians),
                        y = harmCenter + distance * sin(radians),
                        isPrimary = false,
                    ),
                )
            }
        }
    }

    /** Inverse mapping, used to seed a dot from an existing colour. */
    fun positionFromColor(c: Rgb): Pair<Double, Double> {
        val hsl = ZenColor.rgbToHsl(c[0], c[1], c[2])
        val angle = Math.toRadians(hsl[0])
        val saturation = hsl[1]
        val radius = SIZE / 2
        return Pair(
            harmCenter + radius * saturation * cos(angle),
            harmCenter + radius * saturation * sin(angle),
        )
    }
}
