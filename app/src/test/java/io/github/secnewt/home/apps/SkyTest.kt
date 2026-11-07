package io.github.secnewt.home.apps

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import io.github.secnewt.home.ui.SKY_GLOW_ALPHA
import io.github.secnewt.ui.NewtDay
import io.github.secnewt.ui.NewtNight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyTest {

    private val clear = WeatherNow(
        temperature = 82, highLow = 90, isHigh = true, condition = "CLEAR", isDaytime = true,
        description = "Sunny", warnings = 0, warningEvent = null, updatedAt = 0L,
    )

    @Test
    fun `with no weather it follows the time of day`() {
        assertEquals(SkyTone.DAWN, Sky.of(6, null))
        assertEquals(SkyTone.DAY, Sky.of(12, null))
        assertEquals(SkyTone.DUSK, Sky.of(18, null))
        assertEquals(SkyTone.NIGHT, Sky.of(23, null))
        assertEquals(SkyTone.NIGHT, Sky.of(2, null))
    }

    @Test
    fun `the weather wins over the time of day`() {
        assertEquals(SkyTone.DAY, Sky.of(12, clear))
        assertEquals(SkyTone.RAIN, Sky.of(12, clear.copy(condition = "RAIN")))
        assertEquals(SkyTone.SNOW, Sky.of(23, clear.copy(condition = "SNOW")))
        assertEquals(SkyTone.STORM, Sky.of(18, clear.copy(condition = "THUNDERSTORM")))
        assertEquals(SkyTone.STORM, Sky.of(12, clear.copy(warnings = 1)))
    }

    @Test
    fun `a grey day is overcast but a grey night is just night`() {
        assertEquals(SkyTone.OVERCAST, Sky.of(12, clear.copy(condition = "CLOUDY")))
        assertEquals(SkyTone.NIGHT, Sky.of(23, clear.copy(condition = "FOG")))
    }

    @Test
    fun `night is the usual Newt blue and purple`() {
        assertEquals(NewtNight.primary, SkyTone.NIGHT.start(dark = true))
        assertEquals(NewtNight.secondary, SkyTone.NIGHT.end(dark = true))
        assertEquals(NewtDay.primary, SkyTone.NIGHT.start(dark = false))
        assertEquals(NewtDay.secondary, SkyTone.NIGHT.end(dark = false))
    }

    @Test
    fun `every sky color keeps 7 to 1 on the backgrounds text sits on`() {
        for (tone in SkyTone.entries) {
            for ((dark, scheme) in listOf(true to NewtNight, false to NewtDay)) {
                for (color in listOf(tone.start(dark), tone.end(dark))) {
                    for (bg in listOf(scheme.background, scheme.surfaceContainerLow)) {
                        val ratio = contrast(color, bg)
                        assertTrue("$tone dark=$dark: ${"%.2f".format(ratio)}:1", ratio >= 7f)
                    }
                }
            }
        }
    }

    @Test
    fun `the glow behind the clock keeps the clock and date at 7 to 1`() {
        for (tone in SkyTone.entries) {
            for ((dark, scheme) in listOf(true to NewtNight, false to NewtDay)) {
                val glow = tone.start(dark).copy(alpha = SKY_GLOW_ALPHA).compositeOver(scheme.background)
                for (text in listOf(scheme.onBackground, scheme.onSurfaceVariant)) {
                    val ratio = contrast(text, glow)
                    assertTrue("$tone dark=$dark: ${"%.2f".format(ratio)}:1", ratio >= 7f)
                }
            }
        }
    }

    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }
}
