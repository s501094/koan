package com.tyell.koan.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tyell.koan.design.KoanDimens
import com.tyell.koan.engine.UrlInput

/**
 * One bar, at the bottom, in thumb reach.
 *
 * Desktop Zen collapses everything into a single toolbar
 * (`zen.view.use-single-toolbar`); the phone equivalent is a single pill that
 * shows the domain and nothing else until you touch it.
 */
@Composable
fun Toolbar(
    url: String,
    isLoading: Boolean,
    isSecure: Boolean,
    tabCount: Int,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onTabsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(KoanDimens.toolbarHeight)
                .background(colors.surfaceVariant, RoundedCornerShape(26.dp))
                .border(1.dp, colors.outline.copy(alpha = 0.25f), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (editing) {
                UrlEditor(
                    initial = url,
                    onSubmit = {
                        onNavigate(it)
                        onEditingChange(false)
                    },
                    onCancel = { onEditingChange(false) },
                )
            } else {
                UrlDisplay(
                    url = url,
                    isSecure = isSecure,
                    isLoading = isLoading,
                    onClick = { onEditingChange(true) },
                    onReload = onReload,
                    onStop = onStop,
                )
            }
        }

        AnimatedVisibility(
            visible = !editing,
            enter = expandHorizontally(),
            exit = shrinkHorizontally(),
        ) {
            TabCountButton(count = tabCount, onClick = onTabsClick)
        }
    }
}

@Composable
private fun UrlDisplay(
    url: String,
    isSecure: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSecure) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = colors.onSurface.copy(alpha = 0.45f),
                modifier = Modifier
                    .size(13.dp)
                    .padding(end = 1.dp),
            )
            Box(Modifier.width(7.dp))
        }

        Text(
            // `zen.urlbar.show-domain-only-in-sidebar` — the domain is the only
            // part worth the horizontal space on a phone.
            text = UrlInput.prettify(url).ifEmpty { "Search or enter address" },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (url.isEmpty()) colors.onSurface.copy(alpha = 0.5f) else colors.onSurface,
            modifier = Modifier.weight(1f),
        )

        IconButton(
            onClick = if (isLoading) onStop else onReload,
            modifier = Modifier.size(38.dp),
        ) {
            Icon(
                imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                contentDescription = if (isLoading) "Stop" else "Reload",
                tint = colors.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun UrlEditor(
    initial: String,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var value by remember {
        mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length)))
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            placeholder = { Text("Search or enter address") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go,
                autoCorrectEnabled = false,
            ),
            keyboardActions = KeyboardActions(onGo = { onSubmit(value.text) }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )

        IconButton(onClick = onCancel, modifier = Modifier.size(42.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Cancel",
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun TabCountButton(count: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // A tick of scale when the count changes is enough feedback that a
    // background tab opened, without a toast getting in the way.
    val scale by animateFloatAsState(if (count > 0) 1f else 0.9f, label = "tabCount")

    Box(
        modifier = Modifier
            .size(KoanDimens.toolbarHeight)
            .background(colors.surfaceVariant, RoundedCornerShape(15.dp))
            .border(1.dp, colors.outline.copy(alpha = 0.25f), RoundedCornerShape(15.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            fontSize = (14 * scale).sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface,
        )
    }
}
