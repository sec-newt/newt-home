package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NewtMoodTest {

    private val clear = WeatherNow(
        temperature = 82, highLow = 90, isHigh = true, condition = "CLEAR", isDaytime = true,
        description = "Sunny", warnings = 0, warningEvent = null, updatedAt = 0L,
    )
    private val fine = DeviceStatus(battery = 80)

    private fun mood(
        hour: Int = 13,
        day: Int = Calendar.FRIDAY,
        weather: WeatherNow? = clear,
        status: DeviceStatus = fine,
        rainSoon: Boolean = false,
    ) = NewtMood.of(hour, day, weather, status, rainSoon)

    @Test
    fun `a clear afternoon is sunny`() = assertEquals(NewtMood.SUNNY, mood())

    @Test
    fun `nothing to say is just the newt`() {
        assertEquals(NewtMood.PLAIN, mood(weather = null))
        assertEquals(NewtMood.PLAIN, mood(weather = clear.copy(condition = "CLOUDY")))
    }

    @Test
    fun `a warning beats everything, then the battery, then the weather`() {
        val low = DeviceStatus(battery = 10)
        assertEquals(NewtMood.STORM, mood(weather = clear.copy(warnings = 1), status = low))
        assertEquals(NewtMood.LOW_BATTERY, mood(weather = clear.copy(condition = "RAIN"), status = low))
        assertEquals(NewtMood.CHARGING, mood(status = DeviceStatus(battery = 10, charging = true)))
        assertEquals(NewtMood.RAIN, mood(weather = clear.copy(condition = "RAIN"), rainSoon = true))
        assertEquals(NewtMood.RAIN_SOON, mood(rainSoon = true))
        assertEquals(NewtMood.SNOW, mood(weather = clear.copy(condition = "SNOW")))
    }

    @Test
    fun `Sunday morning, then nights`() {
        assertEquals(NewtMood.LORDS_DAY, mood(hour = 9, day = Calendar.SUNDAY))
        assertEquals(NewtMood.SUNNY, mood(hour = 14, day = Calendar.SUNDAY))
        assertEquals(NewtMood.SLEEPING, mood(hour = 23))
        assertEquals(NewtMood.SLEEPING, mood(hour = 3, weather = null))
    }

    @Test
    fun `every mood but plain says what it means in words`() {
        NewtMood.entries.filter { it != NewtMood.PLAIN }.forEach { assertTrue(it.name, it.caption.isNotBlank()) }
    }
}
