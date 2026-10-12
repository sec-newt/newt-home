package io.github.secnewt.home.ui

import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

/**
 * Swipe down to close, for anything that slides up from the bottom (the sheets and All
 * apps). Once the content inside is scrolled to its top, dragging further down moves the
 * whole thing down with your finger; letting go past [DISMISS_DISTANCE], or flicking down,
 * closes it, and letting go sooner springs it back. Back, the buttons and tapping outside
 * still work, so swiping is never the only way.
 */
@Stable
class SwipeDismiss internal constructor(private val threshold: Float, private val dismiss: () -> Unit) {

    /** How far down it's been pulled, in pixels. */
    var offset by mutableFloatStateOf(0f)
        private set

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Pushing back up while pulled down undoes the pull before the list scrolls.
            if (available.y < 0f && offset > 0f) {
                val used = maxOf(available.y, -offset)
                offset += used
                return Offset(0f, used)
            }
            return Offset.Zero
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            // The list is at its top and the finger keeps going down: follow it.
            if (source == NestedScrollSource.UserInput && available.y > 0f) {
                offset += available.y
                return Offset(0f, available.y)
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (offset <= 0f) return Velocity.Zero
            settle(available.y)
            return available
        }
    }

    /** Lets go: closes if pulled far enough or flicked down, otherwise springs back. */
    suspend fun settle(velocity: Float) {
        if (offset >= threshold || velocity > FLICK_SPEED) {
            dismiss()
        } else {
            animate(offset, 0f) { value, _ -> offset = value }
        }
    }

    private companion object {
        /** Pixels per second that count as a flick down. */
        const val FLICK_SPEED = 2000f
    }
}

/** How far a pull has to go before letting go closes it. */
private val DISMISS_DISTANCE = 120.dp

@Composable
fun rememberSwipeDismiss(onDismiss: () -> Unit): SwipeDismiss {
    val threshold = with(LocalDensity.current) { DISMISS_DISTANCE.toPx() }
    val latest = rememberUpdatedState(onDismiss)
    return remember(threshold) { SwipeDismiss(threshold) { latest.value() } }
}
