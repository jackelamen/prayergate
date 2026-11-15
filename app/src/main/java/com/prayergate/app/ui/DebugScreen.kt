package com.prayergate.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prayergate.app.R
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.data.Engine
import com.prayergate.app.data.TriggerLog
import com.prayergate.app.data.percentile
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The hidden debug screen (Settings > tap the version number 7 times).
 *
 * It answers one question: how fast does each engine show the question?
 * For each engine it shows the number of triggers, the median delay and
 * the 90th-percentile delay, followed by the raw log.
 */
@Composable
fun DebugScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = (context.applicationContext as PrayerGateApp).database.triggerLogDao()
    val scope = rememberCoroutineScope()
    val allLogs by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    // Delay only means something for "app opened" triggers. Time checks are
    // started by PrayerGate's own timer, so they are counted separately.
    val logs = allLogs.filter { it.reason == TriggerLog.REASON_OPEN }
    val timeChecks = allLogs.size - logs.size

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 20.dp),
        modifier = Modifier.fillMaxSize().systemBarsPadding().readableWidth(),
    ) {
        item {
            Text(
                withAccentFullStop(stringResource(R.string.debug_title)),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(20.dp))

            // One summary block per engine, then one for everything together.
            DelaySummary(stringResource(R.string.engine_usage_title), logs.filter { it.engine == Engine.USAGE.name })
            Spacer(Modifier.height(14.dp))
            DelaySummary(
                stringResource(R.string.engine_accessibility_title),
                logs.filter { it.engine == Engine.ACCESSIBILITY.name },
            )
            Spacer(Modifier.height(14.dp))
            DelaySummary(stringResource(R.string.debug_all), logs)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.debug_time_checks, timeChecks),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
            PrimaryButton(stringResource(R.string.debug_back), onClick = onBack)
            QuietButton(stringResource(R.string.debug_clear), onClick = { scope.launch { dao.deleteAll() } })

            Spacer(Modifier.height(28.dp))
            SectionLabel(stringResource(R.string.debug_log))
            Spacer(Modifier.height(8.dp))
        }
        items(allLogs, key = { it.id }) { log -> LogLine(log) }
    }
}

@Composable
private fun DelaySummary(title: String, logs: List<TriggerLog>) {
    val delays = logs.map { it.delayMs }
    Column(
        Modifier
            .fillMaxWidth()
            .card()
            .padding(20.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        SummaryRow(stringResource(R.string.debug_count), delays.size.toString())
        SummaryRow(stringResource(R.string.debug_median), formatMs(percentile(delays, 50)))
        SummaryRow(stringResource(R.string.debug_p90), formatMs(percentile(delays, 90)))
    }
}

@Composable
private fun formatMs(value: Long?): String =
    if (value == null) stringResource(R.string.debug_no_data) else stringResource(R.string.debug_ms, value)

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** One raw log row: time, app, engine, delay, result and action. */
@Composable
private fun LogLine(log: TriggerLog) {
    val time = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date(log.openedAt))
    val result = when (log.correct) {
        true -> stringResource(R.string.debug_right)
        false -> stringResource(R.string.debug_wrong)
        null -> stringResource(R.string.debug_no_answer)
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            "$time  ${log.packageName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row {
            Text(
                "${log.engine}  ${log.reason}  ${log.delayMs} ms  $result  ${log.action}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}
