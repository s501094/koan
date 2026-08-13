package com.tyell.koan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tyell.koan.data.EssentialEntity
import com.tyell.koan.data.SpaceEntity
import com.tyell.koan.engine.UrlInput
import mozilla.components.browser.state.state.TabSessionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsSheet(
    tabs: List<TabSessionState>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onNewTab: () -> Unit,
    onThemeClick: () -> Unit,
    onBoostsClick: () -> Unit,
    onDismiss: () -> Unit,
    spaces: List<SpaceEntity>,
    activeSpace: SpaceEntity?,
    essentials: List<EssentialEntity>,
    canPinCurrent: Boolean,
    onSelectSpace: (SpaceEntity) -> Unit,
    onCreateSpace: () -> Unit,
    onEditSpace: (SpaceEntity) -> Unit,
    onOpenEssential: (EssentialEntity) -> Unit,
    onRemoveEssential: (EssentialEntity) -> Unit,
    onPinCurrent: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // The sheet wraps its content, and a Column will happily lay its last child
    // out past the bottom of the screen. In landscape the Spaces row and the
    // Essentials grid alone are nearly the whole height, so the tab list ran off
    // the edge and couldn't be scrolled to. Bounding the Column is what makes
    // the list's weight() mean anything.
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.9f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.45f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SpacesBar(
                spaces = spaces,
                activeId = activeSpace?.id,
                onSelect = onSelectSpace,
                onCreate = onCreateSpace,
                onLongPress = onEditSpace,
            )

            EssentialsGrid(
                essentials = essentials,
                tabs = tabs,
                canAddCurrent = canPinCurrent && essentials.size < EssentialEntity.MAX_PER_SPACE,
                onOpen = onOpenEssential,
                onRemove = onRemoveEssential,
                onAddCurrent = onPinCurrent,
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${tabs.size} ${if (tabs.size == 1) "tab" else "tabs"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onBoostsClick) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = "Boosts",
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onThemeClick) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "Theme",
                        modifier = Modifier.size(20.dp),
                    )
                }
                TextButton(onClick = onNewTab) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("New tab", Modifier.padding(start = 6.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 8.dp,
                ),
            ) {
                items(tabs, key = { it.id }) { tab ->
                    TabRow(
                        tab = tab,
                        selected = tab.id == selectedId,
                        onSelect = { onSelect(tab.id) },
                        onClose = { onClose(tab.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TabRow(
    tab: TabSessionState,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) colors.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent,
                shape,
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            val icon = tab.content.icon
            if (icon != null) {
                androidx.compose.foundation.Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    Icons.Default.Language,
                    contentDescription = null,
                    tint = colors.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = tab.content.title.ifEmpty { UrlInput.prettify(tab.content.url) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = UrlInput.host(tab.content.url),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Close tab",
                tint = colors.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
