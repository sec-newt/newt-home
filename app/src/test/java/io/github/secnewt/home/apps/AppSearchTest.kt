package io.github.secnewt.home.apps

import io.github.secnewt.home.apps.AppSearch.Match
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppSearchTest {

    private val apps = listOf("Signal", "Settings", "Sudoku", "Proton Mail", "Magnifier", "Messages", "Café Bleu")

    private fun names(query: String, uses: Map<String, Int> = emptyMap()) =
        AppSearch.search(apps, query, name = { it }, uses = { uses[it] ?: 0 }).map { it.item }

    @Test
    fun `capitals and accents never matter`() {
        assertEquals(listOf("Signal"), names("SIGN"))
        assertEquals(listOf("Café Bleu"), names("cafe"))
        assertEquals("cafe bleu", AppSearch.normalize("  Café   Bleu "))
    }

    @Test
    fun `kinds of match`() {
        assertEquals(Match.START, AppSearch.match("Signal", "sig"))
        assertEquals(Match.WORD_START, AppSearch.match("Proton Mail", "mail"))
        assertEquals(Match.INSIDE, AppSearch.match("Signal", "gnal"))
        assertEquals(Match.LETTERS_IN_ORDER, AppSearch.match("Magnifier", "mgnf"))
        assertNull(AppSearch.match("Signal", "x"))
        assertNull(AppSearch.match("Signal", "   "))
    }

    @Test
    fun `better matches come first, then the apps used most`() {
        // Names starting with "s" (most used first, then A–Z), then Messages, which only has an s inside.
        assertEquals(
            listOf("Sudoku", "Settings", "Signal", "Messages"),
            names("s", mapOf("Sudoku" to 5, "Settings" to 2)),
        )
    }

    @Test
    fun `opens by itself only when one name clearly matches`() {
        fun only(q: String) = AppSearch.onlyMatch(AppSearch.search(apps, q, name = { it }), q)
        assertEquals("Signal", only("sign"))
        assertEquals("Signal", only("SIGN"))
        // Settings only has s…i…g scattered through it, so Signal is the one clear match.
        assertEquals("Signal", only("sig"))
        assertEquals("Proton Mail", only("mail"))
        assertNull("one letter is too little to go on", only("s"))
        assertNull("Magnifier and Proton Mail both have \"ma\"", only("ma"))
        assertNull("letters scattered through a name are too loose", only("mgnf"))
        assertNull(only("zz"))
    }
}

class FavoritesTest {

    @Test
    fun `toggle adds to the end and removes`() {
        assertEquals(listOf("a", "b"), Favorites.toggle(listOf("a"), "b"))
        assertEquals(listOf("b"), Favorites.toggle(listOf("a", "b"), "a"))
    }

    @Test
    fun `move up and down, staying put at the ends`() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), Favorites.move(list, "b", -1))
        assertEquals(listOf("a", "c", "b"), Favorites.move(list, "b", 1))
        assertEquals(list, Favorites.move(list, "a", -1))
        assertEquals(list, Favorites.move(list, "c", 1))
        assertEquals(list, Favorites.move(list, "missing", 1))
    }
}
