package io.github.secnewt.home.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.secnewt.home.apps.SkyTone
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** How long the sky moves after you come home, before it holds still to save battery. */
internal const val SCENE_MOVES_MS = 30_000

/** One loop of the rain, snow and aurora's sway. */
private const val SCENE_LOOP_MS = 6_000

/**
 * The sky behind the clean dark home screen: northern lights at night, the sun at dawn,
 * noon and dusk, and clouds, rain, snow or a storm when the weather says so. It's only a
 * picture: nothing is written on it, and every word on the home screen sits on a solid card
 * in front of it. The mountains' tops are at [horizon], just under the clear "window" at the
 * top of the screen.
 *
 * With [animate] (Android's animations on), the rain falls and the lights sway for
 * [SCENE_MOVES_MS] each time home comes back, then it holds still. It's drawn from shapes,
 * not pictures, so it takes no room in the app, and only redraws while it moves.
 */
@Composable
fun SkyScene(tone: SkyTone, horizon: Dp, motion: Animatable<Float, AnimationVector1D>, modifier: Modifier = Modifier) {
    val look = remember(tone) { SceneLook.of(tone) }
    // Remembered, so the shapes are only worked out again when the size or the sky changes,
    // not every time the clock ticks.
    val picture = remember(look, horizon, motion) {
        Modifier.drawWithCache {
            val hz = horizon.toPx().coerceAtMost(size.height)
            val fixed = SceneShapes.make(size, hz, look, density)
            onDrawBehind { drawScene(fixed, look, hz, motion.value, motion.isRunning) }
        }
    }
    Box(modifier.clearAndSetSemantics { }.clipToBounds().then(picture))
}

/**
 * Where the sky's motion is, 0 to 1 and round again. Kept by the home page rather than the
 * picture, so typing (which hides the picture) doesn't start the 30 seconds over. Only runs
 * when [enabled]: the picture shows and Android's animations are on.
 */
