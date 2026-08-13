package com.tyell.koan

import mozilla.components.browser.state.state.TabSessionState
import mozilla.components.concept.engine.window.WindowRequest

/**
 * `window.open` and `target=_blank`.
 *
 * Gecko doesn't open the window itself — it parks a [WindowRequest] on the
 * *opener's* content state and waits for someone to consume it. Until now
 * nobody did, so a popup link was a link that did nothing.
 *
 * Android Components ships a `WindowFeature` for the generic case, but it knows
 * nothing about Spaces: the tab it creates gets no contextId, so an OAuth popup
 * would land in the default cookie jar while the page that opened it sits in the
 * Space's jar, and the login would fail with no visible reason. Hence our own.
 */
object Popups {

    sealed interface Action {
        /** Open the popup as a tab in the opener's Space. */
        data class Open(
            val request: WindowRequest,
            val url: String,
            val parentId: String,
            val contextId: String?,
        ) : Action

        /** `window.close()` — only ever valid for a window script opened. */
        data class Close(val tabId: String) : Action
    }

    /**
     * Null means "nothing to do" — but the caller still has to consume the
     * request, otherwise the same one is handed back on every state change.
     */
    fun actionFor(tab: TabSessionState): Action? {
        val request = tab.content.windowRequest ?: return null
        return when (request.type) {
            WindowRequest.Type.CLOSE -> Action.Close(tab.id)
            WindowRequest.Type.OPEN ->
                if (isOpenable(request.url)) {
                    Action.Open(request, request.url, tab.id, tab.contextId)
                } else {
                    null
                }
        }
    }

    /**
     * Blank is allowed on purpose: the standard two-step popup opens an empty
     * window and navigates it from script afterwards, so refusing `about:blank`
     * breaks most real sign-in flows. Everything that isn't http(s) or blank —
     * `file:`, `data:`, `intent:` — a page doesn't get to open in a tab.
     */
    fun isOpenable(url: String): Boolean {
        if (url.isBlank() || url == "about:blank") return true
        // The scheme comes straight off the window.open argument, so it hasn't
        // necessarily been normalised yet.
        return url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)
    }
}
