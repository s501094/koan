package com.tyell.koan.engine

import org.mozilla.geckoview.GeckoPreferenceController

/**
 * Turns off everything in Gecko that talks to anyone other than the site you
 * are looking at.
 *
 * A stock Gecko is not a tracker, but it is chatty: it fetches Safe Browsing
 * lists from Google, polls Mozilla's Remote Settings and Normandy, probes a
 * captive-portal endpoint, looks up your region for search defaults, and has a
 * telemetry stack wired up even when nothing is uploading. None of that is
 * needed to render a web page, so all of it is switched off here explicitly
 * rather than left to defaults that could change with an engine bump.
 *
 * These are set on the *default* branch, so `about:config` still shows them as
 * unmodified and you can override any of them by hand. `aboutConfigEnabled` is
 * deliberately on so that is possible.
 *
 * What is deliberately NOT disabled, and why:
 *  - OCSP / certificate revocation. It contacts the CA of the site you are
 *    already visiting, which is not a third party learning anything new, and
 *    turning it off weakens a real protection.
 *  - DNS-over-HTTPS is left off, not on. Enabling it would route every lookup
 *    through one resolver that would then see your whole browsing history.
 *    System DNS spreads that across whatever your network already uses.
 *  - Favicon fetching. That is a request to the site you are on.
 */
object PrivacyHardening {

    private const val BRANCH = GeckoPreferenceController.PREF_BRANCH_DEFAULT

    private val BOOL_PREFS = mapOf(
        // --- Telemetry ---------------------------------------------------
        "datareporting.healthreport.uploadEnabled" to false,
        "datareporting.policy.dataSubmissionEnabled" to false,
        "toolkit.telemetry.enabled" to false,
        "toolkit.telemetry.unified" to false,
        "toolkit.telemetry.archive.enabled" to false,
        "toolkit.telemetry.newProfilePing.enabled" to false,
        "toolkit.telemetry.updatePing.enabled" to false,
        "toolkit.telemetry.bhrPing.enabled" to false,
        "toolkit.telemetry.firstShutdownPing.enabled" to false,
        "toolkit.telemetry.shutdownPingSender.enabled" to false,
        "toolkit.coverage.enabled" to false,
        "toolkit.coverage.opt-out" to true,
        "browser.ping-centre.telemetry" to false,
        "browser.newtabpage.activity-stream.telemetry" to false,
        "browser.newtabpage.activity-stream.feeds.telemetry" to false,

        // --- Experiments and studies --------------------------------------
        "app.shield.optoutstudies.enabled" to false,
        "app.normandy.enabled" to false,

        // --- Background service chatter -----------------------------------
        "network.captive-portal-service.enabled" to false,
        "network.connectivity-service.enabled" to false,
        "browser.region.update.enabled" to false,
        "dom.push.enabled" to false,
        "extensions.blocklist.enabled" to false,
        "extensions.getAddons.cache.enabled" to false,
        "extensions.systemAddon.update.enabled" to false,
        "extensions.update.enabled" to false,
        "app.update.enabled" to false,

        // --- Safe Browsing ------------------------------------------------
        // Firefox's implementation downloads hash lists from Google rather than
        // sending it every URL, so it is far better than it sounds — but it is
        // still a periodic connection to Google that browsing does not require.
        // Off by default here. Turning these four back on restores phishing and
        // malware warnings; that is a real protection to give up knowingly.
        "browser.safebrowsing.malware.enabled" to false,
        "browser.safebrowsing.phishing.enabled" to false,
        "browser.safebrowsing.downloads.enabled" to false,
        "browser.safebrowsing.downloads.remote.block_potentially_unwanted" to false,

        // --- Speculative connections --------------------------------------
        // Each of these contacts hosts you have not chosen to visit yet.
        "network.dns.disablePrefetch" to true,
        "network.prefetch-next" to false,
        "network.predictor.enabled" to false,

        // --- Page-driven beacons ------------------------------------------
        "browser.send_pings" to false,
        "beacon.enabled" to false,
        "privacy.donottrackheader.enabled" to true,

        // WebRTC hands out local interface addresses during ICE, which is a
        // known de-anonymisation route. Restrict it to the default route.
        "media.peerconnection.ice.default_address_only" to true,

        // --- Endpoints found compiled into the shipped native libraries ----
        // Auditing the APK turned up hard-coded hosts inside Mozilla's Rust
        // megazord and crash tools: incoming.telemetry.mozilla.org,
        // firefox.settings.services.mozilla.com, merino.services.mozilla.com,
        // identity.mozilla.com and ads.mozilla.org. Nothing in this app
        // initialises the components that use them, but "we never call it" is a
        // weaker guarantee than "it is also switched off", so both hold.
        "browser.crashReports.unsubmittedCheck.enabled" to false,
        "browser.crashReports.unsubmittedCheck.autoSubmit2" to false,
        "browser.tabs.crashReporting.sendReport" to false,
        "identity.fxaccounts.enabled" to false,
        "browser.urlbar.quicksuggest.enabled" to false,
        "browser.urlbar.suggest.quicksuggest.sponsored" to false,
        "browser.urlbar.suggest.quicksuggest.nonsponsored" to false,
        "browser.newtabpage.activity-stream.showSponsored" to false,
        "browser.newtabpage.activity-stream.showSponsoredTopSites" to false,
        "browser.newtabpage.activity-stream.feeds.system.topstories" to false,
        "browser.discovery.enabled" to false,
    )

