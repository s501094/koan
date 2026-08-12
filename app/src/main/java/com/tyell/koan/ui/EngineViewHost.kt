package com.tyell.koan.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.tyell.koan.engine.KoanComponents
import mozilla.components.feature.session.SessionFeature

/**
 * Hosts Gecko's rendering surface inside Compose.
 *
 * [SessionFeature] is what keeps the view pointed at the right tab — we never
 * call `render()` ourselves. With [tabId] null it follows the store's selection;
 * with a [tabId] it stays pinned to that one tab, which is how Glance shows a
 * second page over the top of the one you're already on.
 */
@Composable
fun EngineViewHost(
    components: KoanComponents,
    modifier: Modifier = Modifier,
    tabId: String? = null,
) {
    val context = LocalContext.current

    // Keyed on tabId: a Glance overlay gets its own engine view, and closing it
    // must tear that view down rather than recycle it onto another tab.
    val engineView = remember(tabId) { components.engine.createView(context) }

    val sessionFeature = remember(tabId, engineView) {
        SessionFeature(
            components.store,
            components.sessionUseCases.goBack,
            components.sessionUseCases.goForward,
            engineView,
            tabId,
        )
    }

    DisposableEffect(engineView) {
        engineView.onCreate()
        engineView.onStart()
        engineView.onResume()
        sessionFeature.start()

        onDispose {
            sessionFeature.stop()
            engineView.onPause()
            engineView.onStop()
            engineView.onDestroy()
            engineView.release()
        }
    }

    AndroidView(
        factory = { engineView.asView() },
        modifier = modifier,
    )
}
