package com.tyell.koan

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.tyell.koan.ui.KoanApp
import com.tyell.koan.engine.UrlInput
import com.tyell.koan.theme.KoanTheme
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.selectedTab
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val components by lazy { koanComponents }

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
            KoanTheme {
                KoanApp(components)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        urlFromIntent(intent)?.let {
            components.tabsUseCases.addTab(it, selectTab = true)
        }
    }

    private fun restoreOrOpenSession(intent: Intent) {
        val incoming = urlFromIntent(intent)

        lifecycleScope.launch {
            components.tabsUseCases.restore(components.sessionStorage)

            when {
                incoming != null ->
                    components.tabsUseCases.addTab(incoming, selectTab = true)

                components.store.state.tabs.isEmpty() ->
                    components.tabsUseCases.addTab(HOME_URL, selectTab = true)

                components.store.state.selectedTab == null ->
                    components.store.state.tabs.lastOrNull()?.let {
                        components.tabsUseCases.selectTab(it.id)
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
