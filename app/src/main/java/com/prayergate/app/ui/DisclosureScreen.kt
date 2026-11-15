package com.prayergate.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prayergate.app.R

/**
 * The "prominent disclosure" Google Play requires before an app asks the
 * user to turn on an accessibility service.
 *
 * It must say, in plain words and BEFORE the system settings page opens:
 * what the service is used for, what data it reads, and what happens to
 * that data. The user must actively agree.
 */
@Composable
fun DisclosureScreen(onAgree: () -> Unit, onDecline: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .readableWidth()
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.disclosure_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(28.dp))
            // Each pair is one question the user might have, and its answer.
            val sections = listOf(
                R.string.disclosure_why_title to R.string.disclosure_why_body,
                R.string.disclosure_what_title to R.string.disclosure_what_body,
                R.string.disclosure_not_title to R.string.disclosure_not_body,
                R.string.disclosure_where_title to R.string.disclosure_where_body,
            )
            for ((title, body) in sections) {
                SectionLabel(stringResource(title), color = MaterialTheme.colorScheme.onBackground)
                Text(
                    stringResource(body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 22.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.disclosure_agree), onAgree)
        QuietButton(stringResource(R.string.disclosure_decline), onDecline)
    }
}
