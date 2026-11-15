package com.prayergate.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.prayergate.app.R
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.data.Engine
import com.prayergate.app.detection.Engines
import com.prayergate.app.detection.Permissions
import com.prayergate.app.ui.theme.LocalAccent

/**
 * The list of permissions the chosen [engine] needs, each with a short
 * reason and an "Allow" button. Used in onboarding and in Settings.
 *
 * Engine A needs: usage access, display over other apps, notifications.
 * Engine B needs: the accessibility service.
 * Both need:      battery optimization turned off.
 */
@Composable
fun PermissionChecklist(engine: Engine, onRequestAccessibility: () -> Unit) {
    val context = LocalContext.current

    // The user grants permissions in system settings and then comes back.
    // Each time they come back we bump this number, which makes Compose
    // re-run the checks below.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refresh++
        Engines.apply(context)
    }

    // The small system pop-up that asks for the notification permission.
    val notificationRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    // Opens a system settings page; some phones lack a page, so never crash.
    fun open(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        }
    }

    // Reading "refresh" here ties the checks to it.
    @Suppress("UNUSED_EXPRESSION") refresh

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (engine == Engine.USAGE) {
            PermissionRow(
                title = stringResource(R.string.perm_usage_title),
                reason = stringResource(R.string.perm_usage_reason),
                granted = Permissions.hasUsageAccess(context),
                onAllow = { open(Permissions.usageAccessIntent()) },
            )
            PermissionRow(
                title = stringResource(R.string.perm_overlay_title),
                reason = stringResource(R.string.perm_overlay_reason),
                granted = Permissions.canDrawOverlays(context),
                onAllow = { open(Permissions.overlayIntent(context)) },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionRow(
                    title = stringResource(R.string.perm_notifications_title),
                    reason = stringResource(R.string.perm_notifications_reason),
                    granted = Permissions.canPostNotifications(context),
                    onAllow = { notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS) },
                )
            }
        } else {
            PermissionRow(
                title = stringResource(R.string.perm_accessibility_title),
                reason = stringResource(R.string.perm_accessibility_reason),
                granted = Permissions.isAccessibilityServiceOn(context),
                // Always show our own disclosure before the system settings page.
                onAllow = onRequestAccessibility,
            )
            // If the user already went to the settings page and the service is
            // still off, Android has probably blocked it (see RestrictedSettingsHelp).
            val settings = (context.applicationContext as PrayerGateApp).settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                settings.accessibilityDisclosureAccepted &&
                !Permissions.isAccessibilityServiceOn(context)
            ) {
                RestrictedSettingsHelp(onOpenAppInfo = { open(Permissions.appInfoIntent(context)) })
            }
        }
        PermissionRow(
            title = stringResource(R.string.perm_battery_title),
            reason = stringResource(R.string.perm_battery_reason),
            granted = Permissions.isIgnoringBatteryOptimizations(context),
            onAllow = { open(Permissions.batteryIntent(context)) },
        )
    }
}

@Composable
private fun PermissionRow(title: String, reason: String, granted: Boolean, onAllow: () -> Unit) {
    val accent = LocalAccent.current.accent
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .padding(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        if (granted) {
            // Done: the lapis full stop and the word, so colour is not the only signal.
            FullStop()
            SectionLabel(stringResource(R.string.perm_done), color = accent, modifier = Modifier.padding(end = 8.dp))
        } else {
            OutlinedButton(
                onClick = onAllow,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Text(
                    stringResource(R.string.perm_allow),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * Help for Android's "Restricted setting" block.
 *
 * From Android 13, an app installed from a downloaded APK (not from the Play
 * Store) cannot be switched on in Accessibility until the user allows it in
 * the app's own settings page. The switch is greyed out until then, which
 * looks like PrayerGate is broken. These steps explain the way through.
 */
@Composable
private fun RestrictedSettingsHelp(onOpenAppInfo: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .card()
            .padding(20.dp),
    ) {
        Text(
            stringResource(R.string.restricted_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.restricted_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        // The numbered steps, one per line.
        val steps = listOf(R.string.restricted_step_1, R.string.restricted_step_2, R.string.restricted_step_3)
        steps.forEachIndexed { index, step ->
            Row(Modifier.padding(vertical = 3.dp)) {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalAccent.current.accent,
                    modifier = Modifier.width(22.dp),
                )
                Text(
                    stringResource(step),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(stringResource(R.string.restricted_open_app_info), onClick = onOpenAppInfo)
    }
}
