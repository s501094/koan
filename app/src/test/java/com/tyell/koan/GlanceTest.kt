package com.tyell.koan

import mozilla.components.concept.engine.HitResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlanceTest {

    @Test
    fun `a plain link is glanceable`() {
        val hit = HitResult.UNKNOWN("https://example.com/article")
        assertEquals("https://example.com/article", Glance.linkFrom(hit))
    }

    @Test
    fun `a linked image glances the link, not the image`() {
        val hit = HitResult.IMAGE_SRC(
            "https://cdn.example.com/thumb.png",
            "https://example.com/article",
        )
        assertEquals("https://example.com/article", Glance.linkFrom(hit))
    }

    @Test
    fun `a bare image is not a link`() {
        assertNull(Glance.linkFrom(HitResult.IMAGE("https://cdn.example.com/photo.jpg")))
    }

    @Test
    fun `mailto and tel are somebody else's job`() {
        assertNull(Glance.linkFrom(HitResult.EMAIL("mailto:me@example.com")))
        assertNull(Glance.linkFrom(HitResult.PHONE("tel:+15550000")))
    }

    @Test
    fun `a long press on nothing produces nothing`() {
        assertNull(Glance.linkFrom(null))
        // Long-pressing text gives an UNKNOWN with no href.
        assertNull(Glance.linkFrom(HitResult.UNKNOWN("")))
    }

    @Test
    fun `non-http schemes are refused`() {
        assertFalse(Glance.isGlanceable("javascript:alert(1)"))
        assertFalse(Glance.isGlanceable("file:///etc/passwd"))
        assertFalse(Glance.isGlanceable("data:text/html,hi"))
        assertFalse(Glance.isGlanceable("intent://scan/#Intent;scheme=zxing;end"))
    }

    @Test
    fun `an anchor on the current page is not worth peeking`() {
        assertFalse(Glance.isGlanceable("#section-2"))
    }

    @Test
    fun `http and https both pass`() {
        assertTrue(Glance.isGlanceable("http://example.com"))
        assertTrue(Glance.isGlanceable("https://example.com"))
    }

    @Test
    fun `animation duration matches Zen's pref`() {
        // zen.glance.animation-duration = 350
        assertEquals(350, Glance.ANIMATION_MS)
    }
}
