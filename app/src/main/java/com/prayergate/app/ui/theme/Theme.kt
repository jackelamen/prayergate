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
 * The idea: "Watch and pray" (Matthew 26:41), said in the olive garden of
 * Gethsemane. PrayerGate is a small lamp kept lit before you scroll. The one
 * feeling it should leave is watchful calm: steady, quiet, unhurried.
 *
 * COLOUR - four roles, nothing else:
 *   Paper  - backgrounds of the tool screens (Settings, Apps, Saved). A cool
 *            grey-green, not cream. White cards sit on it with a soft shadow.
 *   Ink    - all text, and the one main button on each screen.
 *   Olive  - the single accent on paper. It means "yes": a day you prayed, a
 *            selected option, the streak. Use it sparingly.
 *   Gold   - lamp oil. It only ever appears on the olive ground (see
 *            ui/Ground.kt), and only as the reward: the Amen button filling
 *            as you hold it, and the flame's glow when you finish. On paper it
 *            would be unreadable, so it is never used there.
 *   There is deliberately no red and no green "success" colour.
 *
 * THE FLAME - the app's signature:
 *   A small lamp flame. It breathes in the header of the prayer screen (the
 *   only thing that moves there), glows gold when you say Amen, marks the
 *   selected tab and is the app icon. One mark, used with meaning, not
 *   stamped on every heading.
 *
 * TYPE - two families:
 *   EB Garamond (serif)      - the prayer, headlines and numbers. A prayer-book
 *                              face. Its italic is for references and notes.
 *   Instrument Sans (sans)   - buttons, labels and body text.
 *   Small labels are set in capitals with wide spacing (see SectionLabel).
 */

// Paper and ink
val Paper = Color(0xFFF2F3EF)
val Ink = Color(0xFF151A14)
val InkSoft = Color(0xFF656B62)
val Hairline = Color(0xFFDDE0D8)

// The accent on paper, and the ground of the hero screens
val Olive = Color(0xFF3F5A2C)
val OliveDeep = Color(0xFF1C2718)   // the bottom of the olive gradient
val OliveGlow = Color(0xFF55703F)   // the large flame in the background

// The reward: used only on top of olive
val Gold = Color(0xFFE8B84A)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE5E8E1),
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFC9CDC3),
    outlineVariant = Hairline,
    error = Ink,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF0F2EC),
    onPrimary = Ink,
    background = Color(0xFF0E120D),
    onBackground = Color(0xFFF0F2EC),
    surface = Color(0xFF1A2018),
    onSurface = Color(0xFFF0F2EC),
    surfaceVariant = Color(0xFF242B22),
    onSurfaceVariant = Color(0xFF9AA294),
    outline = Color(0xFF3A4337),
    outlineVariant = Color(0xFF262D24),
    error = Color(0xFFF0F2EC),
)

/**
 * The colours of anything placed on the olive ground: white text, a white
 * main button with ink text, and white cards. Used by OliveGround.
 * It is the same in light and dark mode: olive is already dark.
 */
val OliveColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Ink,
    background = Color(0xFF34482A),
    onBackground = Color.White,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE5E8E1),
    onSurfaceVariant = InkSoft,
    outline = Color.White.copy(alpha = 0.45f),
    outlineVariant = Color.White.copy(alpha = 0.18f),
)

/** The accent, and the colour of text placed on top of it. */
data class AccentColors(val accent: Color, val onAccent: Color)

private val LightAccent = AccentColors(accent = Olive, onAccent = Color.White)
private val DarkAccent = AccentColors(accent = Color(0xFFA9C48C), onAccent = Color(0xFF142010))

/** On the olive ground the accent is gold. */
val OliveAccent = AccentColors(accent = Gold, onAccent = Ink)

val LocalAccent = staticCompositionLocalOf { LightAccent }

/*
 * The fonts are "variable": one file holds every weight. We ask each file
 * for the exact weights we need.
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

/** EB Garamond for large sizes: headlines and the big streak number. */
val SerifDisplay = FontFamily(garamond(400), garamond(500), garamond(400, italic = true))

/** EB Garamond for reading sizes: the prayer, values, italic notes. */
val Serif = FontFamily(garamond(400), garamond(500), garamond(400, italic = true))

val Sans = FontFamily(instrument(400), instrument(500), instrument(600))

private val AppTypography = Typography(
    // The streak number
    displayLarge = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight(400), fontSize = 120.sp, lineHeight = 120.sp, letterSpacing = (-2).sp),
    // Screen titles and "Amen"
    headlineLarge = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight(400), fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.4).sp),
    // The prayer itself
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight(400), fontSize = 27.sp, lineHeight = 37.sp, letterSpacing = 0.sp),
    // Numbers in lists
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight(400), fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight(600), fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight(400), fontSize = 14.sp, lineHeight = 21.sp),
    // Buttons
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight(600), fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    // Small capital labels (the text is put in capitals by SectionLabel)
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight(600), fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.0.sp),
    // Italic notes and Bible references
    labelSmall = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight(400), fontSize = 18.sp, lineHeight = 25.sp),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
)

/**
 * The same type scale, adjusted for Korean. EB Garamond and Instrument Sans have
 * no Korean letters, so Android draws Korean with the phone's own Korean font.
 * That font has no italic (a slanted copy looks wrong), and Korean reads best
 * without the tight or wide letter spacing used for English.
 */
private val KoreanTypography = AppTypography.copy(
    displayLarge = AppTypography.displayLarge.copy(letterSpacing = 0.sp),
    headlineLarge = AppTypography.headlineLarge.copy(letterSpacing = 0.sp),
    // Korean sentences are longer on screen than the English, so the prayer is a little smaller.
    headlineMedium = AppTypography.headlineMedium.copy(letterSpacing = 0.sp, fontSize = 23.sp, lineHeight = 36.sp),
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
