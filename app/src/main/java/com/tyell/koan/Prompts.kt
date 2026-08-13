package com.tyell.koan

import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.concept.engine.prompt.Choice
import mozilla.components.concept.engine.prompt.PromptRequest

/**
 * `<select>` dropdowns.
 *
 * Gecko never draws a dropdown itself. It raises a prompt, Android Components
 * turns that into a [PromptRequest] parked on the tab's content state, and waits
 * for someone to answer it. Nobody did, so tapping a `<select>` did nothing at
 * all — the request just sat there until Gecko gave up on it.
 *
 * mozac ships a `PromptFeature` for this, but it wants a FragmentManager and
 * brings its own save-password and credit-card capture UI along for the ride.
 * Neither belongs in this app, so we read the state ourselves and render the
 * choices in Compose.
 */
object Prompts {

    /** A choice prompt waiting on an answer, and the tab that raised it. */
    data class Pending(val tabId: String, val request: PromptRequest)

    /**
     * One row in the sheet. `<optgroup>` arrives as a [Choice] whose children
     * hold the real options, so groups become an unselectable header followed by
     * their members rather than a nested list.
     */
    data class Row(val choice: Choice, val isHeader: Boolean, val inGroup: Boolean = false)

    /** Only choice prompts — the rest of the prompt family isn't handled yet. */
    fun pendingIn(tabs: List<TabSessionState>): Pending? {
        for (tab in tabs) {
            val request = tab.content.promptRequests.lastOrNull(::isChoice) ?: continue
            return Pending(tab.id, request)
        }
        return null
    }

    fun isChoice(request: PromptRequest): Boolean =
        request is PromptRequest.SingleChoice ||
            request is PromptRequest.MultipleChoice ||
            request is PromptRequest.MenuChoice

    fun choicesOf(request: PromptRequest): Array<Choice> = when (request) {
        is PromptRequest.SingleChoice -> request.choices
        is PromptRequest.MultipleChoice -> request.choices
        is PromptRequest.MenuChoice -> request.choices
        else -> emptyArray()
    }

    /** Multi-select stays open and confirms a set; the others close on a tap. */
    fun isMultiple(request: PromptRequest): Boolean = request is PromptRequest.MultipleChoice

    fun flatten(choices: Array<Choice>): List<Row> = buildList {
        for (choice in choices) {
            if (choice.isGroupType) {
                add(Row(choice, isHeader = true))
                choice.children?.forEach { add(Row(it, isHeader = false, inGroup = true)) }
            } else {
                add(Row(choice, isHeader = false))
            }
        }
    }

    /** Pre-ticked options for a multi-select, group members included. */
    fun selectedIds(choices: Array<Choice>): Set<String> =
        flatten(choices).filter { !it.isHeader && it.choice.selected }
            .map { it.choice.id }
            .toSet()

    /**
     * Separators are `<hr>` inside a select. They're rows, not options, so they
     * can't be tapped — and neither can a disabled option or a group header.
     */
    fun isSelectable(row: Row): Boolean =
        !row.isHeader && !row.choice.isASeparator && row.choice.enable

    fun confirmSingle(request: PromptRequest, choice: Choice) {
        when (request) {
            is PromptRequest.SingleChoice -> request.onConfirm(choice)
            is PromptRequest.MenuChoice -> request.onConfirm(choice)
            else -> Unit
        }
    }

    fun confirmMultiple(request: PromptRequest, choices: List<Choice>) {
        if (request is PromptRequest.MultipleChoice) request.onConfirm(choices.toTypedArray())
    }

    /**
     * Dismissing matters as much as confirming: the page is blocked on an
     * answer, and a prompt we drop without telling Gecko leaves the element
     * wedged until navigation.
     */
    fun dismiss(request: PromptRequest) {
        (request as? PromptRequest.Dismissible)?.onDismiss?.invoke()
    }
}
