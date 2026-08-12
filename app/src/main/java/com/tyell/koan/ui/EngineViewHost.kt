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
 * [SessionFeature] is what keeps the view pointed at whichever tab is
 * selected in the store — we never call `render()` ourselves. Select a
 * different tab and the feature swaps the underlying EngineSession.
 */
@Composable
fun EngineViewHost(
    components: KoanComponents,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val engineView = remember { components.engine.createView(context) }

    val sessionFeature = remember {
        SessionFeature(
            components.store,
            components.sessionUseCases.goBack,
            components.sessionUseCases.goForward,
            engineView,
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
