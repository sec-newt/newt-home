package io.github.secnewt.home.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.secnewt.ui.NewtTheme

/**
 * Colors Material has no slot for: the green `~$` prompt (also "good", like a full battery)
 * and amber for "getting low". Both stay at 7:1 against the background.
 */
@Immutable
data class TerminalColors(val prompt: Color, val warn: Color)

private val NightTerminal = TerminalColors(prompt = Color(0xFF9ECE6A), warn = Color(0xFFE0AF68))
private val DayTerminal = TerminalColors(prompt = Color(0xFF2D5A0A), warn = Color(0xFF6E4500))

val LocalTerminalColors = staticCompositionLocalOf { NightTerminal }

/** The shared Newt look (from the Newt library), plus Newt Home's terminal colors. */
@Composable
fun HomeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTerminalColors provides if (darkTheme) NightTerminal else DayTerminal) {
        NewtTheme(darkTheme = darkTheme, content = content)
    }
}
