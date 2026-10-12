package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemInfoTest {

    @Test
    fun `uptime reads like neofetch`() {
        assertEquals("20d 11h", SystemInfoText.uptime(((20 * 24 + 11) * 60 + 5) * 60_000L))
        assertEquals("3h 5m", SystemInfoText.uptime((3 * 60 + 5) * 60_000L))
        assertEquals("12m", SystemInfoText.uptime(12 * 60_000L))
        assertEquals("0m", SystemInfoText.uptime(-5))
    }

    @Test
    fun `usage shows sizes and a percent`() {
        assertEquals("157G / 228G (69%)", SystemInfoText.usage(157_000_000_000, 228_000_000_000))
        assertEquals("512M / 2G (25%)", SystemInfoText.usage(512_000_000, 2_048_000_000))
        assertEquals("unknown", SystemInfoText.usage(5, 0))
    }

    @Test
    fun `battery is left out when unknown`() {
        val info = SystemInfo("newt", "Pixel 9", "Android 16", "BP2A", "6.1", 0, 120, 1, 2, 1, 2, null, "Newt Home 0.1")
        assertTrue(info.rows().none { it.first == "battery" })
        assertEquals("os", info.rows().first().first)
    }
}
