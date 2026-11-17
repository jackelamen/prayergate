package com.prayergate.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.R
import com.prayergate.app.data.Prayer
import com.prayergate.app.data.PrayerBook

/**
 * The Saved tab: prayers the user kept with the heart on the prayer screen,
 * newest first. Tapping one opens it to pray again (not logged, no app
 * waiting). "Pray now" opens a prayer from the chosen themes.
 */
@Composable
fun SavedScreen() {
    val context = LocalContext.current
    val dao = (context.applicationContext as PrayerGateApp).database.savedPrayerDao()
    val savedRows by dao.observeAll().collectAsStateWithLifecycle(initialValue = null)
    val allPrayers = remember { PrayerBook.load(context).associateBy { it.id } }
    val korean = LocalConfiguration.current.locales[0].language == "ko"

    val loaded = savedRows ?: return
    val prayers = loaded.mapNotNull { allPrayers[it.prayerId] }

    LazyColumn(
        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 36.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().readableWidth(),
    ) {
        item {
            Text(
                stringResource(R.string.saved_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(if (prayers.isEmpty()) R.string.saved_empty else R.string.saved_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(22.dp))
            PrimaryButton(
                stringResource(R.string.saved_pray_now),
                onClick = { context.startActivity(PrayerActivity.quietIntent(context)) },
            )
            Spacer(Modifier.height(8.dp))
        }
        items(prayers, key = { it.id }) { prayer ->
            SavedCard(prayer, korean, Modifier.animateItem()) {
                context.startActivity(PrayerActivity.quietIntent(context, prayer.id))
            }
        }
    }
}

/** One saved prayer: its theme, the prayer, and the verse it takes up. */
@Composable
private fun SavedCard(prayer: Prayer, korean: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .card()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(20.dp),
    ) {
        SectionLabel(stringResource(themeName(prayer.theme)))
        Spacer(Modifier.height(8.dp))
        Text(
            if (korean) keepKoreanWordsTogether(prayer.textKo) else prayer.text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (korean) keepKoreanWordsTogether(prayer.verseKo) else prayer.verse,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.9f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SectionLabel(prayer.reference(korean), modifier = Modifier.align(Alignment.End).padding(top = 4.dp))
    }
}
