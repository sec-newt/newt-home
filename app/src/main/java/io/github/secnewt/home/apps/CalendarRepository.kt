package io.github.secnewt.home.apps

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract.Instances
import java.util.Calendar
import java.util.TimeZone

/**
 * Reads today's events from the phone's own calendar (every calendar set to show, including
 * ones synced by apps like DAVx⁵ or Etar's local calendars). Nothing leaves the phone, and
 * it only runs once calendar access is turned on.
 */
class CalendarRepository(private val context: Context) {

    fun today(now: Long = System.currentTimeMillis()): List<DayEvent> {
        val start = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = start.timeInMillis
        val dayEnd = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        // All-day events are stored at midnight UTC, so look a day either side and keep only
        // the ones whose UTC date is today's local date.
        val uri = Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, dayStart - DAY)
            ContentUris.appendId(it, dayEnd + DAY)
        }.build()
        val projection = arrayOf(
            Instances.EVENT_ID,
            Instances.TITLE,
            Instances.BEGIN,
            Instances.END,
            Instances.ALL_DAY,
            Instances.EVENT_LOCATION,
        )
        val today = localDate(now, TimeZone.getDefault())
        val events = mutableListOf<DayEvent>()
        context.contentResolver.query(uri, projection, "${Instances.VISIBLE} = 1", null, "${Instances.BEGIN} ASC")?.use { c ->
            while (c.moveToNext()) {
                val begin = c.getLong(2)
                val end = c.getLong(3)
                val allDay = c.getInt(4) == 1
                val isToday = if (allDay) {
                    localDate(begin, UTC) <= today && today < localDate(end, UTC).coerceAtLeast(localDate(begin, UTC) + 1)
                } else {
                    begin < dayEnd && end > dayStart
                }
                if (!isToday) continue
                events += DayEvent(
                    id = c.getLong(0),
                    title = AgendaText.titleOf(c.getString(1)),
                    begin = begin,
                    end = end,
                    allDay = allDay,
                    location = c.getString(5)?.trim().orEmpty(),
                )
            }
        }
        // All-day first, then by start time.
        return events.sortedWith(compareByDescending<DayEvent> { it.allDay }.thenBy { it.begin })
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
        val UTC: TimeZone = TimeZone.getTimeZone("UTC")

        /** A date as a single number (year, month, day), in [zone]. */
        fun localDate(time: Long, zone: TimeZone): Int {
            val c = Calendar.getInstance(zone).apply { timeInMillis = time }
            return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
        }
    }
}
