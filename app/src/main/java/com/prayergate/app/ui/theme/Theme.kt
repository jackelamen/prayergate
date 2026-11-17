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
 * The idea: a printed prayer book. Prayer Gate opens like a page of a
 * prayer book or Bible before you scroll: warm paper, black text, headings
 * and references printed in red (the "rubrics" of a prayer book), a cross,
 * and gilt for the moment you say Amen.
 *
 * COLOUR - four roles, nothing else:
 *   Paper  - every background. Warm, like the page of a well-used book.
 *            Cards are a slightly lighter sheet of the same paper.
 *   Ink    - all text, and the one main button on each screen.
 *   Rubric - the red of a prayer book's headings. Section labels, Bible
 *            references, the initial letter of a prayer, the cross, the page
 *            frame, selected options. It marks structure, never warnings.
 *   Gilt   - gold leaf. Only the reward: the Amen button filling as you hold
 *            it, the cross turning gold when you finish, and the days you
 *            prayed on the streak card.
 *   There is deliberately no green "success" colour.
 *
 * THE CROSS - the app's signature:
 *   A Latin cross with flared ends (crossPath in ui/Components.kt). It sits
 *   at the top of every page, breathes gently while a prayer waits, turns
 *   gold on Amen, marks the selected tab and is the app icon.
 *
 * THE PAGE (ui/Ground.kt): the moments that matter (the prayer, the welcome,
 *   the streak) are framed by a double red rule, like a printed page.
 *
 * TYPE - one family, EB Garamond, a prayer-book face:
 *   regular for the prayer and text, italic for Scripture, small capitals
 *   for headings and labels (see SectionLabel). Korean uses the phone's own
 *   Korean font, upright.
 */

// Paper and ink
val Paper = Color(0xFFF3ECDF)
val PaperSheet = Color(0xFFFBF7EE)  // cards: a lighter sheet of the same paper
val Ink = Color(0xFF1F1A14)
val InkSoft = Color(0xFF6A5F52)
val Hairline = Color(0xFFE0D5C2)

// The red of the rubrics, and gilt for the reward
val Rubric = Color(0xFFA0231C)
val Gilt = Color(0xFFC9973A)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = PaperSheet,
    background = Paper,
    onBackground = Ink,
    surface = PaperSheet,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE9E0D0),
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFCDBFA9),
    outlineVariant = Hairline,
    error = Rubric,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFEFE6D4),
    onPrimary = Color(0xFF16120E),
    background = Color(0xFF16120E),
    onBackground = Color(0xFFEFE6D4),
    surface = Color(0xFF211B15),
    onSurface = Color(0xFFEFE6D4),
    surfaceVariant = Color(0xFF2B241C),
    onSurfaceVariant = Color(0xFFA89B88),
    outline = Color(0xFF4A3F33),
    outlineVariant = Color(0xFF2E271F),
    error = Color(0xFFE07A63),
)

/** The accent, and the colour of text placed on top of it. */
data class AccentColors(val accent: Color, val onAccent: Color)

private val LightAccent = AccentColors(accent = Rubric, onAccent = PaperSheet)
private val DarkAccent = AccentColors(accent = Color(0xFFE07A63), onAccent = Color(0xFF16120E))

/** The rubric red (or its lighter night version). */
val LocalAccent = staticCompositionLocalOf { LightAccent }

/** Gilt, for the reward only. A little brighter at night. */
val LocalGilt = staticCompositionLocalOf { Gilt }

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

/** EB Garamond for large sizes: headlines and the big streak number. */
val SerifDisplay = FontFamily(garamond(400), garamond(500), garamond(400, italic = true))

/** EB Garamond for everything else. */
val Serif = FontFamily(garamond(400), garamond(500), garamond(600), garamond(400, italic = true))

/** True small capitals (EB Garamond has them), for headings and labels. */
private const val SMALL_CAPS = "c2sc, smcp"

private val AppTypography = Typography(
    // The streak number
    displayLarge = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight(400), fontSize = 120.sp, lineHeight = 120.sp, letterSpacing = (-2).sp),
    // Screen titles and "Amen"
    headlineLarge = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight(400), fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.4).sp),
    // The prayer itself
    headlineMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight(400), fontSize = 27.sp, lineHeight = 37.sp, letterSpacing = 0.sp),
    // Numbers in lists
    titleLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight(400), fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight(600), fontSize = 18.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight(400), fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight(400), fontSize = 16.sp, lineHeight = 22.sp),
    // Buttons
    labelLarge = TextStyle(fontFamily = Serif, fontWeight = FontWeight(600), fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = 0.2.sp),
    // Rubrics: headings and labels in red small capitals (see SectionLabel)
    labelMedium = TextStyle(fontFamily = Serif, fontWeight = FontWeight(500), fontSize = 16.5.sp, lineHeight = 21.sp, letterSpacing = 1.3.sp, fontFeatureSettings = SMALL_CAPS),
    // Scripture, notes and references, in italic
    labelSmall = TextStyle(fontFamily = Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight(400), fontSize = 18.sp, lineHeight = 25.sp),
)

// Gentle corners: a book's pages and cards, not soft app bubbles.
private val AppShapes = Shapes(
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
)

/**
 * The same type scale, adjusted for Korean. EB Garamond has no Korean letters, so Android draws Korean with the phone's own Korean font.
 * That font has no italic (a slanted copy looks wrong), and Korean reads best
 * without the tight or wide letter spacing used for English.
 */
private val KoreanTypography = AppTypography.copy(
    displayLarge = AppTypography.displayLarge.copy(letterSpacing = 0.sp),
    headlineLarge = AppTypography.headlineLarge.copy(letterSpacing = 0.sp),
    // Korean sentences are longer on screen than the English, so the prayer is a little smaller.
    headlineMedium = AppTypography.headlineMedium.copy(letterSpacing = 0.sp, fontSize = 23.sp, lineHeight = 36.sp),
    labelMedium = AppTypography.labelMedium.copy(letterSpacing = 0.3.sp, fontSize = 14.sp, lineHeight = 20.sp),
    labelSmall = AppTypography.labelSmall.copy(fontStyle = FontStyle.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    bodyLarge = AppTypography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 25.sp),
    bodyMedium = AppTypography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = AppTypography.labelLarge.copy(fontSize = 15.sp, letterSpacing = 0.sp),
)

/** Wrap any screen in this to give it Prayer Gate's colours, fonts and shapes. */
@Composable
fun PrayerGateTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    // The app's language follows the phone (or the per-app language in Android settings).
    val korean = LocalConfiguration.current.locales[0].language == "ko"
    CompositionLocalProvider(
        LocalAccent provides if (dark) DarkAccent else LightAccent,
        LocalGilt provides if (dark) Color(0xFFD9AE4E) else Gilt,
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = if (korean) KoreanTypography else AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
