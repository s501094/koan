package com.tyell.koan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tyell.koan.Glance
import com.tyell.koan.engine.KoanComponents
import com.tyell.koan.engine.UrlInput
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The Glance card.
 *
 * Drag down to dismiss and the previewed tab is closed; drag up to promote and
 * it becomes a real tab. That pair mirrors desktop Zen's `closeGlance` and
 * `fullyOpenGlance`, and the direction is the mnemonic: down puts it away, up
 * makes it bigger.
 *
 * The page underneath is still live and still selected — Glance never changes
 * which tab the main view is rendering, which is the entire point.
 */
@Composable
fun GlanceOverlay(
    components: KoanComponents,
    tabId: String,
    title: String,
    url: String,
    onDismiss: () -> Unit,
    onPromote: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    // 0 -> hidden, 1 -> settled. Drives scrim, scale and fade together.
    val appear = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }

    val dismissPx = with(density) { 140.dp.toPx() }
    val promotePx = with(density) { 120.dp.toPx() }

    LaunchedEffect(tabId) {
        appear.animateTo(1f, tween(Glance.ANIMATION_MS))
    }

    suspend fun close(then: () -> Unit) {
        appear.animateTo(0f, tween(Glance.ANIMATION_MS / 2))
        then()
    }

    BackHandler { scope.launch { close(onDismiss) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f * appear.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { scope.launch { close(onDismiss) } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.74f)
                .graphicsLayer {
                    val scale = 0.90f + 0.10f * appear.value
                    scaleX = scale
                    scaleY = scale
                    alpha = appear.value
                    translationY = offsetY.value
                }
                .clip(RoundedCornerShape(22.dp))
                .background(colors.surface)
                // Swallow taps so they don't reach the dismiss scrim behind.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {},
        ) {
            GlanceHandle(
                title = title,
                url = url,
                onClose = { scope.launch { close(onDismiss) } },
                onPromote = { scope.launch { close(onPromote) } },
                modifier = Modifier.pointerInput(tabId) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetY.value > dismissPx -> close(onDismiss)
                                    offsetY.value < -promotePx -> close(onPromote)
                                    else -> offsetY.animateTo(
                                        0f,
                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    )
                                }
                            }
                        },
                        onDragCancel = { scope.launch { offsetY.animateTo(0f) } },
                    ) { _, drag ->
                        scope.launch {
                            // Upward travel is rubber-banded: promoting is the
                            // less common intent, so it should take more effort.
                            val next = offsetY.value + if (drag < 0) drag * 0.55f else drag
                            offsetY.snapTo(next)
                        }
                    }
                },
            )

            EngineViewHost(
                components = components,
                modifier = Modifier.fillMaxSize(),
                tabId = tabId,
            )
        }
    }
}

@Composable
private fun GlanceHandle(
    title: String,
    url: String,
    onClose: () -> Unit,
    onPromote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceVariant),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .padding(top = 8.dp)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.onSurface.copy(alpha = 0.25f)),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title.ifEmpty { UrlInput.host(url) },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = UrlInput.host(url),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = onPromote, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Default.OpenInFull,
                    contentDescription = "Open as tab",
                    tint = colors.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.size(17.dp),
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close glance",
                    tint = colors.onSurface.copy(alpha = 0.65f),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

/** Kept for the drag maths when the handle grows a snap-to-height behaviour. */
internal fun Float.roundToPx(): Int = this.roundToInt()
