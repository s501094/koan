package com.tyell.koan.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the shape of the hardening list rather than its effect — the effect
 * needs a live Gecko. If someone deletes a category wholesale, this notices.
 */
class PrivacyHardeningTest {

    private val prefs = PrivacyHardening.allPrefNames

    @Test
    fun `telemetry endpoints and switches are covered`() {
        assertTrue("toolkit.telemetry.enabled" in prefs)
        assertTrue("toolkit.telemetry.server" in prefs)
        assertTrue("datareporting.healthreport.uploadEnabled" in prefs)
    }

    @Test
    fun `google safe browsing endpoints are covered`() {
        assertTrue(prefs.any { it.contains("safebrowsing.provider.google") })
        assertTrue("browser.safebrowsing.malware.enabled" in prefs)
        assertTrue("browser.safebrowsing.phishing.enabled" in prefs)
    }

    @Test
    fun `mozilla background services are covered`() {
        assertTrue("app.normandy.enabled" in prefs)
        assertTrue("services.settings.server" in prefs)
        assertTrue("network.captive-portal-service.enabled" in prefs)
        assertTrue("browser.region.network.url" in prefs)
    }

    @Test
    fun `speculative connections and page beacons are covered`() {
        assertTrue("network.predictor.enabled" in prefs)
        assertTrue("network.prefetch-next" in prefs)
        assertTrue("browser.send_pings" in prefs)
        assertTrue("beacon.enabled" in prefs)
    }

    @Test
    fun `every endpoint found in the shipped native libs is addressed`() {
        // These hosts are compiled into libmegazord.so / libcrashtools.so.
        // Each needs a corresponding off-switch or blanked endpoint here.
        assertTrue("breakpad.reportURL" in prefs)                  // incoming.telemetry
        assertTrue("services.settings.server" in prefs)            // firefox.settings.services
        assertTrue("browser.urlbar.merino.endpointURL" in prefs)   // merino.services
        assertTrue("identity.fxaccounts.enabled" in prefs)         // identity.mozilla.com
        assertTrue("browser.newtabpage.activity-stream.showSponsored" in prefs) // ads.mozilla.org
    }

    @Test
    fun `no pref is listed twice`() {
        assertEquals(prefs.size, prefs.toSet().size)
    }
}
