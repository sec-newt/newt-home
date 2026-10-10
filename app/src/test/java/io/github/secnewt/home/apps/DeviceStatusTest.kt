package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceStatusTest {

    @Test
    fun `the line shows battery and connection as terminal pieces`() {
        assertEquals(
            listOf("bat" to "54%", "wifi" to "●"),
            DeviceStatus(battery = 54, connection = Connection.WIFI).parts(),
        )
        assertEquals(listOf("bat" to "80% charging", "mobile" to "●"), DeviceStatus(80, true, Connection.MOBILE).parts())
        assertEquals(listOf("bat" to "12% low", "net" to "offline"), DeviceStatus(12, false, Connection.OFFLINE).parts())
        assertEquals(emptyList<Pair<String, String>>(), DeviceStatus().parts())
    }

    @Test
    fun `low battery stops counting as low while charging`() {
        assertEquals(true, DeviceStatus(15).batteryLow)
        assertEquals(false, DeviceStatus(15, charging = true).batteryLow)
        assertEquals(false, DeviceStatus(16).batteryLow)
    }

    @Test
    fun `screen readers hear it in words`() {
        assertEquals("Battery 54 percent, on Wi-Fi", DeviceStatus(54, false, Connection.WIFI).spoken())
        assertEquals("Battery 12 percent, low, offline", DeviceStatus(12, false, Connection.OFFLINE).spoken())
    }
}
