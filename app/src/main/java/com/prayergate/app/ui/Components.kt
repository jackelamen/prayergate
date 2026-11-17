package com.prayergate.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayergate.app.R
import com.prayergate.app.data.PrayerTheme
import com.prayergate.app.ui.theme.LocalAccent
import com.prayergate.app.ui.theme.LocalGilt

/*
 * Small building blocks shared by several screens.
 */

/**
 * Keeps a screen's content at a comfortable reading width.
 * On phones it fills the width; on wide screens (an unfolded Galaxy Z Fold,
 * a tablet, landscape) it stops at 560dp and sits in the middle, instead of
 * stretching lines and buttons across the whole display.
 */
fun Modifier.readableWidth(maxWidth: Dp = 560.dp): Modifier =
    this.fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = maxWidth)

/**
 * One line of text that never wraps. If the words do not fit (a narrow screen
 * such as the Galaxy Z Fold cover screen, or a large system font size), the
 * text shrinks a little, down to [minFontSize], instead of breaking a word
 * in two ("Vocabular / y").
 */
@Composable
fun FitText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    minFontSize: TextUnit = 10.sp,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val maxWidthPx = constraints.maxWidth
        // Try the normal size first, then step down half a point at a time
        // until one line of text fits the available width.
        val fitted = remember(text, style, maxWidthPx) {
            var size = style.fontSize.value
            while (size > minFontSize.value) {
                val width = measurer.measure(text, style.copy(fontSize = size.sp), maxLines = 1, softWrap = false).size.width
                if (width <= maxWidthPx) break
                size -= 0.5f
            }
            style.copy(fontSize = size.sp)
        }
        Text(text, style = fitted, color = color, textAlign = textAlign, maxLines = 1, softWrap = false)
    }
}

/** Makes anything shrink a little while it is pressed, then spring back. */
@Composable
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this.scale(scale)
}

/**
 * A lighter sheet of paper laid on the page: a hairline edge and the
 * faintest shadow. Cards are how Prayer Gate groups things.
 */
@Composable
fun Modifier.card(shape: Shape = MaterialTheme.shapes.medium, elevation: Dp = 2.dp): Modifier =
    this
        .shadow(elevation, shape, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
        .background(MaterialTheme.colorScheme.surface, shape)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)

/** The main action on a screen: a solid ink button. Use at most one per screen. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).pressScale(interaction),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** A second, quieter action: plain text with no box around it. */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
    }
}

/** The name of a prayer theme, e.g. "Temptation". */
@StringRes
fun themeName(theme: PrayerTheme): Int = when (theme) {
    PrayerTheme.TEMPTATION -> R.string.theme_temptation
    PrayerTheme.JUDGING -> R.string.theme_judging
    PrayerTheme.NEIGHBOR -> R.string.theme_neighbor
    PrayerTheme.INFLUENCE -> R.string.theme_influence
    PrayerTheme.DISCERNMENT -> R.string.theme_discernment
    PrayerTheme.COMPARISON -> R.string.theme_comparison
    PrayerTheme.WORDS -> R.string.theme_words
    PrayerTheme.TIME -> R.string.theme_time
}

/** Text style for Korean sentences. */
@Composable
fun koreanStyle(): TextStyle = MaterialTheme.typography.bodyMedium

/**
 * Android may wrap Korean text in the middle of a word (for example "해" at the
 * end of one line and "요." on the next). Korean puts spaces between words, so
 * we glue the letters of each word together with an invisible "word joiner"
 * character. Lines can then only break at the spaces.
 */
fun keepKoreanWordsTogether(text: String): String =
    text.split(' ').joinToString(" ") { word -> word.toList().joinToString("\u2060") }

/**
 * A rubric: a small heading in red small capitals, e.g. "THEMES", as a
 * prayer book prints its headings.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalAccent.current.accent,
    maxLines: Int = 1,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * "Pick one of these" control, used all over Settings.
 * A white thumb sits on a grey track under the selected option.
 */
@Composable
fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (option in options) {
            val isSelected = option == selected
            val thumb by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                label = "choiceThumb",
            )
            val textColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "choiceText",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .then(if (isSelected) Modifier.shadow(2.dp, RoundedCornerShape(7.dp)) else Modifier)
                    .clip(RoundedCornerShape(7.dp))
                    .background(thumb)
                    .clickable(role = Role.RadioButton) { onSelect(option) }
                    .heightIn(min = 46.dp)
                    .padding(horizontal = 6.dp, vertical = 10.dp),
            ) {
                FitText(label(option), MaterialTheme.typography.labelLarge, textColor)
            }
        }
    }
}

/**
 * The outline of Prayer Gate's cross: a Latin cross (the lower arm is the
 * longest) whose arms widen a little towards their ends, as crosses do in
 * old prayer books. The long lower arm keeps it from reading as the equal-armed
 * red cross of first aid. It fits a box [height] tall and 0.68 x [height]
 * wide, with its top-left corner at [left], [top].
 *
 * tools/make_icons.py draws the launcher, notification and widget icons from
 * the same numbers: change both together.
 */
fun crossPath(left: Float, top: Float, height: Float): Path {
    val h = height
    val w = h * 0.68f
    val cx = left + w / 2
    val cy = top + h * 0.32f      // the middle of the crossbar
    val b = h * 0.075f            // half the thickness where the arms meet
    val a = h * 0.115f            // half the thickness at the end of each arm
    return Path().apply {
        moveTo(cx - a, top); lineTo(cx + a, top)                 // top arm
        lineTo(cx + b, cy - b)
        lineTo(left + w, cy - a); lineTo(left + w, cy + a)       // right arm
        lineTo(cx + b, cy + b)
        lineTo(cx + a, top + h); lineTo(cx - a, top + h)         // lower arm
        lineTo(cx - b, cy + b)
        lineTo(left, cy + a); lineTo(left, cy - a)               // left arm
        lineTo(cx - b, cy - b)
        close()
    }
}

/**
 * The cross: Prayer Gate's signature, in rubric red.
 *
 * With [breathing] it fades softly in and out, like a calm breath.
 * When [celebrate] becomes true (the user said Amen) the cross turns to gilt
 * and a wide gold glow opens around it once and fades.
 */
@Composable
fun Cross(modifier: Modifier = Modifier, breathing: Boolean = false, celebrate: Boolean = false, size: Dp = 28.dp) {
    val red = LocalAccent.current.accent
    val gilt = LocalGilt.current

    // Breathing: a number that drifts between 0 and 1 forever.
    val transition = rememberInfiniteTransition(label = "breathing")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )

    // Celebration: the cross turns gold, and a glow opens around it once.
    val gold = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    LaunchedEffect(celebrate) {
        if (celebrate) gold.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) else gold.snapTo(0f)
    }
    LaunchedEffect(celebrate) {
        if (celebrate) glow.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) else glow.snapTo(0f)
    }

    Canvas(modifier.size(size)) {
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val t = glow.value
        if (t > 0f && t < 1f) {
            drawCircle(gilt, radius = this.size.minDimension * (0.3f + 1.1f * t), center = center, alpha = 0.4f * (1f - t))
        }
        val h = this.size.minDimension * 0.78f
        val alpha = if (breathing) 1f - 0.35f * breath else 1f
        drawPath(crossPath(center.x - h * 0.34f, center.y - h / 2, h), lerp(red, gilt, gold.value), alpha = alpha)
    }
}
