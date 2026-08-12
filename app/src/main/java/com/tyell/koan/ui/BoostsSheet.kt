package com.tyell.koan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tyell.koan.data.BoostEntity
import com.tyell.koan.engine.UrlInput

/**
 * The Boosts panel for the site you're on.
 *
 * Zap is the headline: arm it, tap the thing you don't want, and it's gone for
 * good on that host. The CSS box underneath is the same mechanism with the
 * training wheels off.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoostsSheet(
    url: String,
    boost: BoostEntity?,
    onZapStart: () -> Unit,
    onRemoveZap: (String) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSaveCss: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colors = MaterialTheme.colorScheme
    val host = BoostEntity.patternFor(url) ?: UrlInput.host(url)

    var css by remember(boost?.id) { mutableStateOf(boost?.css.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Boost",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = host,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (boost != null) {
                    Switch(
                        checked = boost.enabled,
                        onCheckedChange = onToggleEnabled,
                    )
                }
            }

            Button(
                onClick = onZapStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Zap an element", Modifier.padding(start = 8.dp))
            }
            Text(
                text = "Then tap anything on the page to hide it here for good.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurface.copy(alpha = 0.5f),
            )

            val zapped = boost?.zapList.orEmpty()
            if (zapped.isNotEmpty()) {
                Text(
                    text = "Hidden (${zapped.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface.copy(alpha = 0.6f),
                )
                zapped.forEach { selector ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = selector,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { onRemoveZap(selector) },
                            modifier = Modifier.size(38.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Unhide",
                                tint = colors.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }

            Text(
                text = "Custom CSS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface.copy(alpha = 0.6f),
            )
            OutlinedTextField(
                value = css,
                onValueChange = { css = it },
                placeholder = { Text("body { font-size: 18px }") },
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 110.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSaveCss(css) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Apply")
                }
                if (boost != null) {
                    TextButton(onClick = onClearAll) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text("Remove boost", Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}

/** The strip shown while the picker is armed. */
@Composable
fun ZapArmedBanner(onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(colors.primary, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = "Tap an element to hide it",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.onPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            TextButton(onClick = onCancel) {
                Text("Cancel", color = colors.onPrimary)
            }
        }
    }
}
