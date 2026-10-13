package io.github.secnewt.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import io.github.secnewt.ui.NewtFonts

/**
 * How the home screen looks. Both have every feature; only the dressing differs.
 *
 * - [TERMINAL]: the `~$` prompt, monospaced lists, scanlines and the ASCII newt.
 * - [CLEAN]: plain cards and words, like Newt Phone.
 */
enum class HomeLook {
    TERMINAL,
    CLEAN,
    ;

    companion object {
        /** The saved name back to a look; anything unknown is the terminal look. */
        fun named(name: String?): HomeLook = entries.firstOrNull { it.name == name } ?: TERMINAL
    }
}

val LocalHomeLook = staticCompositionLocalOf { HomeLook.TERMINAL }

/** Whether the clean look is on. */
@Composable
@ReadOnlyComposable
internal fun cleanLook(): Boolean = LocalHomeLook.current == HomeLook.CLEAN

/** Lists and labels: monospaced in the terminal look, Atkinson Hyperlegible in the clean one. */
@Composable
@ReadOnlyComposable
internal fun rowFont(): FontFamily = if (cleanLook()) NewtFonts.Body else NewtFonts.Mono
