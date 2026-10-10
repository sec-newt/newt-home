package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgendaTest {

    private val hour = 60L * 60 * 1000
    private val time: (Long) -> String = { "${it / hour}:00" }

    private val trash = DayEvent(1, "Trash day", 0, 24 * hour, allDay = true)
    private val standup = DayEvent(2, "Standup", 9 * hour, 10 * hour)
    private val dentist = DayEvent(3, "Dentist", 15 * hour, 16 * hour, location = "Main St")
    private val dinner = DayEvent(4, "Dinner", 18 * hour, 19 * hour)
    private val day = listOf(trash, standup, dentist, dinner)

    @Test
    fun `the line names the next timed event and counts the rest`() {
        assertEquals("next: 9:00 Standup · 3 more today", AgendaText.line(day, 8 * hour, time))
        assertEquals("next: 15:00 Dentist · 2 more today", AgendaText.line(day, 12 * hour, time))
    }

    @Test
    fun `an event happening now says until when`() {
        assertEquals("now: Standup until 10:00 · 3 more today", AgendaText.line(day, 9 * hour + hour / 2, time))
    }

    @Test
    fun `after the last timed event, an all-day one is still today`() {
        assertEquals("today: Trash day", AgendaText.line(day, 20 * hour, time))
    }

    @Test
    fun `empty and finished days say so`() {
        assertEquals("no events today", AgendaText.line(emptyList(), 8 * hour, time))
        assertEquals("nothing else today", AgendaText.line(listOf(standup, dentist), 20 * hour, time))
    }

    @Test
    fun `upcoming drops finished events but keeps all-day ones`() {
        assertEquals(listOf(trash, dentist, dinner), AgendaText.upcoming(day, 12 * hour))
        assertEquals(dentist, AgendaText.next(AgendaText.upcoming(day, 12 * hour), 12 * hour))
        assertNull(AgendaText.next(emptyList(), 0))
    }

    @Test
    fun `when text and titles`() {
        assertEquals("all day", AgendaText.whenText(trash, time))
        assertEquals("15:00", AgendaText.whenText(dentist, time))
        assertEquals("(no title)", AgendaText.titleOf(null))
        assertEquals("(no title)", AgendaText.titleOf("   "))
        assertEquals("Dentist", AgendaText.titleOf("  Dentist "))
    }
}
