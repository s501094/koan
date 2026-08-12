package com.tyell.koan.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoostEntityTest {

    private fun boost(zap: String = "", css: String = "", js: String = "") =
        BoostEntity(id = "1", pattern = "example.com", css = css, js = js, zapSelectors = zap)

    @Test
    fun `pattern comes from the host, without www`() {
        assertEquals("example.com", BoostEntity.patternFor("https://www.example.com/a/b?c=1"))
        assertEquals("news.example.com", BoostEntity.patternFor("https://news.example.com/"))
    }

    @Test
    fun `urls without a host have no pattern`() {
        assertNull(BoostEntity.patternFor("about:blank"))
        assertNull(BoostEntity.patternFor("not a url"))
    }

    @Test
    fun `zap selectors round trip through the joined column`() {
        val b = boost().withZap(listOf("#ad", ".promo > div"))
        assertEquals(listOf("#ad", ".promo > div"), b.zapList)
    }

    @Test
    fun `blank lines in the zap column are ignored`() {
        assertEquals(listOf("#a", "#b"), boost(zap = "#a\n\n  \n#b\n").zapList)
    }

    @Test
    fun `a boost with nothing in it is empty`() {
        assertTrue(boost().isEmpty)
        assertFalse(boost(zap = "#ad").isEmpty)
        assertFalse(boost(css = "body{}").isEmpty)
        assertFalse(boost(js = "void 0").isEmpty)
    }
}
