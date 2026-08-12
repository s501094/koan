package com.tyell.koan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.tyell.koan.engine.UrlInput
import com.tyell.koan.ui.KoanApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val components by lazy { koanComponents }
    private val spaces by lazy { koanSpaces }
    private val controller by lazy { koanSpaceController }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        restoreOrOpenSession(intent)

        // Persist tab state every 30s and on every tab/URL change, so a kill
        // by the OS doesn't lose the session.
        components.sessionStorage
            .autoSave(components.store, 30, TimeUnit.SECONDS)
            .periodicallyInForeground()
            .whenGoingToBackground()
            .whenSessionsChange()

        setContent {
            KoanApp(components, spaces, controller)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val url = urlFromIntent(intent) ?: return
        lifecycleScope.launch {
            controller.openTab(url, spaces.activeSpace.first())
        }
    }

    /**
     * Order matters here. Spaces have to exist before any tab can be given a
     * contextId, and the session has to be restored before we can tell whether
     * the active Space already has tabs in it.
     */
    private fun restoreOrOpenSession(intent: Intent) {
        val incoming = urlFromIntent(intent)

        lifecycleScope.launch {
            spaces.ensureSeeded()
            components.tabsUseCases.restore(components.sessionStorage)

            val active = spaces.activeSpace.first()
            val state = components.store.state

            when {
                incoming != null -> controller.openTab(incoming, active)

                // A restored session can hold tabs for several Spaces. Land in
                // the active one rather than wherever the store happens to point.
                else -> {
                    val inSpace = controller.tabsIn(state, active)
                    if (inSpace.isEmpty()) {
                        controller.openTab(HOME_URL, active)
                    } else if (controller.selectedTabIn(state, active) == null) {
                        val target = inSpace.maxByOrNull { it.lastAccess } ?: inSpace.first()
                        components.tabsUseCases.selectTab(target.id)
                    }
                }
            }
        }
    }

    private fun urlFromIntent(intent: Intent): String? = when (intent.action) {
        Intent.ACTION_VIEW -> intent.dataString
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let(UrlInput::toUrl)
        else -> null
    }

    companion object {
        const val HOME_URL = "https://duckduckgo.com/"
    }
}
