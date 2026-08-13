package com.tyell.koan.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tyell.koan.Prompts
import mozilla.components.concept.engine.prompt.Choice
import mozilla.components.concept.engine.prompt.PromptRequest

/**
 * The dropdown Gecko won't draw. One tap answers a `<select>`; a `multiple`
 * select keeps the sheet open and confirms the whole set on Done.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoicePromptSheet(
    request: PromptRequest,
    onConfirmSingle: (Choice) -> Unit,
    onConfirmMultiple: (List<Choice>) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val choices = remember(request) { Prompts.choicesOf(request) }
    val rows = remember(request) { Prompts.flatten(choices) }
    val multiple = Prompts.isMultiple(request)

    // A long <select> — timezones, country lists — is taller than the screen,
    // so the list has to be bounded before it can scroll. Same trap as TabsSheet.
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.9f).dp

    var ticked by remember(request) { mutableStateOf(Prompts.selectedIds(choices)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding(),
        ) {
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                itemsIndexed(rows) { index, row ->
                    val choice = row.choice
                    when {
                        choice.isASeparator -> HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                        )

                        row.isHeader -> Text(
                            text = choice.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = if (index == 0) 12.dp else 20.dp,
                                bottom = 4.dp,
                            ),
                        )

                        else -> ChoiceRow(
                            choice = choice,
                            multiple = multiple,
                            checked = if (multiple) choice.id in ticked else choice.selected,
                            indented = row.inGroup,
                            onClick = {
                                if (multiple) {
                                    ticked = if (choice.id in ticked) {
                                        ticked - choice.id
                                    } else {
                                        ticked + choice.id
                                    }
                                } else {
                                    onConfirmSingle(choice)
                                }
                            },
                        )
                    }
                }
            }

            if (multiple) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = {
                            val picked = Prompts.flatten(choices)
                                .filter { Prompts.isSelectable(it) && it.choice.id in ticked }
                                .map { it.choice }
                            onConfirmMultiple(picked)
                        },
                    ) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    choice: Choice,
    multiple: Boolean,
    checked: Boolean,
    indented: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (choice.enable) it.clickable(onClick = onClick) else it }
            .padding(
                start = if (indented) 32.dp else 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (multiple) {
            Checkbox(checked = checked, onCheckedChange = null, enabled = choice.enable)
        } else {
            RadioButton(selected = checked, onClick = null, enabled = choice.enable)
        }
        Text(
            text = choice.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (choice.enable) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
