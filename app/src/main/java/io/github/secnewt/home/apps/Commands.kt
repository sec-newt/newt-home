package io.github.secnewt.home.apps

import androidx.compose.runtime.Immutable

/** Something the prompt can do besides opening an app. */
sealed interface Command {
    /** A countdown in the clock app. */
    data class Timer(val seconds: Int) : Command

    /** An alarm in the clock app, in 24-hour time. */
    data class Alarm(val hour: Int, val minute: Int) : Command

    /** [on] true or false, or null to switch it the other way. */
    data class Flashlight(val on: Boolean?) : Command

    /** A phone number to dial, or a name for the phone app to look up. */
    data class Call(val who: String) : Command {
        val isNumber: Boolean get() = NUMBER.matches(who)
    }

    data object Wifi : Command
    data object Bluetooth : Command
    data object Files : Command
    data object Help : Command

    /** A question handed to the Claude app (or another app, if Claude isn't installed). Empty opens it. */
    data class Ask(val question: String) : Command

    /** This phone's system info, like the Linux `neofetch` command. */
    data object Sys : Command

    /** [clean] true for the clean look, false for the terminal look, or null to switch. */
    data class Look(val clean: Boolean?) : Command

    /** [show] true to show the Folders and All apps buttons, false to hide them, or null to switch. */
    data class Buttons(val show: Boolean?) : Command

    /** A home screen setting ([HomeSwitch]) [on], off, or null to switch it the other way. */
    data class Switch(val what: HomeSwitch, val on: Boolean?) : Command

    /** The calculator's answer ([Calculator]): [shown] at the prompt, [copy] copied on Enter or a tap. */
    data class Answer(val shown: String, val copy: String) : Command
}

/**
 * Home screen settings that are simply on or off, each with its own command word. All are
 * also switches at the end of `help`, so none needs typing. The word alone isn't a command
 * (`sky` might be the start of an app's name), and the newt's word is `pet`, since every
 * Newt app starts with "newt".
 */
enum class HomeSwitch(val word: String, val title: String, val hint: String, val onWord: String, val offWord: String) {
    SKY("sky", "Sky colors", "Outlines follow the time of day and the weather", "on", "off"),
    NEWT("pet", "Newt by the clock", "Clean look: shows how the phone and the day are going", "on", "off"),
    BIG_TILES("tiles", "Big tiles", "Clean look: taller favorites, icon above the name", "big", "small"),
    MATH("answers", "Math answers", "Answers math and conversions as you type, like 5 km to mi", "on", "off"),
    SCENE("scene", "Sky picture", "Clean dark look: northern lights, sun, rain or snow behind the home screen", "on", "off"),
    TWO_ACROSS("columns", "Two favorites to a row", "Clean look: two to a row even at big text sizes", "2", "1"),
}

private val NUMBER = Regex("""\+?[\d\s().\-*#]*\d[\d\s().\-*#]*""")

/** What the prompt makes of what's typed, when it starts with a command word. */
@Immutable
sealed interface CommandMatch {
    /** A complete command: Enter or a tap runs it. Never runs by itself. */
    data class Ready(val command: Command) : CommandMatch

    /** A command word without what it needs yet, like `timer` with no time. */
    data class Incomplete(val word: String, val hint: String) : CommandMatch
}

/** The groups in `help`, in the order they're shown. */
enum class HelpTopic(val title: String) { TIME("Time"), PEOPLE("People"), PHONE("Phone"), MATH("Math & units"), APPS("Apps & look") }

/** One line of `help`: what to type, what it does, and which group it's in. */
data class CommandHelp(val example: String, val does: String, val topic: HelpTopic)

/**
 * The prompt's short list of commands. Capitals and extra spaces never matter, and a
 * command only runs on Enter or a tap, never while you're still typing.
 */
object Commands {

