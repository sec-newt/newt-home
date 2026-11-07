package io.github.secnewt.home.apps

import java.util.Calendar

/**
 * What the newt by the clock is up to. Each mood means something, and its [caption] always
 * says it in words, so the newt is never the only way to know. The most important wins:
 * a weather warning, then the battery, then the weather, then the day.
 */
enum class NewtMood(val caption: String) {
    STORM("storm warning"),
    LOW_BATTERY("battery low"),
    CHARGING("charging"),
    RAIN("rain"),
    RAIN_SOON("rain soon"),
    SNOW("snow"),
    LORDS_DAY("Lord's day"),
    SLEEPING("sleeping"),
    SUNNY("sunny"),
    /** Nothing to say: just the newt, no badge or caption. */
    PLAIN(""),
    ;

    companion object {
        /** [hour] 0–23 and [dayOfWeek] as [Calendar.DAY_OF_WEEK], local time. */
        fun of(
            hour: Int,
            dayOfWeek: Int,
            weather: WeatherNow?,
            status: DeviceStatus,
            rainSoon: Boolean,
        ): NewtMood = when {
            weather != null && (weather.warnings > 0 || weather.condition == "THUNDERSTORM") -> STORM
            status.batteryLow -> LOW_BATTERY
            status.charging -> CHARGING
            weather?.condition == "RAIN" -> RAIN
            rainSoon -> RAIN_SOON
            weather?.condition == "SNOW" -> SNOW
            dayOfWeek == Calendar.SUNDAY && hour in 6..12 -> LORDS_DAY
            hour >= 22 || hour < 6 -> SLEEPING
            weather?.condition == "CLEAR" && weather.isDaytime -> SUNNY
            else -> PLAIN
        }
    }
}
