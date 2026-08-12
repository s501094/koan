package com.tyell.koan.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

fun Rgb.toColor(alpha: Float = 1f): Color =
    Color(red = this[0] / 255f, green = this[1] / 255f, blue = this[2] / 255f, alpha = alpha)

/**
 * CSS gradient geometry.
 *
 * CSS measures angles from straight up, clockwise, and sizes the gradient line
 * so it spans the box corner to corner along that direction. Compose wants two
 * points. This is the conversion.
 */
private fun linearBrush(layer: ZenGradient.Layer.Linear, size: Size): Brush {
    val radians = Math.toRadians(layer.angleDeg)
    val dx = sin(radians)
    val dy = -cos(radians)

    val length = abs(size.width * dx) + abs(size.height * dy)
    val cx = size.width / 2
    val cy = size.height / 2
    val half = (length / 2).toFloat()

    return Brush.linearGradient(
        colorStops = layer.toColorStops(),
        start = Offset(cx - (dx * half).toFloat(), cy - (dy * half).toFloat()),
        end = Offset(cx + (dx * half).toFloat(), cy + (dy * half).toFloat()),
    )
}

private fun radialBrush(layer: ZenGradient.Layer.Radial, size: Size): Brush {
    val cx = size.width * layer.centerFracX
    val cy = size.height * layer.centerFracY

    // CSS `circle` with no explicit size means farthest-corner.
    val radius = maxOf(
        hypot(cx, cy),
        hypot(size.width - cx, cy),
        hypot(cx, size.height - cy),
        hypot(size.width - cx, size.height - cy),
    )

    return Brush.radialGradient(
        colorStops = layer.toColorStops(),
        center = Offset(cx, cy),
        radius = radius,
    )
}

/**
 * Builds the stop array, including the fade-out stop.
 *
 * The fade target is the layer's own colour at zero alpha, never
 * [Color.Transparent]. Compose interpolates in straight (non-premultiplied)
 * sRGB, so fading to `Color.Transparent` — which is transparent *black* —
 * drags the midtones toward grey. Browsers premultiply and do not. Fading to
 * `colour.copy(alpha = 0f)` reproduces the CSS result.
 */
private fun List<Pair<Float, Rgb>>.stopsWithFade(
    transparentFrom: Float,
): Array<Pair<Float, Color>> {
    val base = map { (pos, rgb) -> pos to rgb.toColor() }
    if (transparentFrom.isNaN()) return base.toTypedArray()
    val last = last().second.toColor(alpha = 0f)
    return (base + (transparentFrom to last)).toTypedArray()
}

private fun ZenGradient.Layer.Linear.toColorStops() = stops.stopsWithFade(transparentFrom)
private fun ZenGradient.Layer.Radial.toColorStops() = stops.stopsWithFade(transparentFrom)

/**
 * Paints a Zen theme as the background of whatever it modifies.
 *
 * Layers arrive bottom-first and are drawn in order. The grain overlay, if the
 * theme asks for one, goes on last — the same film-noise texture the desktop
 * browser tiles over its chrome.
 */
@Composable
fun Modifier.zenGradientBackground(
    spec: ZenThemeSpec,
    isDark: Boolean,
): Modifier {
    val layers = remember(spec, isDark) { ZenGradient.layers(spec, isDark) }

    val grain = if (spec.texture > 0.0) {
        ImageBitmap.imageResource(R.drawable.grain_bg)
    } else {
        null
    }
    val grainBrush = remember(grain) {
        grain?.let { ShaderBrush(ImageShader(it, TileMode.Repeated, TileMode.Repeated)) }
    }

    return this.drawBehind {
        layers.forEach { layer ->
            when (layer) {
                is ZenGradient.Layer.Solid -> drawRect(layer.color.toColor())
                is ZenGradient.Layer.Linear -> drawRect(linearBrush(layer, size))
                is ZenGradient.Layer.Radial -> drawRect(radialBrush(layer, size))
            }
        }
        grainBrush?.let { drawRect(it, alpha = spec.texture.toFloat()) }
    }
}
