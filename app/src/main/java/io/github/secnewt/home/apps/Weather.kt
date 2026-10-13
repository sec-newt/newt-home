package io.github.secnewt.home.apps

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import androidx.compose.runtime.Immutable

/** The weather now, as Newt Weather shares it. Temperatures are °F. */
@Immutable
data class WeatherNow(
    val temperature: Int?,
    /** Today's high or tonight's low, whichever NOAA's current forecast period is. */
    val highLow: Int?,
    val isHigh: Boolean,
    /** CLEAR, PARTLY_CLOUDY, CLOUDY, RAIN, THUNDERSTORM, SNOW, FOG or WIND. */
    val condition: String?,
    val isDaytime: Boolean,
    /** NOAA's short words, like "Mostly Sunny". */
    val description: String,
    /** Active Warnings (danger now), not watches or advisories. */
    val warnings: Int,
    /** The first warning's name, like "Tornado Warning". */
    val warningEvent: String?,
    /** When Newt Weather last refreshed, in milliseconds. */
    val updatedAt: Long,
)

/** One hour of the hourly strip. */
@Immutable
data class WeatherHour(
    val start: Long,
    val temperature: Int,
    val condition: String?,
    val isDaytime: Boolean,
    val precipChance: Int?,
)

/**
 * Reads the forecast Newt Weather shares with apps signed with the Newt key. Newt Home has no
 * internet of its own: Newt Weather fetches, and shares only temperatures, conditions and
 * warnings (not your ZIP). Null when Newt Weather isn't installed, hasn't fetched yet, or
 * its forecast is too old to trust.
 */
class Weather(private val context: Context) {

    fun now(currentTime: Long = System.currentTimeMillis()): WeatherNow? {
        val rows = read(SUMMARY) { c ->
            WeatherNow(
                temperature = c.intOrNull("temperature"),
                highLow = c.intOrNull("high_low"),
                isHigh = c.intOrNull("is_high") == 1,
                condition = c.stringOrNull("condition"),
                isDaytime = c.intOrNull("is_daytime") != 0,
                description = c.stringOrNull("description").orEmpty(),
                warnings = c.intOrNull("warnings") ?: 0,
                warningEvent = c.stringOrNull("warning_event"),
                updatedAt = c.longOrNull("updated_at") ?: 0L,
            )
        }
        return rows?.firstOrNull()?.takeIf { WeatherText.fresh(it, currentTime) }
    }

    fun hours(): List<WeatherHour> = read(HOURLY) { c ->
        WeatherHour(
            start = c.longOrNull("start") ?: 0L,
            temperature = c.intOrNull("temperature") ?: 0,
            condition = c.stringOrNull("condition"),
            isDaytime = c.intOrNull("is_daytime") != 0,
            precipChance = c.intOrNull("precip_chance"),
        )
    }.orEmpty()

    private fun <T> read(uri: Uri, row: (Cursor) -> T): List<T>? {
        if (context.checkSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) return null
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                buildList { while (cursor.moveToNext()) add(row(cursor)) }
            }
        } catch (e: RuntimeException) {
            // Newt Weather is missing, too old to share, or its provider failed.
            null
        }
    }

    companion object {
        const val NEWT_WEATHER = "io.github.secnewt.weather"
        const val PERMISSION = "$NEWT_WEATHER.permission.READ_FORECAST"
        // Built on first use, so the plain-Kotlin tests can use the rest without Android.
        val SUMMARY: Uri by lazy { Uri.parse("content://$NEWT_WEATHER.forecast/summary") }
        val HOURLY: Uri by lazy { Uri.parse("content://$NEWT_WEATHER.forecast/hourly") }
    }
}

private fun Cursor.column(name: String): Int? = getColumnIndex(name).takeIf { it >= 0 && !isNull(it) }
private fun Cursor.intOrNull(name: String): Int? = column(name)?.let { getInt(it) }
private fun Cursor.longOrNull(name: String): Long? = column(name)?.let { getLong(it) }
private fun Cursor.stringOrNull(name: String): String? = column(name)?.let { getString(it) }

/** Words for the weather line, the hourly strip and screen readers. Plain Kotlin, easy to test. */
object WeatherText {
    /** A forecast older than this is left off rather than shown wrong. */
    const val MAX_AGE_MS = 12L * 60 * 60 * 1000

    fun fresh(now: WeatherNow, currentTime: Long): Boolean = currentTime - now.updatedAt <= MAX_AGE_MS

    /** "82°" (or "--°" when the station and hour are both unknown). */
    fun temperature(now: WeatherNow): String = now.temperature?.let { "$it°" } ?: "--°"

    /** "↑90°" / "↓75°": the arrow says high or low, so color is never the only signal. */
    fun highLow(now: WeatherNow): String? = now.highLow?.let { if (now.isHigh) "↑$it°" else "↓$it°" }

    /** "82 degrees, mostly sunny. High 90." */
    fun spoken(now: WeatherNow): String = buildString {
        append(now.temperature?.let { "$it degrees" } ?: "Temperature unknown")
        if (now.description.isNotEmpty()) append(", ${now.description.lowercase()}")
        append(".")
        now.highLow?.let { append(if (now.isHigh) " High $it." else " Low $it.") }
        if (now.warnings > 0) append(" ${now.warningEvent ?: "Weather warning"} in effect.")
    }

    /** "Tornado Warning" (or "2 warnings: Tornado Warning +1"). */
    fun warning(now: WeatherNow): String? = when {
        now.warnings <= 0 -> null
        now.warnings == 1 -> now.warningEvent ?: "Weather warning"
        else -> "${now.warningEvent ?: "Weather warning"} +${now.warnings - 1}"
    }

    /** Rain or snow at least this likely counts (NOAA says "likely" from about 60%). */
    const val LIKELY = 50

    /** How far ahead the heads-up looks. */
    const val HEADS_UP_HOURS = 8

    /**
     * A heads-up when rain is likely soon: "Rain now", "Rain likely around 3 PM" or "Storms
     * likely around 4 PM". Null when nothing likely is coming in the next [HEADS_UP_HOURS].
     * Snow counts as "Snow"; the first likely hour decides.
     */
    fun headsUp(hours: List<WeatherHour>, time: (Long) -> String): String? {
        val index = hours.take(HEADS_UP_HOURS).indexOfFirst { (it.precipChance ?: 0) >= LIKELY }
        if (index < 0) return null
        val hour = hours[index]
        val what = when (hour.condition) {
            "THUNDERSTORM" -> "Storms"
            "SNOW" -> "Snow"
            else -> "Rain"
        }
        return if (index == 0) "$what now" else "$what likely around ${time(hour.start)}"
    }

    /** What "Hear the weather" says: the sentence plus the heads-up. */
    fun spokenWithHeadsUp(now: WeatherNow, hours: List<WeatherHour>, time: (Long) -> String): String =
        listOfNotNull(spoken(now), headsUp(hours, time)?.let { "$it." }).joinToString(" ")

    /** "Now" for the current hour, then the hour by itself ("2 PM"). */
    fun hourLabel(index: Int, time: (Long) -> String, start: Long): String = if (index == 0) "Now" else time(start)

    fun hourSpoken(index: Int, hour: WeatherHour, time: (Long) -> String, words: String): String = buildString {
        append(if (index == 0) "Now" else time(hour.start))
        append(", ${hour.temperature} degrees, $words")
        hour.precipChance?.takeIf { it > 0 }?.let { append(", $it percent chance of rain") }
    }
}
