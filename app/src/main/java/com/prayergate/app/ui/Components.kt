package com.prayergate.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayergate.app.ui.theme.LocalAccent
import kotlin.math.cos
import kotlin.math.sin

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
 * A white card lifted off the paper by a very soft shadow.
 * Cards are how PrayerGate groups things; there are no outlined boxes.
 */
@Composable
fun Modifier.card(shape: Shape = MaterialTheme.shapes.medium, elevation: Dp = 10.dp): Modifier =
    this
        .shadow(elevation, shape, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.10f))
        .background(MaterialTheme.colorScheme.surface, shape)

/** The main action on a screen: a solid ink pill. Use at most one per screen. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = CircleShape,
        modifier = modifier.fillMaxWidth().heightIn(min = 58.dp).pressScale(interaction),
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
        shape = CircleShape,
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

/** A small label in spaced capitals, e.g. "DIFFICULTY". */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
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
 * Returns [text] with its final full stop (or "!") coloured in the accent.
 * This is the app's signature, used on the big headlines.
 */
@Composable
fun withAccentFullStop(text: String): AnnotatedString {
    val accent = LocalAccent.current.accent
    // The final full stop, or the "!" of short Korean feedback such as "정답!".
    val mark = text.lastOrNull()
    if (mark != '.' && mark != '!') return AnnotatedString(text)
    return buildAnnotatedString {
        // The invisible word joiner (U+2060) keeps the coloured dot on the same
        // line as the last word. Without it, Android may treat the colour change
        // as a place to break the line, and the dot falls onto a line of its own.
        append(text.dropLast(1))
        append("\u2060")
        withStyle(SpanStyle(color = accent)) { append(mark.toString()) }
    }
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
            .clip(RoundedCornerShape(16.dp))
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
                    .then(if (isSelected) Modifier.shadow(3.dp, RoundedCornerShape(12.dp)) else Modifier)
                    .clip(RoundedCornerShape(12.dp))
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
 * The lapis full stop: PrayerGate's signature, and the sign for "pause".
 *
 * With [breathing] it swells and settles slowly, like a calm breath.
 * When [celebrate] becomes true it jumps and throws out a ring of fine rays.
 */
@Composable
fun FullStop(modifier: Modifier = Modifier, breathing: Boolean = false, celebrate: Boolean = false) {
    val accent = LocalAccent.current.accent

    // Breathing: a number that drifts between 0 and 1 forever.
    val transition = rememberInfiniteTransition(label = "breathing")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )

    // Celebration: the dot pops, and rays fly outwards once.
    val pop = remember { Animatable(1f) }
    val rays = remember { Animatable(0f) }
    LaunchedEffect(celebrate) {
        if (celebrate) {
            pop.animateTo(1.9f, tween(120))
            pop.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    LaunchedEffect(celebrate) {
        if (celebrate) rays.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    Canvas(modifier.size(28.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = 4.5.dp.toPx() * pop.value

        if (breathing) {
            // A soft halo that grows and fades with each breath.
            drawCircle(accent, radius = radius + 3.dp.toPx() + 6.dp.toPx() * breath, center = center, alpha = 0.20f - 0.14f * breath)
        }
        drawCircle(accent, radius = radius, center = center)

        // The rays: twelve fine lines that travel outwards and fade.
        val t = rays.value
        if (t > 0f && t < 1f) {
            val start = 10.dp.toPx() + 14.dp.toPx() * t
            val length = 8.dp.toPx() * (1f - t)
            for (i in 0 until 12) {
                val angle = Math.toRadians(i * 30.0).toFloat()
                val direction = Offset(cos(angle), sin(angle))
                drawLine(
                    color = accent,
                    start = center + direction * start,
                    end = center + direction * (start + length),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round,
                    alpha = 1f - t,
                )
            }
        }
    }
}
