package com.tyell.koan.engine

import mozilla.components.browser.session.storage.AutoSave
import mozilla.components.browser.state.action.ContentAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPersistenceTest {

    private class RecordingStorage : AutoSave.Storage {
        val saved = mutableListOf<BrowserState>()
        override fun save(state: BrowserState): Boolean {
            saved.add(state)
            return true
        }
    }

    private fun storeWith(storage: RecordingStorage) = BrowserStore(
        initialState = BrowserState(
            tabs = listOf(
                createTab("https://a.example", id = "a"),
                createTab("https://b.example", id = "b"),
            ),
            selectedTabId = "a",
        ),
        middleware = listOf(saveOnRemove(storage)),
    )

    @Test
    fun `closing a tab writes the session before dispatch returns`() {
        val storage = RecordingStorage()
        val store = storeWith(storage)

        store.dispatch(TabListAction.RemoveTabAction("a"))

        assertEquals(1, storage.saved.size)
        assertEquals(listOf("b"), storage.saved.single().tabs.map { it.id })
    }

    @Test
    fun `removing several tabs writes once, with all of them gone`() {
        val storage = RecordingStorage()
        val store = storeWith(storage)

        store.dispatch(TabListAction.RemoveTabsAction(listOf("a", "b")))

        assertEquals(1, storage.saved.size)
        assertTrue(storage.saved.single().tabs.isEmpty())
    }

    @Test
    fun `removing everything writes an empty session`() {
        val storage = RecordingStorage()
        val store = storeWith(storage)

        store.dispatch(TabListAction.RemoveAllTabsAction())

        assertEquals(1, storage.saved.size)
        assertTrue(storage.saved.single().tabs.isEmpty())
        assertNull(storage.saved.single().selectedTabId)
    }

    @Test
    fun `ordinary traffic is left on the autoSave throttle`() {
        val storage = RecordingStorage()
        val store = storeWith(storage)

        store.dispatch(TabListAction.SelectTabAction("b"))
        store.dispatch(TabListAction.AddTabAction(createTab("https://c.example", id = "c")))
        store.dispatch(ContentAction.UpdateLoadingStateAction("b", false))

        assertTrue(storage.saved.isEmpty())
    }
}
