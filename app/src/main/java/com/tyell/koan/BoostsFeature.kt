package com.tyell.koan

import android.util.Log
import com.tyell.koan.data.BoostEntity
import com.tyell.koan.data.KoanDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.webextension.MessageHandler
import mozilla.components.concept.engine.webextension.Port
import mozilla.components.concept.engine.webextension.WebExtension
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Boosts — per-site CSS, script and zapped elements.
 *
 * The rules live in the app; the extension is a dumb applier. Everything moves
 * over a native port between this class and the bundled extension's background
 * page. Nothing here touches the network, and the extension has no host beyond
 * the pages it styles.
 */
class BoostsFeature(
    private val engine: Engine,
    private val db: KoanDatabase,
    private val scope: CoroutineScope,
) {
    private var extension: WebExtension? = null
    private var port: Port? = null

    private val _picked = MutableSharedFlow<PickedElement>(extraBufferCapacity = 4)

    /** Emitted when the user taps an element while the picker is armed. */
    val picked: Flow<PickedElement> = _picked.asSharedFlow()

    data class PickedElement(val selector: String, val url: String)

    fun install() {
        engine.installBuiltInWebExtension(
            id = EXTENSION_ID,
            url = EXTENSION_URL,
            onSuccess = { ext ->
                extension = ext
                ext.registerBackgroundMessageHandler(PORT_NAME, handler)
            },
            onError = { error ->
                Log.w(TAG, "Boosts extension failed to install", error)
            },
        )

        // Any change to the table is pushed straight to the extension, so a
        // toggle or a new zap takes effect without a reload.
        scope.launch {
            db.boosts().observeAll().collect { pushRules(it) }
        }
    }

    private val handler = object : MessageHandler {
        override fun onPortConnected(port: Port) {
            this@BoostsFeature.port = port
            // The extension restarts independently of us, so send the current
            // table on every connect rather than relying on the observer having
            // fired at the right moment.
            scope.launch { pushRules(db.boosts().getAll()) }
        }

        override fun onPortDisconnected(port: Port) {
            if (this@BoostsFeature.port === port) this@BoostsFeature.port = null
        }

        override fun onPortMessage(message: Any, port: Port) {
            val json = message as? JSONObject ?: return
            if (json.optString("type") != "picked") return
            val selector = json.optString("selector").takeIf { it.isNotBlank() } ?: return
            val url = json.optString("url")
            scope.launch { _picked.emit(PickedElement(selector, url)) }
        }
    }

    private fun pushRules(boosts: List<BoostEntity>) {
        val port = port ?: return
        val array = JSONArray()
        boosts.forEach { boost ->
            array.put(
                JSONObject().apply {
                    put("pattern", boost.pattern)
                    put("css", boost.css)
                    put("js", boost.js)
                    put("enabled", boost.enabled)
                    put("zap", JSONArray(boost.zapList))
                },
            )
        }
        port.postMessage(JSONObject().apply {
            put("type", "rules")
            put("rules", array)
        })
    }

    fun startPicker() {
        port?.postMessage(JSONObject().apply { put("type", "startPicker") })
    }

    fun stopPicker() {
        port?.postMessage(JSONObject().apply { put("type", "stopPicker") })
    }

    /** Adds a zapped selector to the site's Boost, creating one if needed. */
    suspend fun zap(url: String, selector: String) {
        val pattern = BoostEntity.patternFor(url) ?: return
        val existing = db.boosts().forPattern(pattern)
        val boost = existing ?: BoostEntity(id = UUID.randomUUID().toString(), pattern = pattern)
        if (selector in boost.zapList) return
        db.boosts().upsert(boost.withZap(boost.zapList + selector))
    }

    suspend fun removeZap(boost: BoostEntity, selector: String) {
        val remaining = boost.zapList - selector
        val updated = boost.withZap(remaining)
        if (updated.isEmpty) db.boosts().delete(boost) else db.boosts().upsert(updated)
    }

    suspend fun setEnabled(boost: BoostEntity, enabled: Boolean) {
        db.boosts().upsert(boost.copy(enabled = enabled))
    }

    /** Clearing the last thing in a Boost removes the row rather than leaving an empty one. */
    suspend fun saveCss(url: String, css: String) {
        val pattern = BoostEntity.patternFor(url) ?: return
        val existing = db.boosts().forPattern(pattern)
        val updated = (existing ?: BoostEntity(UUID.randomUUID().toString(), pattern))
            .copy(css = css)

        when {
            !updated.isEmpty -> db.boosts().upsert(updated)
            existing != null -> db.boosts().delete(existing)
        }
    }

    suspend fun clear(boost: BoostEntity) = db.boosts().delete(boost)

    /** Unused today; kept so the picker can be scoped to one session later. */
    fun connectedPort(session: EngineSession): Port? =
        extension?.getConnectedPort(PORT_NAME, session)

    companion object {
        private const val TAG = "Boosts"
        const val EXTENSION_ID = "boosts@koan.tyell"

        /** Bundled in the APK. Not fetched, not updatable, not remote. */
        const val EXTENSION_URL = "resource://android/assets/extensions/boosts/"
        const val PORT_NAME = "koanBoosts"
    }
}
