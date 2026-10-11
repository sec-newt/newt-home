package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchWordsTest {

    @Test
    fun `typed words are tidied`() {
        assertEquals(listOf("bank", "money", "cafe"), SearchWords.parse(" Bank, money  BANK;Café "))
        assertEquals(emptyList<String>(), SearchWords.parse(" , ,"))
        assertEquals(SearchWords.MAX_WORDS, SearchWords.parse((1..20).joinToString(" ") { "w$it" }).size)
        assertEquals(SearchWords.MAX_LENGTH, SearchWords.parse("x".repeat(80)).single().length)
    }

    @Test
    fun `saved and read back`() {
        val words = mapOf("org.app/.Main#0" to listOf("bank", "money"), "other" to listOf("maps"))
        assertEquals(words, SearchWords.decode(SearchWords.encode(words)))
        assertEquals(emptyMap<String, List<String>>(), SearchWords.decode(""))
    }

    @Test
    fun `setting none removes the app`() {
        val words = mapOf("a" to listOf("bank"))
        assertEquals(emptyMap<String, List<String>>(), SearchWords.set(words, "a", emptyList()))
        assertEquals(mapOf("a" to listOf("bank"), "b" to listOf("maps")), SearchWords.set(words, "b", listOf("maps")))
    }
}
