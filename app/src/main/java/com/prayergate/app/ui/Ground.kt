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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
import com.prayergate.app.ui.theme.OliveAccent
import com.prayergate.app.ui.theme.OliveColors
import com.prayergate.app.ui.theme.OliveDeep
import com.prayergate.app.ui.theme.OliveGlow
import com.prayergate.app.ui.theme.LocalAccent
import com.prayergate.app.ui.theme.SerifDisplay

/*
 * THE OLIVE GROUND: where PrayerGate shows its character.
 *
 * Most screens are quiet paper. The moments that matter (the prayer, the
 * welcome, the streak) sit on a deep olive instead, with two pieces of art
 * behind the content:
 *
 *   1. A giant flame, so big that only its upper part fits on the screen.
 *      It is the app's signature mark, scaled up. It does not move.
 *   2. One large, pale italic word (see GhostWord). It is always shown
 *      whole, lined up with the text, and never sits behind other text.
 *
 * Both are only slightly lighter than the background, so they add depth
 * without competing with the text in front of them.
 */

/**
 * Draws the olive background art and switches the colours of everything
 * inside it: text becomes white, the main button becomes white, and the
 * accent becomes gold (olive on olive would be invisible).
 *
 * @param word        a large pale word drawn near the top, or null for none.
 *                    Leave the area under it empty.
 * @param fullScreen  true when this fills the whole window (it then also makes
 *                    the status bar icons light). Use false for a card.
 */
@Composable
fun OliveGround(
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
            // A gentle fall-off from olive to a deeper olive gives the flat colour some depth.
            .background(Brush.verticalGradient(listOf(OliveColors.background, OliveDeep))),
    ) {
        Canvas(Modifier.matchParentSize()) {
            // The giant flame rises from below the bottom edge, on the right.
            // Its tip and upper body show; its base is cut off by the edge, so
            // it reads as a lamp flame seen close up. It stays still: only the
            // small flame in the header breathes, so there is one calm motion.
            // Its size is capped by the height, so the tip shows on short
            // grounds too (a card, a phone turned sideways).
            val width = minOf(size.width * 0.8f, size.height * 0.55f)
            val left = size.width - width * 0.85f
            val top = size.height - width * 1.5f * 0.7f
            drawPath(
                path = flamePath(left, top, width),
                brush = Brush.verticalGradient(
                    colors = listOf(OliveGlow.copy(alpha = 0.55f), OliveGlow.copy(alpha = 0.12f)),
                    startY = top,
                    endY = top + width * 1.5f,
                ),
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

        // Everything in front uses the "on olive" colours.
        CompositionLocalProvider(LocalAccent provides OliveAccent) {
            MaterialTheme(colorScheme = OliveColors, content = { content() })
        }
    }
}

/**
 * A large, pale, light italic word used as a piece of typography rather
 * than as something to read closely. Only use it on the olive ground.
 */
@Composable
fun GhostWord(text: String, fontSize: TextUnit, modifier: Modifier = Modifier, fitWidth: Boolean = false) {
    if (fitWidth) {
        // Shrink the word if it would not fit the screen width (narrow phones,
        // or a large system font size). It is decoration, so it should never
        // push against the edge.
        BoxWithConstraints(modifier) {
            val density = LocalDensity.current
            val perChar = if (text.any { it in '\uAC00'..'\uD7A3' }) 0.95f else 0.47f
            val maxSize = with(density) { maxWidth.toSp() } / (text.length * perChar).coerceAtLeast(1f)
            val size = if (maxSize.value < fontSize.value) maxSize else fontSize
            GhostWord(text, size)
        }
        return
    }
    // Korean has no true italic (Android would slant the letters), so Korean
    // stays upright. Upright Hangul is much heavier than a light Latin italic,
    // so it is drawn smaller to stay in the background.
    val korean = LocalConfiguration.current.locales[0].language == "ko"
    val size = if (korean) fontSize * 0.62f else fontSize
    Text(
        text = text,
        style = TextStyle(
            fontFamily = SerifDisplay,
            fontStyle = if (korean) FontStyle.Normal else FontStyle.Italic,
            fontWeight = FontWeight(300),
            fontSize = size,
            lineHeight = size,
            letterSpacing = if (korean) 0.em else (-0.03).em,
        ),
        color = Color.White.copy(alpha = 0.3f),
        maxLines = 1,
        softWrap = false,
        modifier = modifier,
    )
}

/**
 * Makes the status bar icons (clock, battery) light while a full-screen
 * olive ground is showing, and puts them back afterwards.
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
