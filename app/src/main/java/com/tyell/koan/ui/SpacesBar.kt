package com.tyell.koan.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tyell.koan.data.SpaceEntity
import com.tyell.koan.theme.ZenGradient
import com.tyell.koan.theme.zenGradientBackground

/**
 * The Space switcher.
 *
 * Each chip wears its own Space's gradient, which is the point — on the desktop
 * you know which Space you're in because the whole browser changes colour, and
 * the chip is a preview of that.
 */
@Composable
fun SpacesBar(
    spaces: List<SpaceEntity>,
    activeId: String?,
    onSelect: (SpaceEntity) -> Unit,
    onCreate: () -> Unit,
    onLongPress: (SpaceEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        spaces.forEach { space ->
            SpaceChip(
                space = space,
                selected = space.id == activeId,
                onClick = { onSelect(space) },
                onLongClick = { onLongPress(space) },
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(13.dp),
                )
                .clickable(onClick = onCreate),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "New space",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpaceChip(
    space: SpaceEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(13.dp)

    val spec = remember(space) { space.toThemeSpec() }
    // The chip's own gradient decides its label colour, not the app theme —
    // a Work space in pale sand sits next to a Personal space in deep ink.
    val onChip = remember(spec) {
        if (ZenGradient.shouldBeDark(spec, systemDark = false)) Color.White else Color.Black
    }

    val border by animateColorAsState(
        if (selected) colors.primary else colors.outline.copy(alpha = 0.3f),
        label = "spaceBorder",
    )

    Row(
        modifier = Modifier
            .clip(shape)
            .zenGradientBackground(spec, isDark = ZenGradient.shouldBeDark(spec, false))
            .border(if (selected) 2.dp else 1.dp, border, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(space.icon, fontSize = 15.sp)
        if (selected) {
            Text(
                text = space.name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = onChip,
            )
        }
    }
}
