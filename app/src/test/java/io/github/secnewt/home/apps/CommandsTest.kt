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
    fun `buttons shows or hides the screen buttons`() {
        assertEquals(Command.Buttons(null), ready("buttons"))
        assertEquals(Command.Buttons(false), ready("buttons off"))
        assertEquals(Command.Buttons(true), ready("Buttons On"))
        assertEquals(Command.Buttons(false), ready("buttons hide"))
        assertTrue(Commands.parse("buttons purple") is CommandMatch.Incomplete)
        assertTrue(Commands.mightBecomeCommand("butt"))
        val time = { h: Int, m: Int -> "$h:$m" }
        assertEquals("hide the Folders and All apps buttons", Commands.describe(Command.Buttons(false), time))
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
        // Games named with numbers still open by themselves: a plain number is never an answer.
        assertNull(Commands.parse("2048"))
        assertNull(Commands.parse("42"))
        assertNull(Commands.parse("20"))
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

    @Test
    fun `sky, pet and tiles switch home screen settings`() {
        assertEquals(Command.Switch(HomeSwitch.SKY, false), ready("sky off"))
        assertEquals(Command.Switch(HomeSwitch.SKY, true), ready("Sky On"))
        assertEquals(Command.Switch(HomeSwitch.NEWT, false), ready("pet off"))
        assertEquals(Command.Switch(HomeSwitch.BIG_TILES, true), ready("tiles big"))
        assertEquals(Command.Switch(HomeSwitch.BIG_TILES, false), ready("tiles small"))
        assertEquals(Command.Switch(HomeSwitch.BIG_TILES, false), ready("tiles off"))
    }

    @Test
    fun `scene and columns switch the sky picture and favorites to a row`() {
        assertEquals(Command.Switch(HomeSwitch.SCENE, false), ready("scene off"))
        assertEquals(Command.Switch(HomeSwitch.SCENE, true), ready("Scene on"))
        assertEquals(Command.Switch(HomeSwitch.TWO_ACROSS, false), ready("columns 1"))
        assertEquals(Command.Switch(HomeSwitch.TWO_ACROSS, true), ready("columns 2"))
        assertEquals(Command.Switch(HomeSwitch.TWO_ACROSS, false), ready("columns off"))
        assertTrue(Commands.parse("columns") is CommandMatch.Incomplete)
    }

    @Test
    fun `a switch word alone asks for on or off, so Enter still opens an app`() {
        assertTrue(Commands.parse("sky") is CommandMatch.Incomplete)
        assertTrue(Commands.parse("pet") is CommandMatch.Incomplete)
        assertTrue(Commands.parse("sky blue") is CommandMatch.Incomplete)
        // Every Newt app starts with "newt", so it's never a command.
        assertEquals(null, Commands.parse("newt"))
    }

    @Test
    fun `math and conversions are answers that copy`() {
        assertEquals(Command.Answer("3.107 miles", "3.107"), ready("5 km to mi"))
        assertEquals(Command.Answer("12", "12"), ready("15% of 80"))
        assertEquals("= 12", Commands.describe(Command.Answer("12", "12")) { _, _ -> "" })
        assertTrue(Commands.parse("5 kg to miles") is CommandMatch.Incomplete)
        // Command words still win: call 555-0100 dials, it isn't subtraction.
        assertEquals(Command.Call("555-0100"), ready("call 555-0100"))
    }

    @Test
    fun `answers off leaves numbers alone`() {
        assertNull(Commands.parse("2+2", calculator = false))
        assertNull(Commands.parse("5 km to mi", calculator = false))
        assertEquals(Command.Switch(HomeSwitch.MATH, false), ready("answers off"))
        assertEquals(Command.Switch(HomeSwitch.MATH, true), ready("answers on"))
    }
}