    private val STRING_PREFS = mapOf(
        // Blanking the endpoints means that even if something flips a boolean
        // back on, it has nowhere to send anything.
        "toolkit.telemetry.server" to "",
        "app.normandy.api_url" to "",
        "services.settings.server" to "",
        "captivedetect.canonicalURL" to "",
        "network.connectivity-service.IPv4.url" to "",
        "network.connectivity-service.IPv6.url" to "",
        "browser.safebrowsing.provider.google.updateURL" to "",
        "browser.safebrowsing.provider.google.gethashURL" to "",
        "browser.safebrowsing.provider.google4.updateURL" to "",
        "browser.safebrowsing.provider.google4.gethashURL" to "",
        "browser.safebrowsing.provider.mozilla.updateURL" to "",
        "browser.safebrowsing.provider.mozilla.gethashURL" to "",
        "browser.region.network.url" to "",
        "browser.search.geoip.url" to "",
        "dom.push.serverURL" to "",
        "breakpad.reportURL" to "",
        "browser.urlbar.merino.endpointURL" to "",
        "identity.fxaccounts.auth.uri" to "",
        "browser.newtabpage.activity-stream.discoverystream.endpoints" to "",
    )

    private val INT_PREFS = mapOf(
        "network.http.speculative-parallel-limit" to 0,
        // 0 = never send the Referer header cross-origin... 2 = send on same
        // origin only for cross-site. Keep Firefox's trimming behaviour but
        // never leak the full path across sites.
        "network.http.referer.XOriginTrimmingPolicy" to 2,
    )

    /**
     * Applies the lot. Fire-and-forget: a pref that a future Gecko has renamed
     * simply fails on its own and takes nothing else down with it.
     */
    fun apply() {
        BOOL_PREFS.forEach { (name, value) ->
            runCatching { GeckoPreferenceController.setGeckoPref(name, value, BRANCH) }
        }
        STRING_PREFS.forEach { (name, value) ->
            runCatching { GeckoPreferenceController.setGeckoPref(name, value, BRANCH) }
        }
        INT_PREFS.forEach { (name, value) ->
            runCatching { GeckoPreferenceController.setGeckoPref(name, value, BRANCH) }
        }
    }

    /** Everything this touches, for the audit doc and for tests. */
    val allPrefNames: List<String>
        get() = BOOL_PREFS.keys.toList() + STRING_PREFS.keys.toList() + INT_PREFS.keys.toList()
}
