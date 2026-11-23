package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeopleTest {

    private fun row(id: Long, name: String?, number: String?, type: Int = 2, label: String? = null) =
        ContactRow(id, "key$id", name, number, type, label)

    @Test
    fun `rows for one contact become one person`() {
        val people = People.fromRows(listOf(row(1, "Sarah Lee", "555-0100", 2), row(1, "Sarah Lee", "555-0101", 1)))
        assertEquals(1, people.size)
        assertEquals(listOf(PhoneNumber("555-0100", "mobile"), PhoneNumber("555-0101", "home")), people[0].numbers)
    }

    @Test
    fun `the same number twice counts once`() {
        val people = People.fromRows(listOf(row(1, "Sam Ortiz", "(555) 010-0100"), row(1, "Sam Ortiz", "5550100100")))
        assertEquals(1, people[0].numbers.size)
    }

    @Test
    fun `no name or no number is left out`() {
        assertTrue(People.fromRows(listOf(row(1, null, "555-0100"), row(2, "  ", "555-0101"), row(3, "Ana Diaz", null))).isEmpty())
    }

    @Test
    fun `phone types become words`() {
        assertEquals("mobile", People.typeWord(2, null))
        assertEquals("home", People.typeWord(1, null))
        assertEquals("work", People.typeWord(3, null))
        assertEquals("gym", People.typeWord(0, "Gym"))
        assertEquals("", People.typeWord(7, null))
    }

    @Test
    fun `search matches loosely, ignores accents, best first`() {
        val people = People.fromRows(listOf(row(1, "José Ruiz", "555-0100"), row(2, "Sarah Lee", "555-0101"), row(3, "Rosa Hart", "555-0102")))
        assertEquals(listOf("José Ruiz"), People.search(people, "jose").map { it.name })
        assertEquals("Sarah Lee", People.search(people, "srh").first().name)
        assertEquals(listOf("Sarah Lee", "Rosa Hart"), People.search(people, "sa").map { it.name })
    }

    @Test
    fun `at most five people`() {
        val many = People.fromRows((1L..40L).map { row(it, "Sam $it", "555-01${it.toString().padStart(2, '0')}") })
        assertEquals(5, People.search(many, "sam").size)
    }

    @Test
    fun `a blank query finds nobody`() {
        assertTrue(People.search(People.fromRows(listOf(row(1, "Sam", "555-0100"))), " ").isEmpty())
    }
}
