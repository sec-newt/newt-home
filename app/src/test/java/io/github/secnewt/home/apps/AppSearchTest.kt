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

    @Test
    fun `finds apps by the name of their folder`() {
        val folders = mapOf("Magnifier" to "Blind tools", "Sudoku" to "Games")
        fun find(q: String) = AppSearch.search(apps, q, name = { it }, folder = { folders[it] })
        assertEquals(listOf("Magnifier"), find("blind").map { it.item })
        assertEquals(listOf("Magnifier"), find("TOOLS").map { it.item })
        assertEquals(Match.IN_FOLDER, find("blind").single().match)
        // A name match always ranks above a folder match, however often the other app is used.
        val withGameApp = AppSearch.search(
            listOf("Sudoku", "Games Hub"),
            "games",
            name = { it },
            uses = { if (it == "Sudoku") 99 else 0 },
            folder = { folders[it] },
        )
        assertEquals(listOf("Games Hub", "Sudoku"), withGameApp.map { it.item })
        // A folder match alone never opens an app by itself.
        assertNull(AppSearch.onlyMatch(find("games"), "games"))
    }

    @Test
    fun `finds apps by your own search words`() {
        val words = mapOf("Proton Mail" to listOf("email", "work"), "Signal" to listOf("chat"))
        fun find(q: String) = AppSearch.search(apps, q, name = { it }, words = { words[it].orEmpty() })
        assertEquals(listOf("Proton Mail"), find("EMA").map { it.item })
        assertEquals(Match.YOUR_WORD, find("work").single().match)
        assertEquals("work", find("work").single().word)
        // A name match still comes first; your word ranks above scattered letters.
        val ranked = mapOf("Signal" to listOf("messenger"), "Sudoku" to listOf("mgnfpuzzle"))
        fun rank(q: String) = AppSearch.search(apps, q, name = { it }, words = { ranked[it].orEmpty() }).map { it.item }
        assertEquals(listOf("Messages", "Signal"), rank("mes"))
        assertEquals(listOf("Sudoku", "Magnifier"), rank("mgnf"))
        // Your word alone never opens an app by itself.
        assertNull(AppSearch.onlyMatch(find("chat"), "chat"))
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

class FoldersTest {

    private val start = listOf(Folder("AI"), Folder("Games", listOf("sudoku")))

    @Test
    fun `names are tidied and must be new`() {
        assertEquals("Music and podcasts", Folders.cleanName("  Music \t and\npodcasts  "))
        assertEquals(30, Folders.cleanName("x".repeat(50)).length)
        assertEquals(start + Folder("Music"), Folders.create(start, " Music "))
        assertEquals(start, Folders.create(start, "games"))
        assertEquals(start, Folders.create(start, "   "))
    }

    @Test
    fun `rename keeps the apps and refuses a name already used`() {
        assertEquals(listOf(Folder("AI"), Folder("Fun", listOf("sudoku"))), Folders.rename(start, "Games", "Fun"))
        assertEquals(start, Folders.rename(start, "Games", "ai"))
        assertEquals(listOf(Folder("AI"), Folder("GAMES", listOf("sudoku"))), Folders.rename(start, "Games", "GAMES"))
    }

    @Test
    fun `an app is in at most one folder`() {
        val moved = Folders.moveApp(start, "sudoku", "AI")
        assertEquals(listOf(Folder("AI", listOf("sudoku")), Folder("Games")), moved)
        assertEquals("AI", Folders.folderOf(moved, "sudoku"))
        assertEquals(listOf(Folder("AI"), Folder("Games")), Folders.moveApp(start, "sudoku", null))
    }

    @Test
    fun `delete and reorder`() {
        assertEquals(listOf(Folder("AI")), Folders.delete(start, "games"))
        assertEquals(start.reversed(), Folders.move(start, "Games", -1))
        assertEquals(start, Folders.move(start, "AI", -1))
    }

    @Test
    fun `saving and loading keeps everything`() {
        val folders = listOf(Folder("Blind tools", listOf("a/b", "c/d")), Folder("Empty"))
        assertEquals(folders, Folders.decode(Folders.encode(folders)))
        assertEquals(emptyList<Folder>(), Folders.decode(""))
    }
}
