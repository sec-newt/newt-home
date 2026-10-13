package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherTest {

    private val hour = 60L * 60 * 1000
    private val noon = WeatherNow(
        temperature = 82, highLow = 90, isHigh = true, condition = "PARTLY_CLOUDY", isDaytime = true,
        description = "Mostly Sunny", warnings = 0, warningEvent = null, updatedAt = 0L,
    )

    @Test
    fun daytimeShowsTheHigh() {
        assertEquals("82°", WeatherText.temperature(noon))
        assertEquals("↑90°", WeatherText.highLow(noon))
    }

    @Test
    fun eveningShowsTheLow() = assertEquals("↓75°", WeatherText.highLow(noon.copy(highLow = 75, isHigh = false)))

    @Test
    fun unknownTemperature() = assertEquals("--°", WeatherText.temperature(noon.copy(temperature = null)))

    @Test
    fun spokenAsOneSentence() {
        assertEquals("82 degrees, mostly sunny. High 90.", WeatherText.spoken(noon))
        assertEquals(
            "82 degrees, mostly sunny. High 90. Tornado Warning in effect.",
            WeatherText.spoken(noon.copy(warnings = 1, warningEvent = "Tornado Warning")),
        )
    }

    @Test
    fun warnings() {
        assertNull(WeatherText.warning(noon))
        assertEquals("Tornado Warning", WeatherText.warning(noon.copy(warnings = 1, warningEvent = "Tornado Warning")))
        assertEquals("Flood Warning +1", WeatherText.warning(noon.copy(warnings = 2, warningEvent = "Flood Warning")))
    }

    @Test
    fun anOldForecastIsLeftOff() {
        assertTrue(WeatherText.fresh(noon, 11 * hour))
        assertFalse(WeatherText.fresh(noon, 13 * hour))
    }

    @Test
    fun hourlyLabelsAndWords() {
        val h = WeatherHour(start = 14 * hour, temperature = 88, condition = "RAIN", isDaytime = true, precipChance = 40)
        assertEquals("Now", WeatherText.hourLabel(0, { "x" }, h.start))
        assertEquals("2 PM", WeatherText.hourLabel(3, { "2 PM" }, h.start))
        assertEquals("2 PM, 88 degrees, rain, 40 percent chance of rain", WeatherText.hourSpoken(3, h, { "2 PM" }, "rain"))
    }
}