@Composable
fun rememberSkyMotion(enabled: Boolean): Animatable<Float, AnimationVector1D> {
    val motion = remember { Animatable(0f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    if (enabled) {
        LaunchedEffect(lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                repeat(SCENE_MOVES_MS / SCENE_LOOP_MS) {
                    motion.snapTo(0f)
                    motion.animateTo(1f, tween(SCENE_LOOP_MS, easing = LinearEasing))
                }
            }
        }
    }
    return motion
}

/** The colors and pieces of one sky. */
internal data class SceneLook(
    val sky: List<Color>,
    val stars: Float = 0f,
    val sun: Sun? = null,
    val curtains: List<Curtain> = emptyList(),
    val puffs: Boolean = false,
    val bands: Color? = null,
    val bandAlpha: Float = 0.5f,
    val fog: Boolean = false,
    val back: Color,
    val front: Color,
    val pines: Color,
    val caps: Boolean = false,
    val rain: Boolean = false,
    val snow: Boolean = false,
    val bolt: Boolean = false,
) {
    /** A sun (or its glow) at [x] across and [y] down the window, both 0 to 1. */
    data class Sun(val x: Float, val y: Float, val radiusDp: Float, val color: Color, val glow: Color)

    /** One curtain of northern lights: where its foot sits (0 to 1 down the window), how tall and how strong. */
    data class Curtain(val base: Float, val height: Float, val sway: Float, val alpha: Float, val top: Color, val bottom: Color, val seed: Int)

    companion object {
        private val green = Color(0xFF5CFFB0)
        private val teal = Color(0xFF3FD8D0)
        private val violet = Color(0xFFB07CFF)
        private val pink = Color(0xFFFF7AC8)

        fun of(tone: SkyTone): SceneLook = when (tone) {
            SkyTone.NIGHT -> SceneLook(
                sky = listOf(Color(0xFF040812), Color(0xFF0A1630), Color(0xFF0F2A3C)),
                stars = 1f,
                curtains = listOf(
                    Curtain(0.55f, 0.55f, 0.05f, 0.85f, violet, green, 1),
                    Curtain(0.72f, 0.45f, 0.04f, 0.75f, teal, green, 2),
                    Curtain(0.42f, 0.35f, 0.06f, 0.45f, pink, violet, 3),
                ),
                back = Color(0xFF16263A), front = Color(0xFF080E16), pines = Color(0xFF04080D),
            )
            SkyTone.DAWN -> SceneLook(
                sky = listOf(Color(0xFF1A1440), Color(0xFF5A2A5E), Color(0xFFC2566B), Color(0xFFF29A5C)),
                stars = 0.35f,
                sun = Sun(0.72f, 0.98f, 26f, Color(0xFFFFD08A), Color(0xFFFFB86C)),
                back = Color(0xFF3A2440), front = Color(0xFF140C18), pines = Color(0xFF0A060C),
            )
            SkyTone.DAY -> SceneLook(
                sky = listOf(Color(0xFF15508F), Color(0xFF3A86C8), Color(0xFF7DBDE8)),
                sun = Sun(0.78f, 0.22f, 20f, Color(0xFFFFF4C2), Color(0xFFFFE08A)),
                puffs = true,
                back = Color(0xFF2E5E78), front = Color(0xFF0E2430), pines = Color(0xFF07151C),
            )
            SkyTone.DUSK -> SceneLook(
                sky = listOf(Color(0xFF140F33), Color(0xFF3D1F5C), Color(0xFFA0406A), Color(0xFFE8794A)),
                stars = 0.5f,
                sun = Sun(0.25f, 0.92f, 24f, Color(0xFFFFB070), Color(0xFFFF9E64)),
                curtains = listOf(Curtain(0.45f, 0.4f, 0.04f, 0.4f, violet, violet, 4)),
                back = Color(0xFF2E1A3E), front = Color(0xFF110A1A), pines = Color(0xFF08050D),
            )
            SkyTone.OVERCAST -> SceneLook(
                sky = listOf(Color(0xFF1C2230), Color(0xFF2C3445), Color(0xFF3E4758)),
                bands = Color(0xFF7A869C), bandAlpha = 0.35f, fog = true,
                back = Color(0xFF2A3242), front = Color(0xFF111620), pines = Color(0xFF0A0D14),
            )
            SkyTone.RAIN -> SceneLook(
                sky = listOf(Color(0xFF0A111E), Color(0xFF16223A), Color(0xFF22314A)),
                bands = Color(0xFF4A5A78), bandAlpha = 0.45f, rain = true,
                back = Color(0xFF1A2536), front = Color(0xFF0A1018), pines = Color(0xFF05090E),
            )
            SkyTone.SNOW -> SceneLook(
                sky = listOf(Color(0xFF0E1828), Color(0xFF1F2E4A), Color(0xFF3A4C6A)),
                stars = 0.25f,
                curtains = listOf(Curtain(0.5f, 0.4f, 0.04f, 0.35f, teal, green, 5)),
                bands = Color(0xFF8FA3C2), bandAlpha = 0.25f, snow = true, caps = true,
                back = Color(0xFF1E2A40), front = Color(0xFF0E1622), pines = Color(0xFF070C14),
            )
            SkyTone.STORM -> SceneLook(
                sky = listOf(Color(0xFF0C0510), Color(0xFF2A0F1E), Color(0xFF4A1A22)),
                bands = Color(0xFF5A2A38), bandAlpha = 0.55f, rain = true, bolt = true,
                back = Color(0xFF2A1420), front = Color(0xFF10070C), pines = Color(0xFF080306),
            )
        }
    }
}

/** The parts that don't move, worked out once for a size and a sky. */
private class SceneShapes(
    val back: Path,
    val front: Path,
    val pines: Path,
    val caps: Path?,
    val stars: List<Triple<Offset, Float, Float>>,
    val puffs: List<Pair<Offset, Float>>,
    val bands: List<Pair<Offset, Size>>,
    val drops: List<Offset>,
    val bolt: Path?,
    /** Each curtain's brush and its rays: across, how tall (0 to 1) and how strong (0 to 1). */
    val curtains: List<Pair<Brush, List<FloatArray>>>,
    val dp: Float,
) {
    companion object {
        fun make(size: Size, hz: Float, look: SceneLook, density: Float): SceneShapes {
            val w = size.width
            val h = size.height
            val dp = density
            val r = Random(11)
            fun ridge(top: Float, spread: Float, steps: Int): List<Offset> {
                var y = top
                return (0..steps).map { i ->
                    y = (y + (r.nextFloat() - 0.5f) * spread).coerceIn(top - spread, top + spread)
                    Offset(w * i / steps, y)
                }
            }
            fun fill(points: List<Offset>) = Path().apply {
                moveTo(0f, h)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(w, h)
                close()
            }
            val backPts = ridge(hz - 46 * dp, 22 * dp, 9)
            val frontPts = ridge(hz - 14 * dp, 10 * dp, 14)
            val caps = if (look.caps) Path().apply {
                // A white cap on each back peak.
                backPts.zipWithNext().forEach { (a, b) ->
                    val peak = if (a.y < b.y) a else b
                    moveTo(peak.x - 14 * dp, peak.y + 10 * dp)
                    lineTo(peak.x, peak.y)
                    lineTo(peak.x + 14 * dp, peak.y + 10 * dp)
                    close()
                }
            } else null
            val pines = Path().apply {
                var x = 4 * dp
                while (x < w) {
                    val t = x / w * (frontPts.size - 1)
                    val i = t.toInt().coerceAtMost(frontPts.size - 2)
                    val base = frontPts[i].y + (frontPts[i + 1].y - frontPts[i].y) * (t - i) + 4 * dp
                    val tall = (10 + r.nextFloat() * 14) * dp
                    moveTo(x - tall * 0.3f, base)
                    lineTo(x, base - tall)
                    lineTo(x + tall * 0.3f, base)
                    close()
                    x += (6 + r.nextFloat() * 16) * dp
                }
            }
            val stars = List(90) {
                Triple(Offset(r.nextFloat() * w, r.nextFloat() * hz * 0.9f), (0.6f + r.nextFloat()) * dp, 0.3f + r.nextFloat() * 0.7f)
            }
            val puffs = if (look.puffs) {
                listOf(0.18f to 0.3f, 0.5f to 0.5f, 0.86f to 0.62f).flatMap { (cx, cy) ->
                    val c = Offset(w * cx, hz * cy)
                    listOf(Offset(-18f, 4f) to 14f, Offset(0f, -4f) to 20f, Offset(20f, 2f) to 15f, Offset(6f, 8f) to 14f)
                        .map { (o, rad) -> Offset(c.x + o.x * dp, c.y + o.y * dp) to rad * dp }
                }
            } else {
                emptyList()
            }
            val bands = if (look.bands != null) {
                List(7) { i ->
                    val y = hz * (0.08f + i * 0.11f) + r.nextFloat() * 8 * dp
                    val bw = w * (0.5f + r.nextFloat() * 0.6f)
                    Offset(r.nextFloat() * w - bw / 2, y) to Size(bw, (18 + r.nextFloat() * 18) * dp)
                }
            } else {
                emptyList()
            }
            val drops = List(if (look.rain) 110 else if (look.snow) 80 else 0) { Offset(r.nextFloat() * w, r.nextFloat() * h) }
            val bolt = if (look.bolt) Path().apply {
                var x = w * 0.32f
                var y = hz * 0.2f
                moveTo(x, y)
                while (y < hz - 40 * dp) {
                    x += (r.nextFloat() - 0.45f) * 22 * dp
                    y += (14 + r.nextFloat() * 12) * dp
                    lineTo(x, y)
                }
            } else null
            val curtains = look.curtains.map { c ->
                val base = hz * c.base
                val tall = hz * c.height
                val sway = hz * c.sway
                val brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to c.top.copy(alpha = 0.55f),
                    0.9f to c.bottom,
                    1f to c.bottom.copy(alpha = 0.2f),
                    startY = base - tall - sway,
                    endY = base + sway,
                )
                val cr = Random(c.seed)
                val step = 3f * dp
                val rays = List((w / step).toInt() + 1) { i ->
                    floatArrayOf(i * step, 0.45f + 0.55f * cr.nextFloat(), 0.35f + 0.65f * cr.nextFloat())
                }
                brush to rays
            }
            return SceneShapes(fill(backPts), fill(frontPts), pines, caps, stars, puffs, bands, drops, bolt, curtains, dp)
        }
    }
}

