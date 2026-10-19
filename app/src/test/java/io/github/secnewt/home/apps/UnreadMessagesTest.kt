package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnreadMessagesTest {

    @Test
    fun `the label says how many, in words`() {
        assertEquals("3 unread", UnreadMessages.label(3))
        assertNull(UnreadMessages.label(0))
        assertNull(UnreadMessages.label(null))
    }

    @Test
    fun `only Newt Messages gets it`() {
        assertTrue(UnreadMessages.isNewtMessages("io.github.secnewt.messages/io.github.secnewt.messages.MainActivity"))
        assertFalse(UnreadMessages.isNewtMessages("io.github.secnewt.messages.debug/io.github.secnewt.messages.MainActivity"))
        assertFalse(UnreadMessages.isNewtMessages("dev.octoshrimpy.quik/.Main"))
    }
}
