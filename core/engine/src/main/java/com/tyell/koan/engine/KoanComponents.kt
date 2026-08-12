package com.tyell.koan.engine

import android.content.Context
import mozilla.components.browser.engine.gecko.GeckoEngine
import mozilla.components.browser.engine.gecko.fetch.GeckoViewFetchClient
import mozilla.components.browser.icons.BrowserIcons
import mozilla.components.browser.session.storage.SessionStorage
import mozilla.components.browser.state.engine.EngineMiddleware
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession.TrackingProtectionPolicy
import mozilla.components.concept.fetch.Client
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

/**
 * Service locator for everything engine-shaped.
 *
 * Everything is `by lazy` because [GeckoRuntime.create] spins up the Gecko
 * parent process, and we don't want that happening during Application.onCreate
 * on a cold start before the first frame.
 */
class KoanComponents(private val context: Context) {

    val runtime: GeckoRuntime by lazy {
        GeckoRuntime.create(
            context,
            GeckoRuntimeSettings.Builder()
                .aboutConfigEnabled(true)
                .consoleOutput(false)
                .debugLogging(false)
                // Glean rides along in the dependency graph via service-nimbus.
                // Never initialised, and telemetry is explicitly off.
                .extensionsProcessEnabled(true)
                .build(),
        )
    }

    val engineSettings: DefaultSettings by lazy {
        DefaultSettings().apply {
            trackingProtectionPolicy = TrackingProtectionPolicy.strict()
            javascriptEnabled = true
            remoteDebuggingEnabled = false
            globalPrivacyControlEnabled = true
            suspendMediaWhenInactive = false
        }
    }

    val engine: Engine by lazy {
        GeckoEngine(context, engineSettings, runtime)
    }

    val client: Client by lazy {
        GeckoViewFetchClient(context, runtime)
    }

    /**
     * The single source of truth for tab state. Zen features read from here
     * rather than poking Gecko, which is what keeps them unit-testable.
     */
    val store: BrowserStore by lazy {
        BrowserStore(middleware = EngineMiddleware.create(engine))
    }

    val sessionStorage: SessionStorage by lazy {
        SessionStorage(context, engine)
    }

    val sessionUseCases: SessionUseCases by lazy { SessionUseCases(store) }

    val tabsUseCases: TabsUseCases by lazy { TabsUseCases(store) }

    val icons: BrowserIcons by lazy { BrowserIcons(context, client) }

    /**
     * Cookie-jar isolation key for a Space. Gecko partitions storage by
     * contextId, so two Spaces can hold separate logins to the same site —
     * the same mechanism desktop Zen uses for container-per-workspace.
     */
    fun contextIdForSpace(spaceId: String): String = "zen-space-$spaceId"
}