private fun DrawScope.drawScene(s: SceneShapes, look: SceneLook, hz: Float, phase: Float, moving: Boolean) {
    val w = size.width
    val h = size.height
    val dp = s.dp
    val turn = phase * 2f * PI.toFloat()
    // The sky, top to the horizon; below it the ground covers everything.
    drawRect(Brush.verticalGradient(look.sky, startY = 0f, endY = hz))
    if (look.stars > 0f) {
        s.stars.forEach { (at, rad, a) -> drawCircle(Color.White, rad, at, alpha = a * look.stars) }
    }
    look.sun?.let { sun ->
        val c = Offset(w * sun.x, hz * sun.y)
        val rad = sun.radiusDp * dp
        drawCircle(Brush.radialGradient(listOf(sun.glow.copy(alpha = 0.55f), Color.Transparent), c, rad * 4.5f), rad * 4.5f, c)
        drawCircle(sun.color, rad, c)
    }
    look.curtains.forEachIndexed { i, cur -> drawCurtain(cur, s.curtains[i], hz, turn) }
    look.bands?.let { color ->
        s.bands.forEachIndexed { i, (at, sz) ->
            // Clouds drift a little while the sky moves.
            val drift = sin(turn + i) * 6 * dp
            drawOval(color, Offset(at.x + drift, at.y), sz, alpha = look.bandAlpha)
        }
    }
    if (s.puffs.isNotEmpty()) {
        s.puffs.forEach { (c, rad) -> drawCircle(Color(0xFFC8DCEC), rad, Offset(c.x, c.y + 3 * dp)) }
        s.puffs.forEach { (c, rad) -> drawCircle(Color(0xFFF4F8FC), rad, c) }
    }
    s.bolt?.let { bolt ->
        // A flash now and then while it moves; a still storm keeps its bolt showing.
        val flash = if (!moving) 0.85f else if (phase % 0.5f < 0.06f) 1f else 0f
        if (flash > 0f) {
            drawRect(Color(0xFFFFC777), alpha = 0.08f * flash, size = Size(w, hz))
            drawPath(bolt, Color(0xFFFFE6B0), alpha = flash, style = Stroke(2.5f * dp, cap = StrokeCap.Round))
        }
    }
    drawPath(s.back, look.back)
    s.caps?.let { drawPath(it, Color(0xFFDDE8F5), alpha = 0.85f) }
    if (look.fog) {
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF8C97AB).copy(alpha = 0.35f), Color.Transparent), hz - 60 * dp, hz),
            Offset(0f, hz - 60 * dp), Size(w, 60 * dp),
        )
    }
    drawPath(s.front, look.front)
    drawPath(s.pines, look.pines)
    if (look.rain) {
        val fall = phase * h
        s.drops.forEach { d ->
            val y = (d.y + fall) % h
            drawLine(Color(0xFF8FB0F5), Offset(d.x, y), Offset(d.x - 3 * dp, y + 14 * dp), 1.2f * dp, alpha = 0.45f)
        }
    }
    if (look.snow) {
        val fall = phase * h * 0.5f
        s.drops.forEachIndexed { i, d ->
            val y = (d.y + fall) % h
            val x = d.x + sin(turn + i) * 6 * dp
            drawCircle(Color.White, (1.2f + (i % 3) * 0.7f) * dp, Offset(x, y), alpha = 0.8f)
        }
    }
}

/** One curtain of light: thin rays standing on a wavy line, bright at the foot, fading up. */
private fun DrawScope.drawCurtain(c: SceneLook.Curtain, made: Pair<Brush, List<FloatArray>>, hz: Float, turn: Float) {
    val w = size.width
    val base = hz * c.base
    val tall = hz * c.height
    val sway = hz * c.sway
    val width = 2.4f * density
    val (brush, rays) = made
    rays.forEach { (x, height, strength) ->
        val foot = base + sway * sin(x / w * 5f + turn + c.seed)
        val ray = tall * height
        drawRect(brush, Offset(x, foot - ray), Size(width, ray), alpha = c.alpha * strength)
    }
}
