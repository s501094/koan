package com.tyell.koan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tyell.koan.data.SpaceEntity

/**
 * Create or edit a Space.
 *
 * Zen ships a full emoji picker here; a curated dozen covers the cases people
 * actually use and costs nothing. Swapping in a real picker later means
 * replacing one Row.
 */
private val ICONS = listOf(
    "🏡", "💼", "📚", "🎨", "🛠", "🎮", "🎧", "🛒", "✈️", "🧪", "💬", "🌙",
)

@Composable
fun SpaceEditDialog(
    existing: SpaceEntity?,
    canDelete: Boolean,
    onConfirm: (name: String, icon: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var icon by remember { mutableStateOf(existing?.icon ?: ICONS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New space" else "Edit space") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    ICONS.forEach { candidate ->
                        val selected = candidate == icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    } else {
                                        Color.Transparent
                                    },
                                    CircleShape,
                                )
                                .clickable { icon = candidate },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(candidate, fontSize = 19.sp)
                        }
                    }
                }

                if (existing != null && canDelete) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Text(
                            "Delete space and its tabs",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim().ifEmpty { "Space" }, icon) },
            ) {
                Text(if (existing == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
