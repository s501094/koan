package com.tyell.koan.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tyell.koan.data.EssentialEntity
import com.tyell.koan.design.KoanDimens
import com.tyell.koan.engine.UrlInput
import mozilla.components.browser.state.state.TabSessionState

/**
 * Essentials — the Space's pinned favourites.
 *
 * Four across, wrapping, capped at Zen's twelve. Not a LazyGrid: the list is
 * bounded and tiny, and nesting a lazy grid inside a scrolling bottom sheet
 * means fighting over which one owns the scroll.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EssentialsGrid(
    essentials: List<EssentialEntity>,
    tabs: List<TabSessionState>,
    canAddCurrent: Boolean,
    onOpen: (EssentialEntity) -> Unit,
    onRemove: (EssentialEntity) -> Unit,
    onAddCurrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (essentials.isEmpty() && !canAddCurrent) return

    val columns = 4
    val cells: List<EssentialEntity?> = essentials + if (canAddCurrent) listOf(null) else emptyList()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        cells.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { essential ->
                    Box(Modifier.weight(1f)) {
                        if (essential == null) {
                            AddTile(onClick = onAddCurrent)
                        } else {
                            EssentialTile(
                                essential = essential,
                                // Favicons come from whichever open tab happens
                                // to be on that URL; there is no icon cache yet.
                                icon = tabs.firstOrNull { it.content.url == essential.url }
                                    ?.content?.icon,
                                onClick = { onOpen(essential) },
                                onLongClick = { onRemove(essential) },
                            )
                        }
                    }
                }
                // Keep the last row's tiles the same width as a full row's.
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EssentialTile(
    essential: EssentialEntity,
    icon: android.graphics.Bitmap?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(KoanDimens.essentialsIcon + 14.dp)
                .background(colors.surfaceVariant, shape)
                .border(1.dp, colors.outline.copy(alpha = 0.25f), shape)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                androidx.compose.foundation.Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Text(
                    text = UrlInput.host(essential.url).take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        Text(
            text = essential.title.ifEmpty { UrlInput.host(essential.url) },
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurface.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}

@Composable
private fun AddTile(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(KoanDimens.essentialsIcon + 14.dp)
                .border(1.dp, colors.outline.copy(alpha = 0.35f), shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Pin this page",
                tint = colors.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = "Pin",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.padding(top = 5.dp),
        )
    }
}
