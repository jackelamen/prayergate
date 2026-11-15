package com.prayergate.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prayergate.app.R
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.data.AppWeek
import com.prayergate.app.data.prayedDays
import com.prayergate.app.data.weekByApp
import com.prayergate.app.data.computeStats
import com.prayergate.app.ui.theme.Olive
import com.prayergate.app.ui.theme.LocalAccent
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * The Stats screen. The streak is the hero: one very large, light numeral.
 * Everything else is a quiet ledger underneath it.
 * All numbers are worked out from the pause log (see data/Stats.kt).
 */
@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val dao = (context.applicationContext as PrayerGateApp).database.triggerLogDao()
    val accent = LocalAccent.current.accent

    // null until the database has answered, so we never flash "0" by mistake.
    val logs by dao.observeAll().collectAsStateWithLifecycle(initialValue = null)
    val loaded = logs ?: return

    val stats = computeStats(loaded)
    val days = prayedDays(loaded)

    // Numbers count up from zero when the screen opens.
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .readableWidth()
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(36.dp))
        Text(
            stringResource(R.string.stats_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        if (stats.prayed == 0 && stats.closes == 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.stats_empty),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        // --- The hero: the daily streak, on an olive card ---
        Spacer(Modifier.height(24.dp))
        val streak by animateIntAsState(
            if (started) stats.streakDays else 0, tween(1000, easing = FastOutSlowInEasing), label = "streak",
        )
        OliveGround(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(18.dp, MaterialTheme.shapes.large, ambientColor = Olive, spotColor = Olive)
                .clip(MaterialTheme.shapes.large),
            fullScreen = false,
        ) {
            Column(Modifier.padding(start = 26.dp, end = 26.dp, top = 26.dp, bottom = 28.dp)) {
                Text(
                    streak.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                SectionLabel(
                    pluralStringResource(R.plurals.stats_streak_label, stats.streakDays),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(28.dp))
                WeekDots(days)
            }
        }
        if (stats.streakDays == 0) {
            Text(
                stringResource(R.string.stats_streak_start),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp),
            )
        }

        // --- The ledger ---
        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val week by animateIntAsState(if (started) stats.prayedThisWeek else 0, tween(900), label = "week")
        StatRow(stringResource(R.string.stats_prayed_week), week.toString())

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val prayed by animateIntAsState(if (started) stats.prayed else 0, tween(900), label = "prayed")
        StatRow(stringResource(R.string.stats_prayed_total), prayed.toString())

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val closes by animateIntAsState(if (started) stats.closes else 0, tween(900), label = "closes")
        StatRow(stringResource(R.string.stats_closes), closes.toString())

        // --- This week, app by app ---
        val apps = weekByApp(loaded, System.currentTimeMillis())
        if (apps.isNotEmpty()) {
            Spacer(Modifier.height(36.dp))
            SectionLabel(stringResource(R.string.stats_by_app))
            Spacer(Modifier.height(4.dp))
            for (week in apps) AppWeekRow(week)
        }
        Spacer(Modifier.height(32.dp))
    }
}

/**
 * The last seven days, shown on the olive card. A filled gold dot = prayed
 * that day; an empty ring = no prayer. The dots appear one after another.
 */
@Composable
private fun WeekDots(activeDays: Set<LocalDate>) {
    val accent = LocalAccent.current.accent
    val today = LocalDate.now()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        for (daysAgo in 6 downTo 0) {
            val day = today.minusDays(daysAgo.toLong())
            val active = day in activeDays
            val scale by animateFloatAsState(
                targetValue = if (shown) 1f else 0f,
                animationSpec = tween(durationMillis = 360, delayMillis = 300 + (6 - daysAgo) * 70),
                label = "dot",
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(14.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .then(
                            if (active) Modifier.background(accent, CircleShape)
                            else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        ),
                )
                Spacer(Modifier.height(10.dp))
                // One-letter day name, in the phone's language. Today is in ink.
                SectionLabel(
                    day.dayOfWeek.getDisplayName(TextStyle.NARROW, LocalConfiguration.current.locales[0]),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (daysAgo == 0) 1f else 0.6f),
                )
            }
        }
    }
}

/** The name and icon of an installed app, or the package name if it was uninstalled. */
@Composable
private fun rememberAppInfo(packageName: String): Pair<String, ImageBitmap?> {
    val context = LocalContext.current
    return remember(packageName) {
        try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString() to pm.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap()
        } catch (e: Exception) {
            packageName to null
        }
    }
}

/**
 * One app's week: icon, name, and "closed 9 of 23". The fine line underneath
 * fills to the share of pauses where the user chose Close.
 */
@Composable
private fun AppWeekRow(week: AppWeek) {
    val (label, icon) = rememberAppInfo(week.packageName)
    val accent = LocalAccent.current.accent
    val share = if (week.pauses == 0) 0f else week.closes.toFloat() / week.pauses
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val fill by animateFloatAsState(if (started) share else 0f, tween(900, easing = FastOutSlowInEasing), label = "appShare")

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                pluralStringResource(R.plurals.stats_app_closed, week.pauses, week.closes, week.pauses),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.outlineVariant)) {
            Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(accent))
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}
