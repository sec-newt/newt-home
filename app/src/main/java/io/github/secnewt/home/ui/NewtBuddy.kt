package io.github.secnewt.home.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.secnewt.home.apps.NewtMood
import io.github.secnewt.ui.NewtFonts
import io.github.secnewt.ui.NewtTone
import io.github.secnewt.ui.NewtWeather
import io.github.secnewt.ui.color
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * The newt by the clock: the same newt as the app icon, in the sky's colors, with a small
 * badge for what's going on and a caption that says it in words. Tapping it opens the phone's
 * status (like tapping the status line). It blinks now and then, unless animations are off.
 */
@Composable
fun NewtBuddy(mood: NewtMood, animate: Boolean, onTap: () -> Unit, modifier: Modifier = Modifier) {
    var blink by remember { mutableStateOf(false) }
    if (animate && mood != NewtMood.SLEEPING) {
        LaunchedEffect(mood) {
            while (true) {
                delay(Random.nextLong(4_000, 9_000))
                blink = true
                delay(160)
                blink = false
            }
        }
    }
    val start = MaterialTheme.colorScheme.primary
    val end = MaterialTheme.colorScheme.secondary
    val badge = badgeColor(mood)
    val badgeFill = MaterialTheme.colorScheme.surfaceContainerLow
    val eyeColor = MaterialTheme.colorScheme.background
    val spoken = if (mood.caption.isEmpty()) "Newt" else "Newt: ${mood.caption}"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(min = 88.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onTap)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
                onClick(label = "Phone status") { onTap(); true }
            },
    ) {
        Canvas(Modifier.size(84.dp)) {
            // The art is drawn on a 100 × 100 grid.
            scale(size.minDimension / 100f, pivot = Offset.Zero) {
                drawNewt(
                    brush = Brush.linearGradient(listOf(start, end), Offset(10f, 10f), Offset(90f, 90f)),
                    eyesClosed = blink || mood == NewtMood.SLEEPING,
                    droop = mood == NewtMood.LOW_BATTERY,
                    eyeColor = eyeColor,
                )
                if (mood != NewtMood.PLAIN) drawBadge(mood, badge, badgeFill)
            }
        }
        if (mood.caption.isNotEmpty()) {
            Text(
                mood.caption,
                fontFamily = NewtFonts.Body,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun badgeColor(mood: NewtMood): Color = when (mood) {
    NewtMood.STORM, NewtMood.LOW_BATTERY -> NewtTone.Danger.color()
    NewtMood.CHARGING -> NewtTone.Good.color()
    NewtMood.SUNNY -> NewtWeather.highColor()
    NewtMood.SNOW -> MaterialTheme.colorScheme.tertiary
    NewtMood.RAIN, NewtMood.RAIN_SOON -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.secondary
}

// The app icon's newt (app/src/main/res/drawable/ic_launcher_monochrome.xml), without the house.
private const val BODY = "M46,10.5 C52,10.5 57,14 56.5,20 C56,25 53,27.5 53.5,30 C56,34 56,48 53.5,54 C52,62 49,70 44,76 " +
    "C38,82 28,87 19,84.5 C13,83 10.5,79 12,75.5 C15,78 21,79 27,77 C33,75 38,70 40.5,62 C41,58 39.5,56 38.5,54 " +
    "C36,48 36,34 38.5,30 C39,27.5 36,25 35.5,20 C35,14 40,10.5 46,10.5 Z"
private const val EYES = "M39.3,17.5 a2.2,2.2 0 1,0 4.4,0 a2.2,2.2 0 1,0 -4.4,0 Z M48.3,17.5 a2.2,2.2 0 1,0 4.4,0 a2.2,2.2 0 1,0 -4.4,0 Z"
private const val TOES = "M21.6,25.7 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z M23.7,22.6 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z " +
    "M26.7,22.5 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z M66.4,25.7 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z " +
    "M64.3,22.6 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z M61.3,22.5 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z " +
    "M24.1,59.3 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z M26.2,62.4 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z " +
    "M29.2,62.5 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z M63.9,59.3 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z " +
    "M61.8,62.4 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z M58.8,62.5 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0 Z"
private const val LEGS = "M40,33 Q32,32 27,26 M52,33 Q60,32 65,26 M40.5,50 Q33,52 29.5,59 M51.5,50 Q59,52 62.5,59"
/** Tired: the legs hang down instead of reaching out. */
private const val LEGS_DROOP = "M40,33 Q33,36 30,42 M52,33 Q59,36 62,42 M40.5,50 Q34,55 33,62 M51.5,50 Q58,55 59,62"
private const val EYES_CLOSED = "M39.5,17.5 Q41.5,19.5 43.5,17.5 M48.5,17.5 Q50.5,19.5 52.5,17.5"

private fun path(d: String, fill: PathFillType = PathFillType.NonZero): Path =
    PathParser().parsePathString(d).toPath().apply { fillType = fill }

private val bodyOpen by lazy { path("$BODY $EYES", PathFillType.EvenOdd) }
private val bodyShut by lazy { path(BODY) }
private val toes by lazy { path(TOES) }
private val legs by lazy { path(LEGS) }
private val legsDroop by lazy { path(LEGS_DROOP) }
private val eyesShut by lazy { path(EYES_CLOSED) }

private fun DrawScope.drawNewt(brush: Brush, eyesClosed: Boolean, droop: Boolean, eyeColor: Color) {
    translate(top = if (droop) 6f else 0f) {
        drawPath(if (eyesClosed) bodyShut else bodyOpen, brush)
        if (eyesClosed) drawPath(eyesShut, eyeColor, style = Stroke(1.6f, cap = StrokeCap.Round))
        if (!droop) drawPath(toes, brush)
        drawPath(if (droop) legsDroop else legs, brush, style = Stroke(4.5f, cap = StrokeCap.Round))
    }
}

// Badge icons, drawn around (0, 0) inside a circle of radius 15.
private val badgeIcons: Map<NewtMood, String> = mapOf(
    NewtMood.SUNNY to "M-5,0 a5,5 0 1,0 10,0 a5,5 0 1,0 -10,0 M0,-10 V-8 M0,8 V10 M-10,0 H-8 M8,0 H10 " +
        "M-7,-7 L-5.6,-5.6 M5.6,5.6 L7,7 M-7,7 L-5.6,5.6 M5.6,-5.6 L7,-7",
    NewtMood.RAIN to "M-9,1 Q0,-12 9,1 Z M0,1 V8 Q0,10 -2.5,9",
    NewtMood.RAIN_SOON to "M-9,1 Q0,-12 9,1 Z M0,1 V8 Q0,10 -2.5,9",
    NewtMood.SLEEPING to "M3,-8 A8,8 0 1,0 8,4 A6,6 0 1,1 3,-8 Z",
    NewtMood.CHARGING to "M2,-10 L-6,2 H0 L-2,10 L6,-2 H0 Z",
    NewtMood.LORDS_DAY to "M0,-5 Q-5,-8 -10,-6 V7 Q-5,5 0,8 Q5,5 10,7 V-6 Q5,-8 0,-5 Z M0,-5 V8",
    NewtMood.LOW_BATTERY to "M-9,-5 H7 V5 H-9 Z M9,-2 V2 M-6,-2 V2",
    NewtMood.STORM to "M0,-9 L9,7 H-9 Z M0,-3 V2 M0,4.5 V4.6",
    NewtMood.SNOW to "M0,-9 V9 M-7.8,-4.5 L7.8,4.5 M-7.8,4.5 L7.8,-4.5",
)
private val badgePaths: Map<NewtMood, Path> by lazy { badgeIcons.mapValues { path(it.value) } }

private fun DrawScope.drawBadge(mood: NewtMood, color: Color, fill: Color) {
    val icon = badgePaths[mood] ?: return
    translate(80f, 22f) {
        drawCircle(fill, radius = 15f, center = Offset.Zero)
        drawCircle(color, radius = 15f, center = Offset.Zero, style = Stroke(2.5f))
        drawPath(icon, color, style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