    val HELP = listOf(
        CommandHelp("timer 10", "A 10 minute timer. Also timer 90s, timer 1h 30m", HelpTopic.TIME),
        CommandHelp("alarm 7:30", "An alarm at 7:30. Also alarm 6pm, alarm 1845", HelpTopic.TIME),
        CommandHelp("15% of 80", "Math: + − × ÷, ^, percent and parentheses. The answer shows as you type; Enter copies it", HelpTopic.MATH),
        CommandHelp("5 km to mi", "Converts length, weight, cooking, temperature, speed, time, data and area", HelpTopic.MATH),
        CommandHelp("= 20 x 3", "Starting with = never opens an app by itself, handy when a sum starts like a game's name", HelpTopic.MATH),
        CommandHelp("350 f", "A number and a unit converts the usual way: °F to °C, cups to milliliters, km to miles", HelpTopic.MATH),
        CommandHelp("flashlight", "Turns the flashlight on or off. Also torch", HelpTopic.PHONE),
        CommandHelp("call mom", "Finds Mom in Newt Phone's contacts. A number is dialed, ready to call", HelpTopic.PEOPLE),
        CommandHelp("wifi", "Wi-Fi on or off and networks", HelpTopic.PHONE),
        CommandHelp("bluetooth", "Bluetooth settings", HelpTopic.PHONE),
        CommandHelp("files", "Opens your files app", HelpTopic.APPS),
        CommandHelp("ls", "Every app, A to Z", HelpTopic.APPS),
        CommandHelp("ask what's a good buttermilk substitute", "Asks the Claude app. ask on its own opens it", HelpTopic.APPS),
        CommandHelp("sys", "This phone's system info. Tapping the status line does the same", HelpTopic.PHONE),
        CommandHelp("look clean", "Plain cards and words, like Newt Phone. look terminal goes back", HelpTopic.APPS),
        CommandHelp("buttons off", "Hides the Folders and All apps buttons; swiping still works. buttons on brings them back", HelpTopic.APPS),
        CommandHelp("sky off", "Plain blue and purple outlines. sky on lets them follow the time and the weather", HelpTopic.APPS),
        CommandHelp("scene off", "Hides the sky picture behind the home screen. scene on brings it back", HelpTopic.APPS),
        CommandHelp("columns 1", "One favorite to a row at big text sizes. columns 2 puts two side by side", HelpTopic.APPS),
        CommandHelp("pet off", "Hides the newt by the clock. pet on brings it back", HelpTopic.APPS),
        CommandHelp("answers off", "Stops showing math and conversion answers. answers on brings them back", HelpTopic.MATH),
        CommandHelp("tiles big", "Taller favorites, icon above the name. tiles small fits more on the screen", HelpTopic.APPS),
        CommandHelp("help", "This list", HelpTopic.APPS),
    )

