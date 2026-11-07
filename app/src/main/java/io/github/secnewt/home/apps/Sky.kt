package io.github.secnewt.home.apps

import androidx.compose.ui.graphics.Color

/**
 * The sky: what the outlines, headings and the newt are colored like right now. The weather
 * wins over the time of day (a storm at noon is a storm), and with no weather it's just the
 * time of day. Only the two accent colors change, never the text colors, and every accent
 * here keeps at least 7:1 contrast on the backgrounds (checked by SkyTest).
 */
enum class SkyTone(
    /** Dark mode: the outline's start and end colors. */
    val darkStart: Long,
    val darkEnd: Long,
    /** Light mode: darker versions of the same two. */
    val lightStart: Long,
    val lightEnd: Long,
) {
    /** The usual Newt blue to purple. */
    NIGHT(0xFF85ABF8, 0xFFBB9AF7, 0xFF1A43A3, 0xFF5E33B0),
    DAWN(0xFFFFB86C, 0xFFFFA9C2, 0xFF7A3D00, 0xFF8C1A4B),
    DAY(0xFF7DCFFF, 0xFF85ABF8, 0xFF004F6E, 0xFF1A43A3),
    DUSK(0xFFBB9AF7, 0xFFFF9E64, 0xFF5E33B0, 0xFF7A3200),
    OVERCAST(0xFFA9B8E0, 0xFF85ABF8, 0xFF3D4A73, 0xFF1A43A3),
    RAIN(0xFF8FB0F5, 0xFFA9B1D6, 0xFF234A8A, 0xFF3D4A73),
    STORM(0xFFFFC777, 0xFFFFA0B4, 0xFF6B4100, 0xFF94102A),
    SNOW(0xFFC9E7FF, 0xFFB4C6F5, 0xFF1A4C6B, 0xFF36448A),
    ;

    fun start(dark: Boolean): Color = Color(if (dark) darkStart else lightStart)
    fun end(dark: Boolean): Color = Color(if (dark) darkEnd else lightEnd)
}

object Sky {
    /** [hour] is 0–23, local time. */
    fun of(hour: Int, weather: WeatherNow?): SkyTone {
        val time = when (hour) {
            in 5..7 -> SkyTone.DAWN
            in 8..16 -> SkyTone.DAY
            in 17..19 -> SkyTone.DUSK
            else -> SkyTone.NIGHT
        }
        if (weather == null) return time
        if (weather.warnings > 0 || weather.condition == "THUNDERSTORM") return SkyTone.STORM
        return when (weather.condition) {
            "RAIN" -> SkyTone.RAIN
            "SNOW" -> SkyTone.SNOW
            // A grey day is grey; a grey night is just night.
            "CLOUDY", "FOG" -> if (time == SkyTone.NIGHT) time else SkyTone.OVERCAST
            else -> time
        }
    }
}
