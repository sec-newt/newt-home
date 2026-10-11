package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilesTest {

    @Test
    fun `main apps keep their old keys, others add the profile`() {
        assertEquals("org.signal/.Main", Profiles.key("org.signal/.Main", 0, main = true))
        assertEquals("org.signal/.Main#10", Profiles.key("org.signal/.Main", 10, main = false))
    }

    @Test
    fun `private space stays out of search`() {
        assertTrue(Profiles.searchable(ProfileKind.MAIN))
        assertTrue(Profiles.searchable(ProfileKind.WORK))
        assertFalse(Profiles.searchable(ProfileKind.PRIVATE))
    }

    @Test
    fun `work comes before private space`() {
        val views = listOf(
            ProfileView(ProfileKind.PRIVATE, locked = true, apps = emptyList()),
            ProfileView(ProfileKind.MAIN, locked = false, apps = emptyList()),
            ProfileView(ProfileKind.WORK, locked = false, apps = emptyList()),
        )
        assertEquals(listOf(ProfileKind.WORK, ProfileKind.PRIVATE), Profiles.ordered(views).map { it.kind })
    }
}
