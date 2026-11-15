package com.prayergate.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayergate.app.R

/*
 * THE DESIGN SYSTEM (read this before changing any colour or font)
 *
 * The idea: fine stationery. Paper, ink, and one colour of ink for emphasis.
 * The app should feel like a well-made dictionary, not like a game.
 *
 * COLOUR - four roles, nothing else:
 *   Paper  - backgrounds. White cards sit on it with a soft shadow.
 *   Ink    - all text, and the one main button on each screen.
 *   Lapis  - the single accent. It means "yes": the right answer, a day you
 *            practised, a selected option, the streak. It is also the colour
 *            of the full stop. Use it sparingly or it loses its meaning.
 *   Citron - the spark. It only ever appears on a lapis ground (see
 *            ui/Ground.kt), where it takes over the accent's job: the right
 *            answer, the full stop, the confetti. On paper it would be
 *            unreadable, so it is never used there.
 *   There is deliberately no red and no green. A wrong answer is struck
 *   through, the way you would cross out a word on paper.
 *
 * THE FULL STOP - the app's signature:
 *   A full stop is the punctuation mark for a pause, which is what PrayerGate
 *   is. The dot appears on the question screen (the only thing that
 *   breathes), in the app icon, under the selected tab, and at the end of
 *   real sentences such as the welcome line and the verdict. Plain titles
 *   ("Settings", "Review") get no full stop: stamped on every heading it
 *   would turn into a habit instead of a meaning.
 *
 * TYPE - two families:
 *   Newsreader (serif)     - questions, headlines and numbers. Its italic is
 *                            used for small notes, like a dictionary does.
 *   Hanken Grotesk (sans)  - buttons, labels and body text.
 *   Small labels are set in capitals with wide spacing (see SectionLabel).
 */

// Paper and ink
val Paper = Color(0xFFF6F5F1)
val Ink = Color(0xFF101114)
val InkSoft = Color(0xFF6C6D72)
val Hairline = Color(0xFFE2E1DB)

// The accent on paper, and the ground of the hero screens
val Lapis = Color(0xFF1E36C0)
val LapisDeep = Color(0xFF111C7E)   // the bottom of the lapis gradient
val LapisGlow = Color(0xFF3F5BF0)   // the giant full stop in the background

// The spark: used only on top of lapis
val Citron = Color(0xFFE4F45E)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEAE9E3),
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFCFCEC8),
    outlineVariant = Hairline,
    error = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF3F2EE),
    onPrimary = Ink,
    background = Color(0xFF0D0E11),
    onBackground = Color(0xFFF3F2EE),
    surface = Color(0xFF1A1B20),
    onSurface = Color(0xFFF3F2EE),
    surfaceVariant = Color(0xFF24252B),
    onSurfaceVariant = Color(0xFF9A9BA3),
    outline = Color(0xFF3A3B43),
    outlineVariant = Color(0xFF26272D),
    error = Color(0xFFF3F2EE),
)

/**
 * The colours of anything placed on a lapis ground: white text, a white
 * main button with ink text, and white cards. Used by LapisGround.
 * It is the same in light and dark mode: lapis is already dark.
 */
val LapisColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Ink,
    background = Color(0xFF2038C8),
    onBackground = Color.White,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEAE9E3),
    onSurfaceVariant = InkSoft,
    outline = Color.White.copy(alpha = 0.45f),
    outlineVariant = Color.White.copy(alpha = 0.18f),
)

/** The accent, and the colour of text placed on top of it. */
data class AccentColors(val accent: Color, val onAccent: Color)

private val LightAccent = AccentColors(accent = Lapis, onAccent = Color.White)
private val DarkAccent = AccentColors(accent = Color(0xFF7B8FFF), onAccent = Color(0xFF0B1030))

/** On lapis the accent is citron. */
val LapisAccent = AccentColors(accent = Citron, onAccent = Ink)

val LocalAccent = staticCompositionLocalOf { LightAccent }

/*
 * The fonts are "variable": one file holds every weight. We ask each file
 * for the exact weights we need. "opsz" (optical size) picks letter shapes
 * drawn for big headlines (72) or for normal reading sizes (26).
 */
@OptIn(ExperimentalTextApi::class)
private fun garamond(weight: Int, italic: Boolean = false) = Font(
    if (italic) R.font.ebgaramond_italic else R.font.ebgaramond,
    weight = FontWeight(weight),
    style = if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun instrument(weight: Int) = Font(
    R.font.instrument_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Newsreader drawn for large sizes: headlines and the big streak number. */
val SerifDisplay = FontFamily(garamond(300), garamond(400), garamond(300, italic = true))

/** Newsreader drawn for reading sizes: the question, values, italic notes. */
val Serif = FontFamily(garamond(400), garamond(500), garamond(400, italic = true))

val Sans = FontFamily(instrument(400), instrument(500), instrument(600))

private val AppTypography = Typography(
    // The streak number
    displayLarge = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight(300), fontSize = 120.sp, lineHeight = 120.sp, letterSpacing = (-3).sp),
    // Screen titles and the verdict ("Correct.")
    headlineLarge = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight(400), fontSize = 38.sp, lineHeight = 44.sp, letterSpacing = (-0.6).sp),
    // The question itself
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight(400), fontSize = 28.sp, lineHeight = 37.sp, letterSpacing = (-0.2).sp),
    // Numbers in lists
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight(400), fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight(600), fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight(400), fontSize = 14.sp, lineHeight = 21.sp),
    // Buttons
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight(600), fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    // Small capital labels (the text is put in capitals by SectionLabel)
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight(600), fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.0.sp),
    // Italic notes, as in a dictionary
    labelSmall = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight(400), fontSize = 17.sp, lineHeight = 24.sp),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

/**
 * The same type scale, adjusted for Korean. Newsreader and Hanken Grotesk have
 * no Korean letters, so Android draws Korean with the phone's own Korean font.
 * That font has no italic (a slanted copy looks wrong), and Korean reads best
 * without the tight or wide letter spacing used for English.
 */
private val KoreanTypography = AppTypography.copy(
    displayLarge = AppTypography.displayLarge.copy(letterSpacing = 0.sp),
    headlineLarge = AppTypography.headlineLarge.copy(letterSpacing = 0.sp),
    headlineMedium = AppTypography.headlineMedium.copy(letterSpacing = 0.sp),
    labelMedium = AppTypography.labelMedium.copy(letterSpacing = 0.3.sp, fontSize = 12.5.sp, lineHeight = 18.sp),
    labelSmall = AppTypography.labelSmall.copy(fontStyle = FontStyle.Normal, fontFamily = Sans, fontSize = 15.sp, lineHeight = 22.sp),
)

/** Wrap any screen in this to give it PrayerGate's colours, fonts and shapes. */
@Composable
fun PrayerGateTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    // The app's language follows the phone (or the per-app language in Android settings).
    val korean = LocalConfiguration.current.locales[0].language == "ko"
    CompositionLocalProvider(LocalAccent provides if (dark) DarkAccent else LightAccent) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = if (korean) KoreanTypography else AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
