package io.github.secnewt.home.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import io.github.secnewt.home.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.secnewt.home.ui.theme.LocalTerminalColors
import io.github.secnewt.home.ui.theme.NewtFonts
import kotlinx.coroutines.delay

/*
 * The sci-fi gadget look: faint scanlines and a big dim ASCII newt behind everything, the
 * prompt typing itself out when you come home, and a neon glow on whatever you press.
 * None of it sits between you and text you need to read, and it all stays still when
 * Android's animations are turned off.
 */

/** The newt, drawn in text. */
private val ASCII_NEWT = """
          .-~~-.
         ( o  o )
          \ \/ /
     .-----'  '-----.
    /  /|        |\  \
   '--' |        | '--'
        |        |
     .--'        '--.
    /  /|        |\  \
   '--' \        / '--'
         \      /
          \    /
           |  |
           |  |
            \  \
             '. '.
               '~~
""".trimIndent()

/**
 * The ASCII newt as a picture rather than text. Android's "High contrast text" setting
 * redraws all text at full strength with dark boxes behind it, which would make a dim
 * background newt bright; a picture is left alone. Drawn in white; tinted when shown.
 */
private fun renderNewt(context: Context, density: Float): ImageBitmap {
    val font = ResourcesCompat.getFont(context, R.font.jetbrains_mono)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(font, 700, false)
        textSize = 19f * density
        color = android.graphics.Color.WHITE
    }
    val lines = ASCII_NEWT.lines()
    val lineHeight = 20f * density
    val width = lines.maxOf { paint.measureText(it) }.toInt() + 1
    val height = (lines.size * lineHeight).toInt() + 1
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    lines.forEachIndexed { i, line -> canvas.drawText(line, 0f, (i + 0.8f) * lineHeight, paint) }
    return bitmap.asImageBitmap()
}

/**
 * Scanlines across the whole screen, a soft darkening at the edges, and the ASCII newt in
 * the middle. It's drawn, not laid out, so screen readers never see it, and its size stays
 * the same at any text size.
 */
@Composable
fun GadgetBackground(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.lightness() < 0.5f
    val line = if (dark) Color(0xFFAABEFF).copy(alpha = 0.045f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    val newt = MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.13f else 0.07f)
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val picture = remember(density) { renderNewt(context, density) }
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                val gap = 4.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    y += gap
                }
                if (dark) {
                    drawRect(
                        Brush.radialGradient(
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.35f),
                            center = Offset(size.width / 2, size.height * 0.4f),
                            radius = size.maxDimension * 0.75f,
                        ),
                    )
                }
                drawImage(
                    picture,
                    topLeft = Offset(
                        (size.width - picture.width) / 2,
                        (size.height - picture.height) / 2 + 30.dp.toPx(),
                    ),
                    colorFilter = ColorFilter.tint(newt),
                )
            },
    )
}

private fun Color.lightness(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/**
 * `newt@home:~$ date`, typed out a letter at a time each time [bootKey] changes (coming home),
 * then [content] (the clock and date) fades in. With [animate] off it's all just there.
 * The whole thing takes under half a second.
 */
@Composable
fun BootLine(command: String, bootKey: Int, animate: Boolean, content: @Composable (alpha: Float) -> Unit) {
    // The clean look has no prompt: just what's under it.
    if (cleanLook()) {
        content(1f)
        return
    }
    var shown by remember { mutableIntStateOf(command.length) }
    LaunchedEffect(bootKey, animate) {
        if (!animate) {
            shown = command.length
            return@LaunchedEffect
        }
        shown = 0
        while (shown < command.length) {
            delay(70)
            shown++
        }
    }
    val typing = shown < command.length
    val alpha by animateFloatAsState(
        targetValue = if (typing) 0f else 1f,
        animationSpec = tween(if (animate) 250 else 0),
        label = "boot",
    )
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = LocalTerminalColors.current.prompt, fontWeight = FontWeight.Bold)) {
                append("newt@home:~$ ")
            }
            append(command.take(shown))
            if (typing) {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) { append("▌") }
            }
        },
        fontFamily = NewtFonts.Mono,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clearAndSetSemantics { },
    )
    content(alpha)
}

/** Fades in with the boot line. */
fun Modifier.bootFade(alpha: Float): Modifier = graphicsLayer { this.alpha = alpha }

/** Whether something is being pressed right now, for the neon press glow. */
@Composable
fun pressed(interactions: InteractionSource): Boolean = interactions.collectIsPressedAsState().value

/** The neon press: a blue-to-purple [outline] and a soft halo in [color] around the shape. */
fun Modifier.glow(on: Boolean, color: Color, outline: BorderStroke, shape: Shape = RoundedCornerShape(10.dp)): Modifier =
    if (!on) this else this.shadow(14.dp, shape, ambientColor = color, spotColor = color).border(outline, shape)

/** Each folder's color, in order: AI purple, Blind tools cyan, Games green, Productivity blue, and round again. */
@Composable
fun folderColor(index: Int): Color {
    val colors = listOf(
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        LocalTerminalColors.current.prompt,
        MaterialTheme.colorScheme.primary,
    )
    return colors[((index % colors.size) + colors.size) % colors.size]
}
