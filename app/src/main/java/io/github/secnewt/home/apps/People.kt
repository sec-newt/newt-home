package io.github.secnewt.home.apps

import androidx.compose.runtime.Immutable

/** One of a person's phone numbers, with its [type] in a word ("mobile"), or "" when it has none. */
@Immutable
data class PhoneNumber(val number: String, val type: String)

/** Someone from your contacts with at least one phone number. Kept in memory only. */
@Immutable
data class Person(val id: Long, val lookupKey: String, val name: String, val numbers: List<PhoneNumber>)

/**
 * One phone number row as the phone's contacts hand it over; a person with two numbers has two
 * rows. [nameSource] is where the name came from (`ContactsContract.DisplayNameSources`).
 */
data class ContactRow(
    val id: Long,
    val lookupKey: String,
    val name: String?,
    val number: String?,
    val type: Int,
    val label: String?,
    val nameSource: Int,
)

/** What a tap on a person's buttons asks for. Nothing calls by itself: [Call] fills in the dialpad. */
sealed interface PersonAction {
    data class Call(val number: String) : PersonAction
    data class Text(val number: String) : PersonAction
    data class Open(val person: Person) : PersonAction
}

/** People in search: turning contact rows into people, and finding them as you type. */
object People {
    /** At most this many people show under the apps, best matches first. */
    const val MAX = 5

    /**
     * Groups rows by contact, in the order given. Names are trimmed; contacts with no name or
     * no number are left out, and the same number saved twice in different formats counts once.
     * With no name, Android shows the number or email as one; those are left out too.
     */
    fun fromRows(rows: List<ContactRow>): List<Person> =
        rows.filter { it.nameSource >= REAL_NAME }.groupBy { it.id }.mapNotNull { (id, group) ->
            val name = group.firstNotNullOfOrNull { it.name?.trim()?.ifEmpty { null } } ?: return@mapNotNull null
            val numbers = group
                .mapNotNull { row -> row.number?.trim()?.ifEmpty { null }?.let { PhoneNumber(it, typeWord(row.type, row.label)) } }
                .distinctBy { digits(it.number) }
            if (numbers.isEmpty()) null else Person(id, group.first().lookupKey, name, numbers)
        }

    /** Android's phone types (`ContactsContract.CommonDataKinds.Phone.TYPE_*`) as a plain word. */
    fun typeWord(type: Int, label: String?): String = when (type) {
        0 -> label?.trim()?.lowercase().orEmpty() // TYPE_CUSTOM: the label you typed
        1 -> "home"
        2 -> "mobile"
        3 -> "work"
        else -> ""
    }

    /** People whose name matches what's typed, with the same loose matching as apps. */
    fun search(people: List<Person>, query: String): List<Person> =
        AppSearch.search(people, query, name = { it.name }).map { it.item }.take(MAX)

    /**
     * Whether someone has what's typed together in their name (`sam`, `ortiz`), the same bar
     * an app needs to open by itself. Then no app opens by itself; letters only scattered
     * through a name are too loose to count, as with apps.
     */
    fun closeMatch(people: List<Person>, query: String): Boolean =
        people.any { AppSearch.match(it.name, query)?.let { m -> m.rank >= AppSearch.Match.INSIDE.rank } == true }

    /**
     * People is on but contact access was taken away (in Settings, say), and Android isn't in
     * the middle of asking for it. Then the switch goes off, so `help` never says "on" while
     * nobody can show.
     */
    fun accessLost(on: Boolean, canRead: Boolean, asking: Boolean): Boolean = on && !canRead && !asking

    /** `DisplayNameSources.ORGANIZATION`: a company name or better; below it is an email or number. */
    private const val REAL_NAME = 30

    /**
     * The number to compare by. A leading US/Canada `1` on an 11-digit number is dropped, so
     * "+1 555-010-0100" and "(555) 010-0100" count as one, while "+52 555-010-0100" stays
     * apart. Numbers with letters ("1-800-FLOWERS") compare as typed.
     */
    private fun digits(number: String): String {
        if (number.any { it.isLetter() }) return number.trim().lowercase()
        val d = number.filter { it.isDigit() }
        return if (d.length == 11 && d.startsWith('1')) d.drop(1) else d
    }
}
