package com.example

import com.example.util.UrlAndFaviconUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun normalizeUrl_addsHttpsAndValidatesDomains() {
        assertEquals("https://google.com", UrlAndFaviconUtils.normalizeUrl("google.com"))
        assertEquals(
            "https://github.com/android",
            UrlAndFaviconUtils.normalizeUrl("  HTTPS://GitHub.com/android  ")
        )
        assertEquals("http://example.org/docs", UrlAndFaviconUtils.normalizeUrl("http://example.org/docs"))
        assertNull(UrlAndFaviconUtils.normalizeUrl(""))
        assertNull(UrlAndFaviconUtils.normalizeUrl("not a url"))
        assertNull(UrlAndFaviconUtils.normalizeUrl("javascript:alert(1)"))
    }

    @Test
    fun extractDomainAndMonogram_returnsCleanResults() {
        assertEquals("github.com", UrlAndFaviconUtils.extractDomain("https://www.github.com/explore"))
        assertEquals("G", UrlAndFaviconUtils.extractMonogram("GitHub", "https://github.com"))
        assertEquals("W", UrlAndFaviconUtils.extractMonogram("", "https://wikipedia.org"))
    }
}
