package io.github.secnewt.home.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandsTest {

    private fun ready(typed: String) = (Commands.parse(typed) as CommandMatch.Ready).command

    @Test
    fun `timers take minutes, seconds and hours`() {
        assertEquals(Command.Timer(600), ready("timer 10"))
        assertEquals(Command.Timer(90), ready("Timer 90s"))
        assertEquals(Command.Timer(5400), ready("timer 1h 30m"))
        assertEquals(Command.Timer(5400), ready("timer 1h30"))
        assertEquals(Command.Timer(3600), ready("timer 1 hour"))
        assertEquals(Command.Timer(300), ready("  TIMER   5 min "))
    }

    @Test
    fun `a timer without a usable time says what it needs`() {
        assertTrue(Commands.parse("timer") is CommandMatch.Incomplete)
        assertTrue(Commands.parse("timer 1:30") is CommandMatch.Incomplete)
        assertTrue(Commands.parse("timer 0") is CommandMatch.Incomplete)
        assertTrue(Commands.parse("timer 25h") is CommandMatch.Incomplete)
    }

    @Test
    fun `alarms read times the way people type them`() {
        assertEquals(Command.Alarm(7, 30), ready("alarm 7:30"))
        assertEquals(Command.Alarm(7, 30), ready("alarm 730"))
        assertEquals(Command.Alarm(18, 45), ready("alarm 1845"))
        assertEquals(Command.Alarm(18, 0), ready("alarm 6pm"))
        assertEquals(Command.Alarm(7, 30), ready("alarm 7:30 am"))
        assertEquals(Command.Alarm(0, 0), ready("alarm 12am"))
        assertEquals(Command.Alarm(12, 0), ready("alarm 12 pm"))
        assertTrue(Commands.parse("alarm 25") is CommandMatch.Incomplete)
        assertTrue(Commands.parse("alarm 7:75") is CommandMatch.Incomplete)
        assertTrue(Commands.parse("alarm 13pm") is CommandMatch.Incomplete)
    }

    @Test
    fun `the other commands`() {
        assertEquals(Command.Flashlight(null), ready("flashlight"))
        assertEquals(Command.Flashlight(true), ready("torch on"))
        assertEquals(Command.Flashlight(false), ready("Flashlight OFF"))
        assertEquals(Command.Call("Mom"), ready("call Mom"))
        assertEquals(true, (ready("call +1 555-0100") as Command.Call).isNumber)
        assertEquals(false, (ready("call mom") as Command.Call).isNumber)
        assertEquals(Command.Wifi, ready("wifi"))
        assertEquals(Command.Wifi, ready("Wi-Fi"))
        assertEquals(Command.Bluetooth, ready("bluetooth"))
        assertEquals(Command.Files, ready("files"))
        assertEquals(Command.Help, ready("help"))
    }

    @Test
    fun `the start of a command word holds back auto-open`() {
        assertTrue(Commands.mightBecomeCommand("hel"))
        assertTrue(Commands.mightBecomeCommand("Ca"))
        assertTrue(Commands.mightBecomeCommand("blu"))
        assertEquals(false, Commands.mightBecomeCommand("sig"))
        assertEquals(false, Commands.mightBecomeCommand("cam"))
        assertEquals(false, Commands.mightBecomeCommand("call mom"))
        assertEquals(false, Commands.mightBecomeCommand(""))
    }

    @Test
    fun `call on its own opens the phone app`() {
        assertEquals(Command.Call(""), ready("call"))
    }

    @Test
    fun `ask and sys`() {
        assertEquals(Command.Ask("What's a good buttermilk substitute"), ready("ask What's a good buttermilk substitute"))
        assertEquals(Command.Ask(""), ready("ask"))
        assertEquals(Command.Sys, ready("sys"))
        assertEquals(Command.Sys, ready("neofetch"))
        assertTrue(Commands.mightBecomeCommand("neo"))
        val time = { h: Int, m: Int -> "$h:$m" }
        assertEquals("open the Claude app", Commands.describe(Command.Ask(""), time))
        assertEquals("ask Claude: why is the sky blue", Commands.describe(Command.Ask("why is the sky blue"), time))
    }

    @Test
    fun `look switches between terminal and clean`() {
        assertEquals(Command.Look(null), ready("look"))
        assertEquals(Command.Look(true), ready("look clean"))
        assertEquals(Command.Look(true), ready("Theme Modern"))
        assertEquals(Command.Look(false), ready("look terminal"))
        assertTrue(Commands.parse("look purple") is CommandMatch.Incomplete)
        assertTrue(Commands.mightBecomeCommand("loo"))
        val time = { h: Int, m: Int -> "$h:$m" }
        assertEquals("switch to the clean look", Commands.describe(Command.Look(true), time))
    }

    @Test
    fun `app names are not commands`() {
        assertNull(Commands.parse("signal"))
        assertNull(Commands.parse("tim"))
        assertNull(Commands.parse("files by google"))
        assertNull(Commands.parse(""))
    }

    @Test
    fun `each command says what it will do`() {
        val time = { h: Int, m: Int -> "%d:%02d".format(h, m) }
        assertEquals("start a 1 h 30 min timer", Commands.describe(Command.Timer(5400), time))
        assertEquals("start a 1 min 30 s timer", Commands.describe(Command.Timer(90), time))
        assertEquals("set an alarm for 7:05", Commands.describe(Command.Alarm(7, 5), time))
        assertEquals("dial 555 0100", Commands.describe(Command.Call("555 0100"), time))
        assertEquals("find Mom in your contacts", Commands.describe(Command.Call("Mom"), time))
        assertEquals("open your phone app (or type a name: call mom)", Commands.describe(Command.Call(""), time))
    }
}
