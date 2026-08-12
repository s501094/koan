package com.tyell.koan

import com.tyell.koan.engine.UrlInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlInputTest {

    @Test
    fun `bare host gets https`() {
        assertEquals("https://example.com", UrlInput.toUrl("example.com"))
        assertEquals("https://sub.example.co.uk/path?a=1", UrlInput.toUrl("sub.example.co.uk/path?a=1"))
    }

    @Test
    fun `explicit scheme is left alone`() {
        assertEquals("http://example.com", UrlInput.toUrl("http://example.com"))
        assertEquals("about:config", UrlInput.toUrl("about:config"))
    }

    @Test
    fun `localhost stays on http`() {
        assertEquals("http://localhost:8080", UrlInput.toUrl("localhost:8080"))
    }

    @Test
    fun `prose becomes a search`() {
        val out = UrlInput.toUrl("how do gecko contextIds work")
        assertTrue(out.startsWith("https://duckduckgo.com/?q="))
        assertTrue(out.contains("gecko%20contextIds"))
    }

    @Test
    fun `dotted phrase with spaces is still a search`() {
        assertTrue(UrlInput.isSearch("look at example.com today"))
    }

    @Test
    fun `prettify strips scheme and www`() {
        assertEquals("example.com/x", UrlInput.prettify("https://www.example.com/x"))
    }
}
