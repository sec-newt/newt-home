package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutsTest {

    @Test
    fun `keys round-trip, even with slashes in the id`() {
        val key = Shortcuts.key("app.vanadium.browser", "https://example.org/a/b")
        assertTrue(Shortcuts.isShortcutKey(key))
        assertEquals("app.vanadium.browser" to "https://example.org/a/b", Shortcuts.parse(key))
    }

    @Test
    fun `app keys are not shortcut keys`() {
        assertFalse(Shortcuts.isShortcutKey("org.signal/.Main"))
        assertNull(Shortcuts.parse("org.signal/.Main"))
        assertNull(Shortcuts.parse("shortcut:nopackage"))
        assertNull(Shortcuts.parse("shortcut:pkg/"))
    }

    @Test
    fun `a shortcut says where it came from`() {
        assertTrue(LauncherApp("k", "Weather", from = "Vanadium").isShortcut)
        assertFalse(LauncherApp("k", "Signal").isShortcut)
    }
}
