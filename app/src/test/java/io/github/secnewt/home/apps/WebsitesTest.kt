package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebsitesTest {

    @Test
    fun `addresses are tidied and checked`() {
        assertEquals("https://example.org", Websites.normalize(" example.org "))
        assertEquals("https://weather.gov/forecast?x=1", Websites.normalize("weather.gov/forecast?x=1"))
        assertEquals("http://old.example.org", Websites.normalize("http://old.example.org"))
        assertNull(Websites.normalize(""))
        assertNull(Websites.normalize("not a site"))
        assertNull(Websites.normalize("localhost"))
        assertNull(Websites.normalize("javascript:alert(1)"))
        assertNull(Websites.normalize("file:///sdcard/x.html"))
        assertNull(Websites.normalize("intent://x#Intent;end"))
    }

    @Test
    fun `names default to the site`() {
        assertEquals("example.org", Websites.cleanName("  ", "https://www.example.org/a"))
        assertEquals("My bank", Websites.cleanName(" My   bank ", "https://bank.example"))
        assertEquals(Websites.MAX_NAME, Websites.cleanName("x".repeat(99), "https://a.b").length)
    }

    @Test
    fun `saved and read back, skipping anything broken`() {
        val sites = listOf(Website("1", "Weather", "https://weather.gov"), Website("2", "Bank", "https://bank.example/login"))
        assertEquals(sites, Websites.decode(Websites.encode(sites)))
        assertEquals(emptyList<Website>(), Websites.decode("3\tBad\tjavascript:alert(1)\nnonsense"))
    }

    @Test
    fun `a website is listed like an app`() {
        val app = Website("1", "Weather", "https://www.weather.gov").toApp()
        assertEquals("web:1", app.key)
        assertEquals("https://www.weather.gov", app.url)
        assertEquals("weather.gov", Website("1", "Weather", "https://www.weather.gov").host)
    }
}
