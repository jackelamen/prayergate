package com.prayergate.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.prayergate.app.R
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.data.AppLanguage
import com.prayergate.app.data.AppReset
import com.prayergate.app.data.PrayerTheme
import com.prayergate.app.data.Engine
import com.prayergate.app.detection.Engines
import com.prayergate.app.ui.theme.LocalAccent

/** How many taps on the version number open the hidden debug screen. */
private const val TAPS_TO_OPEN_DEBUG = 7

@Composable
fun SettingsScreen(
    onRequestAccessibility: () -> Unit,
    onOpenDebug: () -> Unit,
    onOpenManual: () -> Unit,
    onOpenLegal: () -> Unit,
    onReset: () -> Unit,
) {
    val context = LocalContext.current
    val settings = (context.applicationContext as PrayerGateApp).settings

    // Each value is kept twice: in Compose state (so the screen redraws)
    // and in Settings (so it is saved). The helpers below update both.
    var themes by remember { mutableStateOf(settings.themes) }
    var cooldown by remember { mutableIntStateOf(settings.cooldownMinutes) }
    var timeCheck by remember { mutableIntStateOf(settings.sessionLimitMinutes) }
    var engine by remember { mutableStateOf(settings.engine) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .readableWidth()
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(36.dp))
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // --- Language ---
        Spacer(Modifier.height(32.dp))
        SectionLabel(stringResource(R.string.settings_language))
        Spacer(Modifier.height(12.dp))
        val setLanguage = rememberLanguageSetter()
        ChoiceRow(
            options = AppLanguage.entries,
            selected = AppLanguage.saved(context),
            label = {
                stringResource(
                    when (it) {
                        AppLanguage.SYSTEM -> R.string.lang_system
                        AppLanguage.ENGLISH -> R.string.lang_english
                        AppLanguage.KOREAN -> R.string.lang_korean
                    }
                )
            },
            onSelect = setLanguage,
        )
        Text(
            stringResource(R.string.settings_language_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )

        // --- Themes ---
        Spacer(Modifier.height(32.dp))
        SectionLabel(stringResource(R.string.settings_themes))
        Spacer(Modifier.height(12.dp))
        ThemePicker(
            selected = themes,
            onToggle = { theme ->
                // At least one theme always stays on.
                val next = if (theme in themes) themes - theme else themes + theme
                if (next.isNotEmpty()) {
                    themes = next
                    settings.themes = next
                }
            },
        )
        Text(
            stringResource(R.string.settings_themes_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )

        // --- Cooldown ---
        Spacer(Modifier.height(32.dp))
        SectionLabel(stringResource(R.string.settings_cooldown))
        Spacer(Modifier.height(10.dp))
        ChoiceRow(
            options = listOf(1, 5, 15),
            selected = cooldown,
            label = { stringResource(R.string.cooldown_minutes, it) },
            onSelect = { cooldown = it; settings.cooldownMinutes = it },
        )
        Text(
            stringResource(R.string.settings_cooldown_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )

        // --- Time check ---
        Spacer(Modifier.height(32.dp))
        SectionLabel(stringResource(R.string.settings_time_check))
        Spacer(Modifier.height(12.dp))
        ChoiceRow(
            options = listOf(0, 5, 15, 30),
            selected = timeCheck,
            label = { if (it == 0) stringResource(R.string.time_check_off) else stringResource(R.string.cooldown_minutes, it) },
            onSelect = { timeCheck = it; settings.sessionLimitMinutes = it },
        )
        Text(
            stringResource(R.string.settings_time_check_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )

        // --- Detection engine ---
        Spacer(Modifier.height(32.dp))
        SectionLabel(stringResource(R.string.settings_engine))
        Spacer(Modifier.height(10.dp))
        val selectEngine = { chosen: Engine ->
            engine = chosen
            settings.engine = chosen
            Engines.apply(context) // start or stop the polling service right away
        }
        EngineCard(
            title = stringResource(R.string.engine_usage_title),
            description = stringResource(R.string.engine_usage_description),
            selected = engine == Engine.USAGE,
            onClick = { selectEngine(Engine.USAGE) },
        )
        Spacer(Modifier.height(14.dp))
        EngineCard(
            title = stringResource(R.string.engine_accessibility_title),
            description = stringResource(R.string.engine_accessibility_description),
            selected = engine == Engine.ACCESSIBILITY,
            onClick = { selectEngine(Engine.ACCESSIBILITY) },
        )

        // --- Permissions for the selected engine ---
        Spacer(Modifier.height(32.dp))
        SectionLabel(stringResource(R.string.settings_permissions))
        Spacer(Modifier.height(10.dp))
        AnimatedContent(
            targetState = engine,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "permissions",
        ) { current ->
            PermissionChecklist(engine = current, onRequestAccessibility = onRequestAccessibility)
        }

        // --- Help ---
        Spacer(Modifier.height(32.dp))
        SectionLabel(stringResource(R.string.settings_help))
        Spacer(Modifier.height(12.dp))
        LinkRow(
            title = stringResource(R.string.manual_open),
            description = stringResource(R.string.manual_open_desc),
            onClick = onOpenManual,
        )
        Spacer(Modifier.height(12.dp))
        LinkRow(
            title = stringResource(R.string.legal_title),
            description = stringResource(R.string.legal_desc),
            onClick = onOpenLegal,
        )

        // --- Data: reset everything, after a confirmation ---
        var confirmReset by remember { mutableStateOf(false) }
        Spacer(Modifier.height(32.dp))
        SectionLabel(stringResource(R.string.settings_data))
        Spacer(Modifier.height(12.dp))
        LinkRow(
            title = stringResource(R.string.reset_title),
            description = stringResource(R.string.reset_desc),
            onClick = { confirmReset = true },
        )
        if (confirmReset) {
            AlertDialog(
                onDismissRequest = { confirmReset = false },
                title = { Text(stringResource(R.string.reset_confirm_title)) },
                text = { Text(stringResource(R.string.reset_confirm_body), style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmReset = false
                        AppReset.resetEverything(context, onDone = onReset)
                    }) {
                        Text(stringResource(R.string.reset_confirm), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmReset = false }) {
                        Text(stringResource(R.string.reset_cancel), color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }

        // --- Version (tap it 7 times to open the debug screen) ---
        var taps by remember { mutableIntStateOf(0) }
        val version = remember {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        }
        SectionLabel(
            stringResource(R.string.settings_version, version),
            modifier = Modifier
                .padding(top = 28.dp, bottom = 28.dp)
                // indication = null: no ripple, so the secret stays secret.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    taps++
                    if (taps >= TAPS_TO_OPEN_DEBUG) {
                        taps = 0
                        onOpenDebug()
                    }
                },
        )
    }
}

/** A card that opens something: a title, one line of description, and an arrow. */
@Composable
private fun LinkRow(title: String, description: String, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .card(shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A selectable card describing one detection engine. The chosen one carries the full stop. */
@Composable
private fun EngineCard(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
        label = "engineBorder",
    )
    val shape = MaterialTheme.shapes.medium
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .card(shape)
            .border(1.5.dp, borderColor, shape)
            .clip(shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(start = 20.dp, top = 18.dp, bottom = 18.dp, end = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(3.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // The cross marks the selected engine.
        val dot by animateFloatAsState(if (selected) 1f else 0f, label = "engineDot")
        Cross(Modifier.graphicsLayer { alpha = dot; scaleX = dot; scaleY = dot }, size = 24.dp)
    }
}

/**
 * The eight prayer themes as pills that wrap onto several lines. A chosen
 * theme is filled with the accent and has a tick, so colour is not the only
 * signal; the last chosen theme cannot be switched off.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemePicker(selected: Set<PrayerTheme>, onToggle: (PrayerTheme) -> Unit) {
    val accent = LocalAccent.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (theme in PrayerTheme.entries) {
            val on = theme in selected
            val background by animateColorAsState(
                if (on) accent.accent else MaterialTheme.colorScheme.surfaceVariant, label = "themeBg",
            )
            val textColor by animateColorAsState(
                if (on) accent.onAccent else MaterialTheme.colorScheme.onSurfaceVariant, label = "themeText",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(background)
                    .toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggle(theme) })
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    (if (on) "\u2713 " else "") + stringResource(themeName(theme)),
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                )
            }
        }
    }
}
