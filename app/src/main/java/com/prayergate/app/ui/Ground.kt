package com.prayergate.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prayergate.app.ui.theme.LocalAccent

/*
 * THE PAGE: where Prayer Gate shows its character.
 *
 * The moments that matter (the prayer, the welcome, the streak) are set like
 * a page of a printed prayer book: the paper, framed by a double rule in
 * rubric red, a heavier line outside and a fine line inside. The frame is
 * the only decoration. It does not move.
 */

/**
 * Paper with a double red rule around it.
 *
 * @param fullScreen true when this fills the window: the frame then stays
 *                   clear of the status bar and navigation bar. Use false for
 *                   a card (the streak card on Stats).
 */
@Composable
fun PageGround(
    modifier: Modifier = Modifier,
    fullScreen: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val rubric = LocalAccent.current.accent
    Box(modifier.background(if (fullScreen) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface)) {
        Canvas(
            Modifier
                .matchParentSize()
                .then(if (fullScreen) Modifier.systemBarsPadding() else Modifier)
                .padding(if (fullScreen) 12.dp else 8.dp),
        ) {
            doubleRule(rubric, gap = 4.dp)
        }
        content()
    }
}

/** A heavier outer rule and a fine inner rule, [gap] apart. */
private fun DrawScope.doubleRule(color: Color, gap: Dp) {
    val outer = 1.3.dp.toPx()
    val inner = 0.6.dp.toPx()
    val g = gap.toPx()
    drawRect(
        color,
        topLeft = Offset(outer / 2, outer / 2),
        size = Size(size.width - outer, size.height - outer),
        style = Stroke(outer),
    )
    drawRect(
        color.copy(alpha = 0.8f),
        topLeft = Offset(outer + g, outer + g),
        size = Size(size.width - 2 * (outer + g), size.height - 2 * (outer + g)),
        style = Stroke(inner),
    )
}