    /** [calculator] false (`answers off`) leaves math and conversions alone. */
    fun parse(typed: String, calculator: Boolean = true): CommandMatch? {
        val text = typed.trim().replace(Regex("""\s+"""), " ")
        val word = text.substringBefore(' ').lowercase()
        val rest = text.substringAfter(' ', "").trim()
        val lower = rest.lowercase()
        return when (word) {
            "timer" -> if (rest.isEmpty()) {
                CommandMatch.Incomplete("timer", "how long? like timer 10, timer 90s or timer 1h 30m")
            } else {
                duration(lower)?.let { CommandMatch.Ready(Command.Timer(it)) }
                    ?: CommandMatch.Incomplete("timer", "a number of minutes, like timer 10, or timer 90s")
            }
            "alarm" -> if (rest.isEmpty()) {
                CommandMatch.Incomplete("alarm", "what time? like alarm 7:30 or alarm 6pm")
            } else {
                clockTime(lower)?.let { (h, m) -> CommandMatch.Ready(Command.Alarm(h, m)) }
                    ?: CommandMatch.Incomplete("alarm", "a time, like alarm 7:30 or alarm 6pm")
            }
            "flashlight", "torch" -> when (lower) {
                "" -> CommandMatch.Ready(Command.Flashlight(null))
                "on" -> CommandMatch.Ready(Command.Flashlight(true))
                "off" -> CommandMatch.Ready(Command.Flashlight(false))
                else -> CommandMatch.Incomplete(word, "$word, $word on, or $word off")
            }
            "call" -> CommandMatch.Ready(Command.Call(rest))
            "wifi", "wi-fi" -> if (rest.isEmpty()) CommandMatch.Ready(Command.Wifi) else null
            "bluetooth" -> if (rest.isEmpty()) CommandMatch.Ready(Command.Bluetooth) else null
            "files" -> if (rest.isEmpty()) CommandMatch.Ready(Command.Files) else null
            "help", "?" -> if (rest.isEmpty()) CommandMatch.Ready(Command.Help) else null
            "ask" -> CommandMatch.Ready(Command.Ask(rest))
            "sys", "neofetch", "fastfetch" -> if (rest.isEmpty()) CommandMatch.Ready(Command.Sys) else null
            "look", "theme" -> when (lower) {
                "" -> CommandMatch.Ready(Command.Look(null))
                "clean", "plain", "modern" -> CommandMatch.Ready(Command.Look(true))
                "terminal", "classic" -> CommandMatch.Ready(Command.Look(false))
                else -> CommandMatch.Incomplete(word, "$word clean or $word terminal")
            }
            "buttons" -> when (lower) {
                "" -> CommandMatch.Ready(Command.Buttons(null))
                "on", "show" -> CommandMatch.Ready(Command.Buttons(true))
                "off", "hide" -> CommandMatch.Ready(Command.Buttons(false))
                else -> CommandMatch.Incomplete(word, "buttons on or buttons off")
            }
            else -> HomeSwitch.entries.firstOrNull { it.word == word }?.let { sw ->
                when (lower) {
                    sw.onWord, "on", "show" -> CommandMatch.Ready(Command.Switch(sw, true))
                    sw.offWord, "off", "hide" -> CommandMatch.Ready(Command.Switch(sw, false))
                    else -> CommandMatch.Incomplete(word, "${sw.word} ${sw.onWord} or ${sw.word} ${sw.offWord}")
                }
            } ?: if (calculator) calculate(text) else null
        }
    }

    /** Not a command word: maybe math or a conversion. */
    private fun calculate(text: String): CommandMatch? = when (val r = Calculator.answer(text)) {
        is Calculator.Result.Answer -> CommandMatch.Ready(Command.Answer(r.shown, r.copy))
        is Calculator.Result.Problem -> CommandMatch.Incomplete("", r.hint)
        null -> null
    }

    private val WORDS = listOf("timer", "alarm", "flashlight", "torch", "call", "wifi", "wi-fi", "bluetooth", "files", "help", "ask", "sys", "neofetch", "fastfetch", "look", "theme", "buttons") +
        HomeSwitch.entries.map { it.word }

    /**
     * Whether what's typed could still become a command (`hel` on the way to `help`). The
     * prompt doesn't auto-open an app then, so a command is never beaten to it by an app
     * whose name starts the same way. One more letter, or Enter, opens the app as usual.
     */
    fun mightBecomeCommand(typed: String): Boolean {
        val text = typed.trim().lowercase()
        return text.isNotEmpty() && ' ' !in text && WORDS.any { it.startsWith(text) }
    }

    /**
     * "10" (minutes), "90s", "1h 30m", "1h30", "2 hours" → seconds; up to 24 hours.
     * A bare number after hours means minutes.
     */
    fun duration(text: String): Int? {
        val part = Regex("""(\d+)\s*(hours|hour|hrs|hr|h|minutes|minute|mins|min|m|seconds|second|secs|sec|s)?""")
        val compact = text.replace(" ", "")
        var total = 0L
        var at = 0
        var any = false
        while (at < compact.length) {
            val m = part.matchAt(compact, at) ?: return null
            val n = m.groupValues[1].toLongOrNull() ?: return null
            total += n * when (m.groupValues[2].firstOrNull()) {
                'h' -> 3600
                's' -> 1
                else -> 60
            }
            at = m.range.last + 1
            any = true
        }
        return if (any && total in 1..24 * 3600L) total.toInt() else null
    }

