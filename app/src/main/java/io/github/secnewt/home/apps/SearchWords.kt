package io.github.secnewt.home.apps

/**
 * Your own words for apps, so "bank" finds your banking app or "maps" finds Organic Maps.
 * Kept as app key → words.
 */
object SearchWords {

    const val MAX_WORDS = 10
    const val MAX_LENGTH = 30

    /** "Bank, money  banking" → [bank, money, banking]: lower case, no repeats, sensible sizes. */
    fun parse(typed: String): List<String> =
        typed.split(',', ' ', '\n', '\t', ';')
            .map { AppSearch.normalize(it).take(MAX_LENGTH) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_WORDS)

    /** One app per line: its key, a tab, then its words separated by spaces. */
    fun encode(words: Map<String, List<String>>): String =
        words.filter { it.value.isNotEmpty() }
            .entries.joinToString("\n") { (key, list) -> key + "\t" + list.joinToString(" ") }

    fun decode(text: String): Map<String, List<String>> =
        text.split('\n').mapNotNull { line ->
            val key = line.substringBefore('\t', "")
            val list = parse(line.substringAfter('\t', ""))
            if (key.isEmpty() || list.isEmpty()) null else key to list
        }.toMap()

    /** Sets [key]'s words, or removes them when [list] is empty. */
    fun set(words: Map<String, List<String>>, key: String, list: List<String>): Map<String, List<String>> =
        if (list.isEmpty()) words - key else words + (key to list)
}
