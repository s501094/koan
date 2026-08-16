package com.tyell.koan.engine

import mozilla.components.browser.session.storage.AutoSave
import mozilla.components.browser.state.action.BrowserAction
import mozilla.components.browser.state.action.CustomTabListAction
import mozilla.components.browser.state.action.TabListAction
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.lib.state.Middleware

/**
 * Which actions have to reach disk immediately.
 *
 * `autoSave` throttles. A change that lands inside the minimum interval is
 * written by a GlobalScope coroutine that sits in `delay()` first, and
 * `whenGoingToBackground` then refuses to write at all while that job is
 * pending — it logs "Skipping save, other job already in flight" and returns
 * the delayed job. Kill the app inside that window and the change dies with the
 * process.
 *
 * For an added tab that costs you a tab. For a *closed* tab it puts the tab
 * back on the next launch, which is a privacy bug: a tab you closed on purpose
 * is a tab you meant to be gone. So removals leave the throttle and are written
 * where the action is reduced; everything else can wait.
 */
object SessionPersistence {

    fun isRemoval(action: BrowserAction): Boolean = when (action) {
        is TabListAction.RemoveTabAction,
        is TabListAction.RemoveTabsAction,
        is TabListAction.RemoveAllTabsAction,
        is TabListAction.RemoveAllNormalTabsAction,
        is TabListAction.RemoveAllPrivateTabsAction,
        is CustomTabListAction.RemoveCustomTabAction,
        is CustomTabListAction.RemoveAllCustomTabsAction,
        -> true

        else -> false
    }
}

/**
 * Writes the session synchronously after a tab is removed.
 *
 * The write is blocking on purpose. `Store.dispatch` runs the whole middleware
 * chain inline on the caller's thread, so by the time this returns the snapshot
 * on disk no longer contains the tab — there is no window left for a kill to
 * land in. It costs a few milliseconds of the tap that closed the tab, only on
 * closes, which is the right side of the trade.
 */
fun saveOnRemove(storage: AutoSave.Storage): Middleware<BrowserState, BrowserAction> =
    { context, next, action ->
        next(action)
        if (SessionPersistence.isRemoval(action)) {
            storage.save(context.state)
        }
    }
