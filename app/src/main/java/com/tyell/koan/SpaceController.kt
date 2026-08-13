package com.tyell.koan

import com.tyell.koan.data.EssentialEntity
import com.tyell.koan.data.SpaceEntity
import com.tyell.koan.data.SpaceRepository
import com.tyell.koan.engine.KoanComponents
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.TabSessionState

/**
 * The questions you can ask about Spaces, answered without an engine.
 *
 * These are pure functions over [BrowserState] on purpose: everything that
 * decides *which tabs are where* is testable on a laptop, and only the parts
 * that actually mutate the browser need a device.
 *
 * It all rests on one fact — a tab's `contextId` is both Gecko's cookie-jar
 * partition key and its Space membership. So "the tabs in this Space" is a
 * filter, not a join, and a tab can never sit in a Space whose cookies it
 * isn't using.
 */
object SpaceQueries {

    fun tabsIn(state: BrowserState, space: SpaceEntity?): List<TabSessionState> {
        if (space == null) return state.tabs
        return state.tabs.filter { it.contextId == space.contextId }
    }

    /** Null when the selected tab belongs to some other Space. */
    fun selectedTabIn(state: BrowserState, space: SpaceEntity?): TabSessionState? {
        val selected = state.tabs.firstOrNull { it.id == state.selectedTabId } ?: return null
        if (space != null && selected.contextId != space.contextId) return null
        return selected
    }

    fun step(spaces: List<SpaceEntity>, current: SpaceEntity?, delta: Int): SpaceEntity? {
        if (spaces.size < 2 || current == null) return null
        val index = spaces.indexOfFirst { it.id == current.id }
        if (index < 0) return null
        // `zen.workspaces.wrap-around-navigation` defaults to true.
        val next = ((index + delta) % spaces.size + spaces.size) % spaces.size
        return spaces[next]
    }

    /** Zen lands you on the tab you were last looking at, not the first one. */
    fun mostRecent(tabs: List<TabSessionState>): TabSessionState? =
        tabs.maxByOrNull { it.lastAccess } ?: tabs.firstOrNull()
}

/** The half that touches the browser. */
class SpaceController(
    private val components: KoanComponents,
    private val repository: SpaceRepository,
) {
    fun tabsIn(state: BrowserState, space: SpaceEntity?) = SpaceQueries.tabsIn(state, space)

    fun selectedTabIn(state: BrowserState, space: SpaceEntity?) =
        SpaceQueries.selectedTabIn(state, space)

    fun step(spaces: List<SpaceEntity>, current: SpaceEntity?, delta: Int) =
        SpaceQueries.step(spaces, current, delta)

    fun openTab(url: String, space: SpaceEntity?, selectTab: Boolean = true): String =
        components.tabsUseCases.addTab(
            url = url,
            selectTab = selectTab,
            contextId = space?.contextId,
        )

    /**
     * The engine session already exists — Gecko built it when the page called
     * `window.open` — so the tab is created around it rather than loading a URL,
     * and `start()` is what finally lets Gecko run the load.
     */
    fun handlePopup(action: Popups.Action) {
        when (action) {
            is Popups.Action.Close -> components.tabsUseCases.removeTab(action.tabId)
            is Popups.Action.Open -> {
                components.tabsUseCases.addTab(
                    url = action.url,
                    selectTab = true,
                    startLoading = false,
                    parentId = action.parentId,
                    contextId = action.contextId,
                    engineSession = action.request.prepare(),
                )
                action.request.start()
            }
        }
    }

    /**
     * Switches Space by selecting something inside it. There is no separate
     * "current Space" in the engine — the Space you're in is simply the Space
     * of whichever tab is being rendered.
     */
    suspend fun switchTo(space: SpaceEntity, state: BrowserState) {
        repository.setActive(space.id)

        val existing = SpaceQueries.tabsIn(state, space)
        if (existing.isEmpty()) {
            openTab(MainActivity.HOME_URL, space)
            return
        }
        SpaceQueries.mostRecent(existing)?.let { components.tabsUseCases.selectTab(it.id) }
    }

    /**
     * Opens an Essential. If a tab in this Space is already on that URL it's
     * selected rather than duplicated; otherwise a new one opens. Navigating
     * away and coming back lands on the pinned URL again — what desktop Zen
     * calls "restore pinned tabs to pinned url".
     */
    fun openEssential(
        essential: EssentialEntity,
        space: SpaceEntity?,
        state: BrowserState,
    ) {
        val existing = SpaceQueries.tabsIn(state, space)
            .firstOrNull { it.content.url == essential.url }
        if (existing != null) {
            components.tabsUseCases.selectTab(existing.id)
        } else {
            openTab(essential.url, space)
        }
    }

    /** Closes every tab in a Space before the Space itself goes away. */
    suspend fun deleteSpace(space: SpaceEntity, state: BrowserState): Boolean {
        val ids = SpaceQueries.tabsIn(state, space).map { it.id }
        if (!repository.deleteSpace(space)) return false
        if (ids.isNotEmpty()) components.tabsUseCases.removeTabs(ids)
        return true
    }
}
