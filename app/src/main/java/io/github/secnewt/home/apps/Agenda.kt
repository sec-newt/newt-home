package io.github.secnewt.home.apps

import androidx.compose.runtime.Immutable

/** One of today's events, as read from the phone's calendar. Times are milliseconds. */
@Immutable
data class DayEvent(
    val id: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean = false,
    val location: String = "",
    /** The color of its calendar (or of the event itself, if it has its own), as ARGB. */
    val color: Int? = null,
)

/** What the line under the date shows. */
sealed interface Agenda {
    /** Turned off ("No thanks" or "Stop showing events"): nothing under the date. */
    data object Off : Agenda

    /** Not decided yet: offer to show today's events. */
    data object Offer : Agenda

    /** Turned on, but Android's calendar permission is off. */
    data object NeedsPermission : Agenda

    /** Today's events: all-day ones first, then by start time. */
    data class Today(val events: List<DayEvent>) : Agenda
}

/** The words for today's events, kept apart from Android so they can be tested. */
object AgendaText {

    /** Events still to come or happening now; all-day events count all day. */
    fun upcoming(events: List<DayEvent>, now: Long): List<DayEvent> =
        events.filter { it.allDay || it.end > now }

    /** The event the line is about: the next timed one, or else an all-day one. */
    fun next(events: List<DayEvent>, now: Long): DayEvent? =
        events.firstOrNull { !it.allDay && it.end > now } ?: events.firstOrNull { it.allDay }

    fun isNow(event: DayEvent, now: Long): Boolean = !event.allDay && event.begin <= now && now < event.end

    /**
     * The line under the date, like "next: 3:00 Dentist · 2 more today". [time] formats a
     * time the way the phone does (12 or 24 hour).
     */
    fun line(events: List<DayEvent>, now: Long, time: (Long) -> String): String {
        if (events.isEmpty()) return "no events today"
        val upcoming = upcoming(events, now)
        val next = next(upcoming, now) ?: return "nothing else today"
        val first = when {
            next.allDay -> "today: ${next.title}"
            isNow(next, now) -> "now: ${next.title} until ${time(next.end)}"
            else -> listOfNotNull("next: ${time(next.begin)} ${next.title}", countdown(next.begin, now)).joinToString(" · ")
        }
        val more = upcoming.size - 1
        return if (more > 0) "$first · $more more today" else first
    }

    /**
     * How long until [startMillis]: "in 45 min", "in 3 h", "in 1 h 15 min" (a part minute counts
     * as a minute). Null once it has started, so nothing ever says "in 0 min".
     */
    fun countdown(startMillis: Long, nowMillis: Long): String? {
        if (startMillis <= nowMillis) return null
        val minutes = (startMillis - nowMillis + 59_999) / 60_000
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0L -> "in $m min"
            m == 0L -> "in $h h"
            else -> "in $h h $m min"
        }
    }

    /** "3:00" for a timed event, "all day" for an all-day one. */
    fun whenText(event: DayEvent, time: (Long) -> String): String =
        if (event.allDay) "all day" else time(event.begin)

    /** Gives events with no title something to say. */
    fun titleOf(raw: String?): String = raw?.trim().orEmpty().ifEmpty { "(no title)" }
}
