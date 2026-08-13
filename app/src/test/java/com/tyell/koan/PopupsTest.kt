package com.tyell.koan

import mozilla.components.browser.state.state.ContentState
import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.window.WindowRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PopupsTest {

    private class FakeRequest(
        override val type: WindowRequest.Type,
        override val url: String,
    ) : WindowRequest {
        var prepared = false
        override fun prepare(): EngineSession {
            prepared = true
            error("no engine in a unit test")
        }
    }

    private fun tab(
        request: WindowRequest?,
        contextId: String? = "zen-space-work",
    ) = TabSessionState(
        id = "opener",
        content = ContentState(url = "https://example.com", windowRequest = request),
        contextId = contextId,
    )

    @Test
    fun `a popup inherits the opener's Space`() {
        val request = FakeRequest(WindowRequest.Type.OPEN, "https://accounts.example.com/oauth")
        val action = Popups.actionFor(tab(request)) as Popups.Action.Open

        assertEquals("https://accounts.example.com/oauth", action.url)
        assertEquals("opener", action.parentId)
        assertEquals("zen-space-work", action.contextId)
        // The engine session is only touched by the caller, never while deciding.
        assertFalse(request.prepared)
    }

    @Test
    fun `the two-step oauth popup opens blank first`() {
        val action = Popups.actionFor(tab(FakeRequest(WindowRequest.Type.OPEN, "about:blank")))
        assertTrue(action is Popups.Action.Open)
    }

    @Test
    fun `a page cannot open a file url in a tab`() {
        assertNull(Popups.actionFor(tab(FakeRequest(WindowRequest.Type.OPEN, "file:///etc/hosts"))))
        assertNull(Popups.actionFor(tab(FakeRequest(WindowRequest.Type.OPEN, "intent://scan/#Intent;end"))))
        assertNull(Popups.actionFor(tab(FakeRequest(WindowRequest.Type.OPEN, "data:text/html,<h1>hi"))))
    }

    @Test
    fun `window close closes the tab that asked`() {
        val action = Popups.actionFor(tab(FakeRequest(WindowRequest.Type.CLOSE, "")))
        assertEquals(Popups.Action.Close("opener"), action)
    }

    @Test
    fun `no request, nothing to do`() {
        assertNull(Popups.actionFor(tab(null)))
    }

    @Test
    fun `a popup from a tab with no Space stays without one`() {
        val action = Popups.actionFor(
            tab(FakeRequest(WindowRequest.Type.OPEN, "https://example.com/x"), contextId = null),
        ) as Popups.Action.Open
        assertNull(action.contextId)
    }
}
