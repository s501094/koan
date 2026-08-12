package com.tyell.koan

import com.tyell.koan.data.SpaceEntity
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The Space logic is pure functions over [BrowserState], deliberately, so it
 * can be exercised without an engine. These are the tests that would otherwise
 * require a device.
 */
class SpaceQueriesTest {

    private fun space(id: String, position: Int) =
        SpaceEntity.create(id = id, name = id, icon = "🏡", position = position)

    private fun state(vararg tabs: Pair<String, String?>, selected: String? = null) =
        BrowserState(
            tabs = tabs.map { (id, ctx) ->
                createTab(url = "https://$id.example", id = id, contextId = ctx)
            },
            selectedTabId = selected,
        )

    @Test
    fun `tabs are filtered by the space's contextId`() {
        val personal = space("personal", 0)
        val work = space("work", 1)
        val s = state(
            "a" to personal.contextId,
            "b" to work.contextId,
            "c" to personal.contextId,
        )

        assertEquals(listOf("a", "c"), SpaceQueries.tabsIn(s, personal).map { it.id })
        assertEquals(listOf("b"), SpaceQueries.tabsIn(s, work).map { it.id })
    }

    @Test
    fun `a null space sees everything, which is what happens before seeding`() {
        val s = state("a" to "x", "b" to null)
        assertEquals(2, SpaceQueries.tabsIn(s, null).size)
    }

    @Test
    fun `tabs with no contextId belong to no space`() {
        val personal = space("personal", 0)
        val s = state("orphan" to null)
        assertEquals(0, SpaceQueries.tabsIn(s, personal).size)
    }

    @Test
    fun `selected tab is invisible from a different space`() {
        val personal = space("personal", 0)
        val work = space("work", 1)
        val s = state(
            "a" to personal.contextId,
            "b" to work.contextId,
            selected = "b",
        )

        assertEquals("b", SpaceQueries.selectedTabIn(s, work)?.id)
        assertNull(SpaceQueries.selectedTabIn(s, personal))
    }

    @Test
    fun `stepping wraps in both directions`() {
        val all = listOf(space("a", 0), space("b", 1), space("c", 2))

        assertEquals("b", SpaceQueries.step(all, all[0], 1)?.id)
        assertEquals("a", SpaceQueries.step(all, all[2], 1)?.id)
        assertEquals("c", SpaceQueries.step(all, all[0], -1)?.id)
        assertEquals("b", SpaceQueries.step(all, all[2], -1)?.id)
    }

    @Test
    fun `stepping is a no-op with fewer than two spaces`() {
        val one = listOf(space("a", 0))
        assertNull(SpaceQueries.step(one, one[0], 1))
        assertNull(SpaceQueries.step(emptyList(), null, 1))
    }

    @Test
    fun `contextId is derived from the space id and is stable`() {
        val s = space("abc", 0)
        assertEquals("koan-space-abc", s.contextId)
        assertEquals(s.contextId, s.copy(name = "renamed").contextId)
    }

    @Test
    fun `theme survives a round trip through the entity columns`() {
        val original = space("a", 0)
        val tweaked = original.toThemeSpec().copy(opacity = 0.83, texture = 0.21, lightness = 44)
        val restored = original.withTheme(tweaked).toThemeSpec()

        assertEquals(0.83, restored.opacity, 1e-9)
        assertEquals(0.21, restored.texture, 1e-9)
        assertEquals(44, restored.lightness)
        assertEquals(tweaked.harmony, restored.harmony)
        assertEquals(tweaked.dots.size, restored.dots.size)
    }
}
