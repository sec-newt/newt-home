package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class LettersTest {

    @Test
    fun `groups know where their header is`() {
        val apps = listOf("Camera", "Clock", "Éclair", "Maps", "Messages", "Signal")
        val groups = Letters.groups(apps, { it }, offset = 1)
        assertEquals(listOf("C", "E", "M", "S"), groups.map { it.letter })
        // Prompt line at 0; C header 1, its 2 apps; E header 4; M header 6; S header 9.
        assertEquals(listOf(1, 4, 6, 9), groups.map { it.index })
        assertEquals(listOf("Maps", "Messages"), groups[2].items)
    }

    @Test
    fun `a finger picks the letter under it`() {
        assertEquals(0, Letters.pick(0f, 260f, 26))
        assertEquals(1, Letters.pick(10f, 260f, 26))
        assertEquals(25, Letters.pick(259f, 260f, 26))
        // Dragging past either end stays on the first or last letter.
        assertEquals(0, Letters.pick(-40f, 260f, 26))
        assertEquals(25, Letters.pick(900f, 260f, 26))
        assertEquals(0, Letters.pick(50f, 260f, 0))
    }
}
