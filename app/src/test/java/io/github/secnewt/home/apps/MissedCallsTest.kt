package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MissedCallsTest {

    @Test
    fun `the label says how many, in words`() {
        assertEquals("2 missed", MissedCalls.label(2))
        assertNull(MissedCalls.label(0))
        assertNull(MissedCalls.label(null))
    }

    @Test
    fun `only Newt Phone gets it`() {
        assertTrue(MissedCalls.isNewtPhone("io.github.secnewt.dialer/io.github.secnewt.dialer.MainActivity"))
        assertFalse(MissedCalls.isNewtPhone("io.github.secnewt.dialer.debug/io.github.secnewt.dialer.MainActivity"))
        assertFalse(MissedCalls.isNewtPhone("com.android.dialer/.Main"))
    }
}
