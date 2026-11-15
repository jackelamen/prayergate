package com.prayergate.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prayergate.app.R

/**
 * The user guide (Settings > Help > User guide).
 * Each section is a title and a short paragraph from strings.xml, so the
 * guide is in English and Korean like the rest of the app.
 * To add a section: add two strings and one line to [SECTIONS].
 */
private val SECTIONS = listOf(
    R.string.manual_how_t to R.string.manual_how,
    R.string.manual_apps_t to R.string.manual_apps,
    R.string.manual_answer_t to R.string.manual_answer,
    R.string.manual_close_t to R.string.manual_close,
    R.string.manual_cooldown_t to R.string.manual_cooldown,
    R.string.manual_timecheck_t to R.string.manual_timecheck,
    R.string.manual_review_t to R.string.manual_review,
    R.string.manual_stats_t to R.string.manual_stats,
    R.string.manual_level_t to R.string.manual_level,
    R.string.manual_widget_t to R.string.manual_widget,
    R.string.manual_engines_t to R.string.manual_engines,
    R.string.manual_trouble_t to R.string.manual_trouble,
    R.string.manual_reset_t to R.string.manual_reset,
    R.string.manual_privacy_t to R.string.manual_privacy,
    R.string.manual_language_t to R.string.manual_language,
)

@Composable
fun ManualScreen(onBack: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 32.dp),
        modifier = Modifier.fillMaxSize().systemBarsPadding().readableWidth(),
    ) {
        item {
            // A visible way back, as well as the phone's Back gesture.
            TextButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
                Text(
                    "‹ " + stringResource(R.string.manual_back),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.manual_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(20.dp))
        }
        items(SECTIONS) { (title, body) ->
            Column(Modifier.fillMaxWidth()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(18.dp))
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}
