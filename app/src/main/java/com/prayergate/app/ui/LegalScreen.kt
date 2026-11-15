package com.prayergate.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayergate.app.R

/*
 * Settings > Help > About & legal.
 *
 *  - Privacy policy: what PrayerGate stores, reads and sends (nothing leaves the phone).
 *  - Open-source licences: the components PrayerGate is built with, and the full
 *    licence texts (app/src/main/assets/licenses). The font licence (SIL OFL)
 *    and Apache 2.0 both require these texts to ship with the app.
 *  - Trademarks: app names such as Instagram belong to their owners.
 */

/** The privacy policy, section by section (title, text). */
private val PRIVACY_SECTIONS = listOf(
    R.string.privacy_summary_t to R.string.privacy_summary,
    R.string.privacy_stored_t to R.string.privacy_stored,
    R.string.privacy_read_t to R.string.privacy_read,
    R.string.privacy_sent_t to R.string.privacy_sent,
    R.string.privacy_delete_t to R.string.privacy_delete,
    R.string.privacy_children_t to R.string.privacy_children,
    R.string.privacy_changes_t to R.string.privacy_changes,
)

/** One open-source component: its name, who made it, its licence, and the licence file. */
private data class Component(val name: String, val owner: String, val licence: String, val file: String)

private val COMPONENTS = listOf(
    Component("Newsreader (font)", "The Newsreader Project Authors", "SIL Open Font License 1.1", "ofl-newsreader.txt"),
    Component("Hanken Grotesk (font)", "The Hanken Grotesk Project Authors", "SIL Open Font License 1.1", "ofl-hanken-grotesk.txt"),
    Component("Jetpack Compose, Material 3", "The Android Open Source Project", "Apache License 2.0", "apache-2.0.txt"),
    Component("AndroidX Core, Activity, Lifecycle, Room", "The Android Open Source Project", "Apache License 2.0", "apache-2.0.txt"),
    Component("Kotlin, kotlinx.coroutines", "JetBrains s.r.o. and contributors", "Apache License 2.0", "apache-2.0.txt"),
)

private enum class LegalPage { MENU, PRIVACY, LICENCES }

@Composable
fun LegalScreen(onBack: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(LegalPage.MENU) }
    var openFile by rememberSaveable { mutableStateOf<String?>(null) }

    // Back steps out one level at a time: licence text > licences > menu > Settings.
    val back = {
        when {
            openFile != null -> openFile = null
            page != LegalPage.MENU -> page = LegalPage.MENU
            else -> onBack()
        }
    }
    BackHandler(onBack = back)

    when {
        openFile != null -> LicenceText(openFile!!, back)
        page == LegalPage.PRIVACY -> PrivacyPolicy(back)
        page == LegalPage.LICENCES -> Licences(back, onOpen = { openFile = it })
        else -> LegalMenu(back, onOpen = { page = it })
    }
}

/** The shared frame: a "Back" link, a title, then the content. */
@Composable
private fun LegalFrame(title: String, onBack: () -> Unit, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 32.dp),
        modifier = Modifier.fillMaxSize().systemBarsPadding().readableWidth(),
    ) {
        item {
            TextButton(onClick = onBack) {
                Text("‹ " + stringResource(R.string.manual_back), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(20.dp))
        }
        content()
    }
}

@Composable
private fun LegalMenu(onBack: () -> Unit, onOpen: (LegalPage) -> Unit) {
    val context = LocalContext.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" }
    LegalFrame(stringResource(R.string.legal_title), onBack) {
        item {
            MenuRow(stringResource(R.string.privacy_title)) { onOpen(LegalPage.PRIVACY) }
            MenuRow(stringResource(R.string.licences_title)) { onOpen(LegalPage.LICENCES) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.trademarks_t))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.trademarks), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.settings_version, version), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MenuRow(title: String, onClick: () -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(onClick = onClick).padding(vertical = 18.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PrivacyPolicy(onBack: () -> Unit) {
    val contact = stringResource(R.string.legal_contact)
    LegalFrame(stringResource(R.string.privacy_title), onBack) {
        item {
            Text(
                stringResource(R.string.privacy_effective, stringResource(R.string.privacy_date)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
        items(PRIVACY_SECTIONS) { (title, body) -> PolicySection(stringResource(title), stringResource(body)) }
        // The contact section appears once a contact address is filled in (strings.xml: legal_contact).
        if (contact.isNotBlank()) {
            item { PolicySection(stringResource(R.string.privacy_contact_t), stringResource(R.string.privacy_contact, contact)) }
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Licences(onBack: () -> Unit, onOpen: (String) -> Unit) {
    LegalFrame(stringResource(R.string.licences_title), onBack) {
        item {
            Text(stringResource(R.string.licences_intro), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
        items(COMPONENTS) { c ->
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.fillMaxWidth().clickable { onOpen(c.file) }.padding(vertical = 14.dp)) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text("${c.owner} · ${c.licence}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** The full text of one licence, read from app/src/main/assets/licenses. */
@Composable
private fun LicenceText(file: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val text = remember(file) { context.assets.open("licenses/$file").bufferedReader().use { it.readText() } }
    LegalFrame(stringResource(R.string.licences_title), onBack) {
        item {
            Text(
                text,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
