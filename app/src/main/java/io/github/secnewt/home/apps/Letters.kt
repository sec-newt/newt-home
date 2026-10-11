package io.github.secnewt.home.apps

/** All apps, grouped by first letter for the letter headers and the letter strip. */
object Letters {

    /** A letter's group, and where its header sits in the list (after [offset] items before it). */
    data class Group<T>(val letter: String, val items: List<T>, val index: Int)

    /**
     * [items] (already sorted A–Z, accents ignored) grouped by first letter. Each group's
     * [Group.index] is its header's position, counting one row per header and per item.
     */
    fun <T> groups(items: List<T>, name: (T) -> String, offset: Int = 0): List<Group<T>> {
        var index = offset
        return items.groupBy { AppSearch.normalize(name(it)).take(1).uppercase().ifEmpty { "#" } }
            .map { (letter, list) -> Group(letter, list, index).also { index += 1 + list.size } }
    }

    /** Which of [count] letters a finger at [y] is on, in a strip [height] tall. */
    fun pick(y: Float, height: Float, count: Int): Int =
        if (count <= 0 || height <= 0f) 0 else (y / height * count).toInt().coerceIn(0, count - 1)
}
