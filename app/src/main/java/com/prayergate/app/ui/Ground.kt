package com.prayergate.app.ui

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.prayergate.app.ui.theme.LapisAccent
import com.prayergate.app.ui.theme.LapisColors
import com.prayergate.app.ui.theme.LapisDeep
import com.prayergate.app.ui.theme.LapisGlow
import com.prayergate.app.ui.theme.LocalAccent
import com.prayergate.app.ui.theme.SerifDisplay

/*
 * THE LAPIS GROUND: where PrayerGate shows its character.
 *
 * Most screens are quiet paper. The moments that matter (the question, the
 * welcome, the streak) sit on a deep lapis blue instead, with two pieces of
 * typographic art behind the content:
 *
 *   1. A giant full stop, so big that only part of it fits on the screen.
 *      It is the app's signature dot, scaled up. It does not move.
 *   2. One large, pale italic word (see GhostWord). It is always shown
 *      whole, lined up with the text, and never sits behind other text.
 *
 * Both are only slightly lighter than the background, so they add depth
 * without competing with the text in front of them.
 */

/**
 * Draws the lapis background art and switches the colours of everything
 * inside it: text becomes white, the main button becomes white, and the
 * accent becomes citron (lapis on lapis would be invisible).
 *
 * @param word        a large pale word drawn near the top, or null for none.
 *                    Leave the area under it empty.
 * @param fullScreen  true when this fills the whole window (it then also makes
 *                    the status bar icons light). Use false for a card.
 */
@Composable
fun LapisGround(
    modifier: Modifier = Modifier,
    word: String? = null,
    wordTop: Dp = 84.dp,
    fullScreen: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    if (fullScreen) LightStatusBarIcons()

    Box(
        modifier
            .clipToBounds()
            // A gentle fall-off from blue to a deeper blue gives the flat colour some depth.
            .background(Brush.verticalGradient(listOf(LapisColors.background, LapisDeep))),
    ) {
        Canvas(Modifier.matchParentSize()) {
            // The giant full stop sits low on the right, mostly off the screen.
            // It stays still: only the small dot in the header breathes, so there is one calm motion, not two.
            val radius = size.width * 0.63f
            val center = Offset(size.width * 1.02f, size.height - size.width * 0.18f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LapisGlow.copy(alpha = 0.55f), LapisGlow.copy(alpha = 0.16f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        if (word != null) {
            GhostWord(
                text = word,
                fontSize = 128.sp,
                // Lined up with the left edge of the text in front of it.
                modifier = Modifier.align(Alignment.TopStart).offset(x = 22.dp, y = wordTop),
            )
        }

        // Everything in front uses the "on lapis" colours.
        CompositionLocalProvider(LocalAccent provides LapisAccent) {
            MaterialTheme(colorScheme = LapisColors, content = { content() })
        }
    }
}

/**
 * A large, pale, light italic word used as a piece of typography rather
 * than as something to read closely. Only use it on a lapis ground.
 */
@Composable
fun GhostWord(text: String, fontSize: TextUnit, modifier: Modifier = Modifier, fitWidth: Boolean = false) {
    if (fitWidth) {
        // Shrink the word if it would not fit the screen width (narrow phones,
        // or a large system font size). It is decoration, so it should never
        // push against the edge.
        BoxWithConstraints(modifier) {
            val density = LocalDensity.current
            val maxSize = with(density) { maxWidth.toSp() } / (text.length * 0.47f).coerceAtLeast(1f)
            val size = if (maxSize.value < fontSize.value) maxSize else fontSize
            GhostWord(text, size)
        }
        return
    }
    Text(
        text = text,
        style = TextStyle(
            fontFamily = SerifDisplay,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight(300),
            fontSize = fontSize,
            lineHeight = fontSize,
            letterSpacing = (-0.03).em,
        ),
        color = Color.White.copy(alpha = 0.3f),
        maxLines = 1,
        softWrap = false,
        modifier = modifier,
    )
}

/**
 * Makes the status bar icons (clock, battery) light while a full-screen
 * lapis ground is showing, and puts them back afterwards.
 */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val bars = window?.let { WindowCompat.getInsetsController(it, view) }
        bars?.isAppearanceLightStatusBars = false
        onDispose { bars?.isAppearanceLightStatusBars = !darkTheme }
    }
}
