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

    private fun hourAt(i: Int, chance: Int, condition: String = "RAIN") =
        WeatherHour(start = (13 + i) * hour, temperature = 80, condition = condition, isDaytime = true, precipChance = chance)
    private val clock: (Long) -> String = { "${(it / hour - 1) % 12 + 1} PM" }

    @Test
    fun headsUpWhenRainIsLikelySoon() {
        val hours = listOf(hourAt(0, 10), hourAt(1, 20), hourAt(2, 60), hourAt(3, 70))
        assertEquals("Rain likely around 3 PM", WeatherText.headsUp(hours, clock))
        assertEquals("Storms likely around 3 PM", WeatherText.headsUp(hours.map { it.copy(condition = "THUNDERSTORM") }, clock))
    }

    @Test
    fun headsUpWhenItsRainingNow() = assertEquals("Rain now", WeatherText.headsUp(listOf(hourAt(0, 80)), clock))

    @Test
    fun noHeadsUpForAChanceOrLaterRain() {
        assertNull(WeatherText.headsUp(listOf(hourAt(0, 30), hourAt(1, 40)), clock))
        val late = List(8) { hourAt(it, 0) } + hourAt(8, 90)
        assertNull(WeatherText.headsUp(late, clock)) // more than 8 hours away
    }

    @Test
    fun hearTheWeatherIncludesTheHeadsUp() = assertEquals(
        "82 degrees, mostly sunny. High 90. Rain likely around 3 PM.",
        WeatherText.spokenWithHeadsUp(noon, listOf(hourAt(0, 0), hourAt(1, 0), hourAt(2, 60)), clock),
    )

    @Test
    fun hourlyLabelsAndWords() {
        val h = WeatherHour(start = 14 * hour, temperature = 88, condition = "RAIN", isDaytime = true, precipChance = 40)
        assertEquals("Now", WeatherText.hourLabel(0, { "x" }, h.start))
        assertEquals("2 PM", WeatherText.hourLabel(3, { "2 PM" }, h.start))
        assertEquals("2 PM, 88 degrees, rain, 40 percent chance of rain", WeatherText.hourSpoken(3, h, { "2 PM" }, "rain"))
    }
}
