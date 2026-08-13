package com.tyell.koan

import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.concept.engine.prompt.Choice
import mozilla.components.concept.engine.prompt.PromptRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptsTest {

    private fun option(
        id: String,
        label: String = id,
        selected: Boolean = false,
        enable: Boolean = true,
        separator: Boolean = false,
        children: Array<Choice>? = null,
    ) = Choice(id, enable, label, selected, separator, children)

    private fun tab(
        id: String = "tab",
        prompts: List<PromptRequest> = emptyList(),
    ) = TabSessionState(
        id = id,
        content = ContentState(url = "https://example.com", promptRequests = prompts),
    )

    private fun single(
        choices: Array<Choice>,
        onConfirm: (Choice) -> Unit = {},
        onDismiss: () -> Unit = {},
    ) = PromptRequest.SingleChoice(choices, onConfirm, onDismiss)

    @Test
    fun `a select on any tab is found, not just the selected one`() {
        val prompt = single(arrayOf(option("a")))
        val pending = Prompts.pendingIn(listOf(tab("background"), tab("noisy", listOf(prompt))))

        assertEquals("noisy", pending?.tabId)
        assertEquals(prompt, pending?.request)
    }

    @Test
    fun `no prompt means nothing to show`() {
        assertNull(Prompts.pendingIn(listOf(tab(), tab())))
    }

    @Test
    fun `prompts we don't handle are left alone`() {
        val alert = PromptRequest.Alert("title", "message", false, {}, {})
        assertNull(Prompts.pendingIn(listOf(tab("t", listOf(alert)))))
    }

    @Test
    fun `the newest prompt wins when a page raises several`() {
        val first = single(arrayOf(option("a")))
        val second = single(arrayOf(option("b")))

        assertEquals(second, Prompts.pendingIn(listOf(tab("t", listOf(first, second))))?.request)
    }

    @Test
    fun `optgroup becomes a header followed by its options`() {
        val rows = Prompts.flatten(
            arrayOf(
                option("plain"),
                option("group", children = arrayOf(option("child1"), option("child2"))),
            ),
        )

        assertEquals(listOf("plain", "group", "child1", "child2"), rows.map { it.choice.id })
        assertEquals(listOf(false, true, false, false), rows.map { it.isHeader })
    }

    @Test
    fun `headers, separators and disabled options can't be tapped`() {
        val rows = Prompts.flatten(
            arrayOf(
                option("ok"),
                option("off", enable = false),
                option("rule", separator = true),
                option("group", children = arrayOf(option("child"))),
            ),
        )

        assertEquals(
            listOf("ok", "child"),
            rows.filter(Prompts::isSelectable).map { it.choice.id },
        )
    }

    @Test
    fun `pre-ticked options include the ones inside groups`() {
        val choices = arrayOf(
            option("a", selected = true),
            option("b"),
            option("group", children = arrayOf(option("c", selected = true))),
        )

        assertEquals(setOf("a", "c"), Prompts.selectedIds(choices))
    }

    @Test
    fun `confirming a single choice answers Gecko`() {
        var answered: Choice? = null
        val choice = option("a")
        val prompt = single(arrayOf(choice), onConfirm = { answered = it })

        Prompts.confirmSingle(prompt, choice)

        assertEquals(choice, answered)
    }

    @Test
    fun `a menu choice confirms the same way`() {
        var answered: Choice? = null
        val choice = option("a")
        val prompt = PromptRequest.MenuChoice(arrayOf(choice), { answered = it }, {})

        Prompts.confirmSingle(prompt, choice)

        assertEquals(choice, answered)
        assertFalse(Prompts.isMultiple(prompt))
    }

    @Test
    fun `a multi-select confirms the whole set at once`() {
        var answered: Array<Choice>? = null
        val a = option("a")
        val b = option("b")
        val prompt = PromptRequest.MultipleChoice(arrayOf(a, b), { answered = it }, {})

        assertTrue(Prompts.isMultiple(prompt))
        Prompts.confirmMultiple(prompt, listOf(a, b))

        assertEquals(listOf("a", "b"), answered?.map { it.id })
    }

    @Test
    fun `dismissing tells Gecko, so the element isn't left wedged`() {
        var dismissed = false
        val prompt = single(arrayOf(option("a")), onDismiss = { dismissed = true })

        Prompts.dismiss(prompt)

        assertTrue(dismissed)
    }
}
