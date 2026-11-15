package com.prayergate.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.prayergate.app.ui.theme.LocalAccent
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** One piece of confetti. Everything about it is decided once, at the start. */
private class Piece(
    val angle: Float,      // direction it flies in (radians)
    val speed: Float,      // how far it flies (dp)
    val size: Float,       // dp
    val color: Color,
    val spin: Float,       // total rotation in degrees
    val isRound: Boolean,
)

/**
 * A burst of confetti that flies out of [origin] once, then disappears.
 *
 * How it works: a single number, "progress", animates from 0 to 1. Every
 * frame, each piece works out where it should be for that progress:
 * it shoots outwards fast, slows down, and gravity pulls it down.
 *
 * @param burstKey change this value to fire a new burst.
 * @param origin   where the burst starts, in pixels inside this composable.
 */
@Composable
fun ConfettiBurst(burstKey: Any?, origin: Offset, modifier: Modifier = Modifier) {
    if (burstKey == null) return

    // One colour family only: the accent in three strengths, plus ink.
    // A single-colour burst looks designed; a rainbow looks like a template.
    val accent = LocalAccent.current.accent
    val ink = MaterialTheme.colorScheme.onBackground
    val colors = listOf(accent, accent, accent.copy(alpha = 0.55f), accent.copy(alpha = 0.3f), ink)
    val pieces = remember(burstKey, colors) {
        val random = Random(System.nanoTime())
        List(PIECE_COUNT) {
            Piece(
                // Mostly upwards (up is -90 degrees), spread out to both sides.
                angle = Math.toRadians(-90.0 + random.nextDouble(-80.0, 80.0)).toFloat(),
                speed = random.nextFloat() * 260f + 120f,
                size = random.nextFloat() * 5f + 5f,
                color = colors[random.nextInt(colors.size)],
                spin = random.nextFloat() * 720f - 360f,
                isRound = random.nextInt(3) == 0,
            )
        }
    }

    val progress = remember(burstKey) { Animatable(0f) }
    LaunchedEffect(burstKey) {
        progress.animateTo(1f, tween(durationMillis = 1600, easing = LinearEasing))
    }

    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas

        // "Ease out": pieces start fast and slow down, like a real pop.
        val outward = 1f - (1f - t) * (1f - t) * (1f - t)
        // Gravity grows with time squared.
        val fall = 520.dp.toPx() * t * t
        // Fade away over the last 30% of the animation.
        val alpha = if (t < 0.7f) 1f else (1f - t) / 0.3f

        for (piece in pieces) {
            val distance = piece.speed.dp.toPx() * outward
            val x = origin.x + cos(piece.angle) * distance
            val y = origin.y + sin(piece.angle) * distance + fall
            val size = piece.size.dp.toPx()
            if (piece.isRound) {
                drawCircle(piece.color, radius = size / 2, center = Offset(x, y), alpha = alpha)
            } else {
                rotate(degrees = piece.spin * t, pivot = Offset(x, y)) {
                    drawRect(
                        color = piece.color,
                        topLeft = Offset(x - size / 2, y - size / 4),
                        size = Size(size, size / 2),
                        alpha = alpha,
                    )
                }
            }
        }
    }
}

private const val PIECE_COUNT = 90
