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

/** One line of `help`: what to type and what it does. */
data class CommandHelp(val example: String, val does: String)

/**
 * The prompt's short list of commands. Capitals and extra spaces never matter, and a
 * command only runs on Enter or a tap, never while you're still typing.
 */
object Commands {

    val HELP = listOf(
        CommandHelp("timer 10", "A 10 minute timer. Also timer 90s, timer 1h 30m"),
        CommandHelp("alarm 7:30", "An alarm at 7:30. Also alarm 6pm, alarm 1845"),
        CommandHelp("flashlight", "Turns the flashlight on or off. Also torch"),
        CommandHelp("call mom", "Finds Mom in Newt Phone's contacts. A number is dialed, ready to call"),
        CommandHelp("wifi", "Wi-Fi on or off and networks"),
        CommandHelp("bluetooth", "Bluetooth settings"),
        CommandHelp("files", "Opens your files app"),
        CommandHelp("ls", "Every app, A to Z"),
        CommandHelp("ask what's a good buttermilk substitute", "Asks the Claude app. ask on its own opens it"),
        CommandHelp("sys", "This phone's system info. Tapping the status line does the same"),
        CommandHelp("look clean", "Plain cards and words, like Newt Phone. look terminal goes back"),
        CommandHelp("help", "This list"),
    )

    fun parse(typed: String): CommandMatch? {
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
            else -> null
        }
    }

    private val WORDS = listOf("timer", "alarm", "flashlight", "torch", "call", "wifi", "wi-fi", "bluetooth", "files", "help", "ask", "sys", "neofetch", "fastfetch", "look", "theme")

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
    }
}
