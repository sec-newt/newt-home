package io.github.secnewt.home.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.secnewt.home.apps.BatteryLevel
import io.github.secnewt.home.ui.theme.LocalTerminalColors

/** The battery's color, like Android's: green, amber getting low, red low. Charging is green. */
@Composable
internal fun batteryColor(level: BatteryLevel): Color = when (level) {
    BatteryLevel.GOOD, BatteryLevel.CHARGING -> LocalTerminalColors.current.prompt
    BatteryLevel.GETTING_LOW -> LocalTerminalColors.current.warn
    BatteryLevel.LOW -> MaterialTheme.colorScheme.error
}

/**
 * A little battery, filled to [percent] in its [level]'s color, with a bolt while charging.
 * It's a picture only: the percent is always written next to it, so it's never color alone,
 * and screen readers skip it.
 */
@Composable
fun BatteryBar(percent: Int, level: BatteryLevel, modifier: Modifier = Modifier, width: Dp = 34.dp, height: Dp = 18.dp) {
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = batteryColor(level)
    val bolt = MaterialTheme.colorScheme.onSurface
    val behind = MaterialTheme.colorScheme.background
    Canvas(
        modifier
            .size(width, height)
            .clearAndSetSemantics { },
    ) {
        val stroke = 2.dp.toPx()
        val nub = size.width * 0.08f
        val bodyWidth = size.width - nub
        drawRoundRect(
            outline,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(bodyWidth - stroke, size.height - stroke),
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(stroke),
        )
        drawRoundRect(
            outline,
            topLeft = Offset(bodyWidth, size.height * 0.3f),
            size = Size(nub, size.height * 0.4f),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )
        val pad = stroke * 1.75f
        val inner = bodyWidth - pad * 2
        val filled = inner * (percent.coerceIn(0, 100) / 100f).coerceAtLeast(0.08f)
        drawRoundRect(
            fill,
            topLeft = Offset(pad, pad),
            size = Size(filled, size.height - pad * 2),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
        if (level == BatteryLevel.CHARGING) {
            val w = bodyWidth
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.56f, h * 0.08f)
                lineTo(w * 0.30f, h * 0.56f)
                lineTo(w * 0.48f, h * 0.56f)
                lineTo(w * 0.42f, h * 0.92f)
                lineTo(w * 0.70f, h * 0.42f)
                lineTo(w * 0.52f, h * 0.42f)
                close()
            }
            drawPath(path, behind, style = Stroke(stroke))
            drawPath(path, bolt)
        }
    }
}

/** A round dot in a calendar's own color, by each of its events. A picture only. */
@Composable
fun CalendarDot(argb: Int?, modifier: Modifier = Modifier, size: Dp = 14.dp) {
    if (argb == null) return
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(argb).copy(alpha = 1f))
            .clearAndSetSemantics { },
    )
}