    /** "7:30", "7.30", "730", "1845", "7", "6pm", "7:30 am" → hour (0–23) and minute. */
    fun clockTime(text: String): Pair<Int, Int>? {
        val m = Regex("""^(\d{1,2})(?:[:.]?(\d{2}))?\s*(am|pm|a|p)?$""").find(text.replace(" ", "")) ?: return null
        var hour = m.groupValues[1].toInt()
        val minute = m.groupValues[2].ifEmpty { "0" }.toInt()
        val half = m.groupValues[3]
        if (minute > 59) return null
        if (half.isNotEmpty()) {
            if (hour !in 1..12) return null
            hour = hour % 12 + if (half.startsWith("p")) 12 else 0
        } else if (hour > 23) {
            return null
        }
        return hour to minute
    }

    /** "10 min", "1 h 30 min", "90 s". */
    fun durationText(seconds: Int): String {
        val h = seconds / 3600
        val m = seconds % 3600 / 60
        val s = seconds % 60
        return listOfNotNull(
            h.takeIf { it > 0 }?.let { "$it h" },
            m.takeIf { it > 0 }?.let { "$it min" },
            s.takeIf { it > 0 }?.let { "$it s" },
        ).joinToString(" ")
    }

    /** What running it will do, in words. [time] formats an hour and minute the phone's way. */
    fun describe(command: Command, time: (Int, Int) -> String): String = when (command) {
        is Command.Timer -> "start a ${durationText(command.seconds)} timer"
        is Command.Alarm -> "set an alarm for ${time(command.hour, command.minute)}"
        is Command.Flashlight -> when (command.on) {
            true -> "turn the flashlight on"
            false -> "turn the flashlight off"
            null -> "switch the flashlight on or off"
        }
        is Command.Call -> when {
            command.who.isEmpty() -> "open your phone app (or type a name: call mom)"
            command.isNumber -> "dial ${command.who}"
            else -> "find ${command.who} in your contacts"
        }
        Command.Wifi -> "open Wi-Fi"
        Command.Bluetooth -> "open Bluetooth settings"
        Command.Files -> "open your files app"
        Command.Help -> "list the commands"
        is Command.Ask -> if (command.question.isEmpty()) "open the Claude app" else "ask Claude: ${command.question}"
        Command.Sys -> "show this phone's system info"
        is Command.Look -> when (command.clean) {
            true -> "switch to the clean look"
            false -> "switch to the terminal look"
            null -> "switch between the terminal and clean looks"
        }
        is Command.Buttons -> when (command.show) {
            true -> "show the Folders and All apps buttons"
            false -> "hide the Folders and All apps buttons"
            null -> "show or hide the Folders and All apps buttons"
        }
        is Command.Switch -> when (command.what) {
            HomeSwitch.SKY -> when (command.on) {
                true -> "let the colors follow the time of day and the weather"
                false -> "keep the usual blue and purple colors"
                null -> "switch sky colors on or off"
            }
            HomeSwitch.NEWT -> when (command.on) {
                true -> "show the newt by the clock"
                false -> "hide the newt by the clock"
                null -> "show or hide the newt by the clock"
            }
            HomeSwitch.BIG_TILES -> when (command.on) {
                true -> "use big favorite tiles"
                false -> "use small favorite tiles, so more fit"
                null -> "switch between big and small favorite tiles"
            }
            HomeSwitch.SCENE -> when (command.on) {
                true -> "show the sky picture behind the home screen"
                false -> "hide the sky picture"
                null -> "show or hide the sky picture"
            }
            HomeSwitch.TWO_ACROSS -> when (command.on) {
                true -> "put two favorites to a row"
                false -> "put one favorite to a row at big text sizes"
                null -> "switch between one and two favorites to a row"
            }
            HomeSwitch.MATH -> when (command.on) {
                true -> "answer math and conversions as you type"
                false -> "stop answering math and conversions"
                null -> "switch math answers on or off"
            }
        }
        is Command.Answer -> "= ${command.shown}"
    }
}
