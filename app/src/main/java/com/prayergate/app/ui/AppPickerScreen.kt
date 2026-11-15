package com.prayergate.app.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prayergate.app.R
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.data.SelectedApp
import com.prayergate.app.ui.theme.LocalAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Apps people most often want to pause, so they can be listed first.
 * Only the ones installed on the phone are shown.
 */
private val SUGGESTED_PACKAGES = listOf(
    "com.instagram.android",          // Instagram
    "com.zhiliaoapp.musically",       // TikTok
    "com.ss.android.ugc.trill",       // TikTok (Asia)
    "com.google.android.youtube",     // YouTube
    "com.facebook.katana",            // Facebook
    "com.twitter.android",            // X
    "com.reddit.frontpage",           // Reddit
    "com.snapchat.android",           // Snapchat
    "com.pinterest",                  // Pinterest
    "com.instagram.barcelona",        // Threads
    "com.netflix.mediaclient",        // Netflix
    "tv.twitch.android.app",          // Twitch
    "com.nhn.android.webtoon",        // Naver Webtoon
    "com.naver.linewebtoon",          // Webtoon
    "com.nhn.android.search",         // Naver
    "net.daum.android.daum",          // Daum
    "com.coupang.mobile.play",        // Coupang Play
)

/** One app on the phone, ready to show in the list. */
data class InstalledApp(val packageName: String, val label: String, val icon: ImageBitmap, val suggested: Boolean = false)

/**
 * Finds every app that has an icon in the launcher.
 * This can take a moment, so it runs on a background thread (Dispatchers.IO).
 */
suspend fun loadInstalledApps(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    pm.queryIntentActivities(launcherIntent, 0)
        .distinctBy { it.activityInfo.packageName }
        .filter { it.activityInfo.packageName != context.packageName } // not PrayerGate itself
        .map {
            InstalledApp(
                packageName = it.activityInfo.packageName,
                label = it.loadLabel(pm).toString(),
                icon = it.loadIcon(pm).toBitmap(96, 96).asImageBitmap(),
                suggested = it.activityInfo.packageName in SUGGESTED_PACKAGES,
            )
        }
        .sortedBy { it.label.lowercase() }
}

/** The app picker: every installed app with a switch. */
@Composable
fun AppPickerScreen() {
    val context = LocalContext.current
    val dao = (context.applicationContext as PrayerGateApp).database.selectedAppDao()
    val scope = rememberCoroutineScope()

    // null means "still loading".
    var apps by remember { mutableStateOf<List<InstalledApp>?>(null) }
    LaunchedEffect(Unit) { apps = loadInstalledApps(context) }

    // The chosen apps, straight from the database. Updates by itself.
    val selected by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedPackages = selected.map { it.packageName }.toSet()

    var search by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().readableWidth().padding(horizontal = 28.dp)) {
        Spacer(Modifier.height(36.dp))
        Text(
            withAccentFullStop(stringResource(R.string.apps_title)),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            // Before any app is chosen: an instruction. After: a running count.
            if (selectedPackages.isEmpty()) stringResource(R.string.apps_subtitle)
            else pluralStringResource(R.plurals.apps_chosen, selectedPackages.size, selectedPackages.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(22.dp))

        // The search box: a white pill with a soft shadow, no outline.
        TextField(
            value = search,
            onValueChange = { search = it },
            placeholder = {
                Text(stringResource(R.string.apps_search), color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            singleLine = true,
            shape = CircleShape,
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = LocalAccent.current.accent,
            ),
            modifier = Modifier.fillMaxWidth().card(CircleShape, elevation = 6.dp),
        )
        Spacer(Modifier.height(10.dp))

        val loaded = apps
        if (loaded == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground, strokeWidth = 1.5.dp)
            }
        } else {
            val visible = loaded.filter { it.label.contains(search.trim(), ignoreCase = true) }
            // Without a search: "Suggested" apps first, then "All apps".
            // While searching: one plain list of matches.
            val suggested = if (search.isBlank()) visible.filter { it.suggested } else emptyList()
            val rest = if (suggested.isEmpty()) visible else visible.filterNot { it.suggested }
            val toggle = { app: InstalledApp, checked: Boolean ->
                scope.launch {
                    val row = SelectedApp(app.packageName)
                    if (checked) dao.insert(row) else dao.delete(row)
                }
                Unit
            }
            LazyColumn(
                contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (suggested.isNotEmpty()) {
                    item(key = "header_suggested") { ListHeader(stringResource(R.string.apps_suggested)) }
                    items(suggested, key = { "s_" + it.packageName }) { app ->
                        AppRow(
                            app = app,
                            checked = app.packageName in selectedPackages,
                            onCheckedChange = { toggle(app, it) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "header_all") { ListHeader(stringResource(R.string.apps_all)) }
                }
                items(rest, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        checked = app.packageName in selectedPackages,
                        onCheckedChange = { toggle(app, it) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

/** A small label above a group of apps. */
@Composable
private fun ListHeader(text: String) {
    SectionLabel(text, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
}

@Composable
private fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                // The whole row is the touch target, not just the small switch.
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(vertical = 14.dp),
        ) {
            Image(bitmap = app.icon, contentDescription = null, modifier = Modifier.size(38.dp))
            Spacer(Modifier.width(16.dp))
            Text(
                app.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            // onCheckedChange = null: the row handles the tap, the switch just shows the state.
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = accent.accent,
                    checkedThumbColor = accent.onAccent,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
        }
        // A hairline between rows, like the rules in a ledger.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
