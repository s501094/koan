package com.tyell.koan.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

/**
 * The colour wheel, rebuilt for touch.
 *
 * Desktop Zen renders the wheel as a CSS conic gradient and drags DOM nodes on
 * top. Here the wheel is drawn once into the canvas by sampling
 * [ColorWheel.colorFromPosition] on a grid, so the swatch under your finger is
 * guaranteed to be the colour you get — the preview and the model are the same
 * function rather than two things that have to agree.
 */
@Composable
fun ColorWheelPicker(
    spec: ZenThemeSpec,
    onPrimaryMoved: (x: Double, y: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = spec.dots.firstOrNull { it.isPrimary } ?: spec.dots.firstOrNull()
    val lightness = spec.lightness
    val type = primary?.type ?: DotType.ExplicitLightness

    val callback by rememberUpdatedState(onPrimaryMoved)
    var boxSize by remember { mutableStateOf(1f) }

    // Resolution of the sampled wheel. 56 is enough that the bands are
    // invisible at phone sizes and cheap enough to redraw during a drag.
    val steps = 56

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.2f))
            .pointerInput(lightness, type) {
                fun emit(pos: Offset) {
                    val scale = ColorWheel.SIZE / size.width
                    callback(pos.x * scale, pos.y * scale)
                }
                detectTapGestures { emit(it) }
            }
            .pointerInput(lightness, type) {
                detectDragGestures { change, _ ->
                    val scale = ColorWheel.SIZE / size.width
                    callback(change.position.x * scale, change.position.y * scale)
                }
            }
            .drawBehind {
                boxSize = size.width
                val cell = size.width / steps
                val scale = ColorWheel.SIZE / size.width

                for (ix in 0 until steps) {
                    for (iy in 0 until steps) {
                        val px = (ix + 0.5f) * cell
                        val py = (iy + 0.5f) * cell
                        val c = ColorWheel.colorFromPosition(
                            (px * scale).toDouble(),
                            (py * scale).toDouble(),
                            lightness,
                            type,
                        )
                        drawRect(
                            color = c.toColor(),
                            topLeft = Offset(ix * cell, iy * cell),
                            size = androidx.compose.ui.geometry.Size(cell + 1f, cell + 1f),
                        )
                    }
                }

                // Dots, primary drawn last so it sits on top.
                val ordered = spec.dots.sortedBy { it.isPrimary }
                ordered.forEach { dot ->
                    val cx = (dot.x / ColorWheel.SIZE * size.width).toFloat()
                    val cy = (dot.y / ColorWheel.SIZE * size.width).toFloat()
                    val r = if (dot.isPrimary) 17.dp.toPx() else 11.dp.toPx()
                    val fill = ZenGradient.resolveColor(dot, spec, isDark = false)
                    drawCircle(fill.toColor(), radius = r, center = Offset(cx, cy))
                    drawCircle(
                        Color.White,
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(width = if (dot.isPrimary) 5.dp.toPx() else 3.dp.toPx()),
                    )
                }
            },
    )
}

/** Distance helper kept for the picker's hit-testing when dots become draggable. */
internal fun distance(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)
