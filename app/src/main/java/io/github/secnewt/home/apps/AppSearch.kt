package io.github.secnewt.home.apps

import java.text.Normalizer

/**
 * Finds apps by name as you type at the prompt. Capitals and accents never matter, and
 * apps you open more often come first among equally good matches.
 */
object AppSearch {

    /** How well a name matches, best first. */
    enum class Match(val rank: Int) {
        /** "sig" → Signal. */
        START(4),

        /** "mail" → Proton Mail: the start of a later word. */
        WORD_START(3),

        /** "gnal" → Signal. */
        INSIDE(2),

        /** "mgnf" → Magnifier: the letters in order, with gaps. */
        LETTERS_IN_ORDER(1),
    }

    data class Result<T>(val item: T, val match: Match)

    /** Lower case, accents removed, extra spaces dropped: "Café  Ü" → "cafe u". */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")

    fun match(name: String, query: String): Match? {
        val n = normalize(name)
        val q = normalize(query)
        if (q.isEmpty()) return null
        return when {
            n.startsWith(q) -> Match.START
            n.split(' ').drop(1).any { it.startsWith(q) } -> Match.WORD_START
            n.contains(q) -> Match.INSIDE
            q.length >= 2 && lettersInOrder(n, q.replace(" ", "")) -> Match.LETTERS_IN_ORDER
            else -> null
        }
    }

    private fun lettersInOrder(name: String, query: String): Boolean {
        var i = 0
        for (c in name) if (i < query.length && c == query[i]) i++
        return i == query.length
    }

    /** Every app whose name matches, best match first, then most used, then A–Z. */
    fun <T> search(
        items: List<T>,
        query: String,
        name: (T) -> String,
        uses: (T) -> Int = { 0 },
    ): List<Result<T>> =
        items.mapNotNull { item -> match(name(item), query)?.let { Result(item, it) } }
            .sortedWith(
                compareByDescending<Result<T>> { it.match.rank }
                    .thenByDescending { uses(it.item) }
                    .thenBy { normalize(name(it.item)) },
            )

    /**
     * The app to open without pressing Enter: when the query is at least 2 letters and
     * exactly one app has them together in its name ("sig" → Signal). Names that only have
     * the letters scattered through them ("Settings" has s…i…g) are too loose to count,
     * either to open an app or to stop one opening.
     */
    fun <T> onlyMatch(results: List<Result<T>>, query: String): T? {
        if (normalize(query).length < 2) return null
        return results.filter { it.match != Match.LETTERS_IN_ORDER }.singleOrNull()?.item
    }
}
