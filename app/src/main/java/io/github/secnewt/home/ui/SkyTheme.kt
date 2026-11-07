package io.github.secnewt.home.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import io.github.secnewt.home.apps.SkyTone

/**
 * Tints the home screen with the sky: the two accent colors the outlines, headings and the
 * newt are drawn in. Text colors are left alone, and every sky color keeps 7:1 contrast.
 * [SkyTone.NIGHT] is the usual Newt blue to purple, so it changes nothing.
 */
@Composable
fun SkyTheme(tone: SkyTone, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    if (tone == SkyTone.NIGHT) {
        content()
        return
    }
    val dark = scheme.background.luminance() < 0.5f
    MaterialTheme(
        colorScheme = scheme.copy(primary = tone.start(dark), secondary = tone.end(dark)),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}
