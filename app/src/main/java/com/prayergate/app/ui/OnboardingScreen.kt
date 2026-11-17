package com.prayergate.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.detection.Permissions
import com.prayergate.app.ui.theme.LocalAccent
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.prayergate.app.R

/** The screens of the first-run setup, in order. */
private enum class Step { WELCOME, OVERLAY, USAGE, NOTIFICATIONS, BATTERY, APPS, READY }

/**
 * First-run setup. One thing per screen, so nobody needs other instructions:
 *
 *   WELCOME        what PrayerGate does
 *   OVERLAY        "Display over other apps" (system settings, comes back by itself)
 *   USAGE          "Usage access"            (system settings, comes back by itself)
 *   NOTIFICATIONS  one tap on a system pop-up (Android 13+ only)
 *   BATTERY        one tap on a system pop-up
 *   APPS           choose at least one app
 *   READY          what to do next
 *
 * OVERLAY comes before USAGE on purpose: once PrayerGate may display over other
 * apps, Android also lets it bring itself back to the front, which is how the
 * USAGE step can return here automatically.
 * Steps that are already done are skipped. Back goes to the previous screen.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val steps = remember {
        Step.entries.filter { it != Step.NOTIFICATIONS || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }
    }
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index]
    // Moves on from [from] to the following step. Several things can notice
    // that a step is finished (the automatic check, coming back to the app);
    // only the first one counts, so a step is never skipped by accident.
    val advanceFrom = { from: Step ->
        if (steps[index] == from && index < steps.lastIndex) index++
    }

    BackHandler(enabled = index > 0) { index-- }

    // Skip a permission step that is already done (for example after reinstalling).
    LaunchedEffect(step) {
        val done = when (step) {
            Step.OVERLAY -> Permissions.canDrawOverlays(context)
            Step.USAGE -> Permissions.hasUsageAccess(context)
            Step.NOTIFICATIONS -> Permissions.canPostNotifications(context)
            Step.BATTERY -> Permissions.isIgnoringBatteryOptimizations(context)
            else -> false
        }
        if (done) advanceFrom(step)
    }

    // "Step 2 of 5" counts only the steps that ask something of the user.
    val numbered = steps.filter { it != Step.WELCOME && it != Step.READY }
    val number = numbered.indexOf(step) + 1

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (fadeIn(tween(260)) + slideInHorizontally(tween(320)) { direction * it / 6 }) togetherWith
                fadeOut(tween(120))
        },
        label = "onboardingStep",
    ) { current ->
        when (current) {
            Step.WELCOME -> WelcomeStep(onNext = { advanceFrom(Step.WELCOME) })
            Step.OVERLAY -> SettingsStep(
                number, numbered.size,
                title = stringResource(R.string.ob_overlay_title),
                body = stringResource(R.string.ob_overlay_body),
                intents = listOf(Permissions.overlayIntent(context)),
                isGranted = { Permissions.canDrawOverlays(context) },
                onGranted = { advanceFrom(Step.OVERLAY) },
            )
            Step.USAGE -> SettingsStep(
                number, numbered.size,
                title = stringResource(R.string.ob_usage_title),
                body = stringResource(R.string.ob_usage_body),
                // PrayerGate's own switch where the phone supports it, otherwise the list.
                intents = listOf(Permissions.usageAccessIntent(context), Permissions.usageAccessIntent()),
                isGranted = { Permissions.hasUsageAccess(context) },
                onGranted = { advanceFrom(Step.USAGE) },
            )
            Step.NOTIFICATIONS -> NotificationStep(number, numbered.size, onDone = { advanceFrom(Step.NOTIFICATIONS) })
            Step.BATTERY -> BatteryStep(number, numbered.size, onDone = { advanceFrom(Step.BATTERY) })
            Step.APPS -> AppsStep(number, numbered.size, onDone = { advanceFrom(Step.APPS) })
            Step.READY -> ReadyStep(onFinished)
        }
    }
}

/** The layout every setup step shares: progress at the top, text, one button at the bottom. */
@Composable
private fun StepLayout(
    number: Int,
    total: Int,
    title: String,
    body: String,
    how: String?,
    button: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .readableWidth()
            .padding(horizontal = 28.dp, vertical = 16.dp),
    ) {
        StepProgress(number, total)
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(14.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (how != null) {
                Spacer(Modifier.height(24.dp))
                // What to do on the next screen, in a card so it stands out.
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth().card().padding(18.dp),
                ) {
                    Cross(size = 24.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(how, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        button()
    }
}

/** "Step 2 of 5" and a row of short bars, one per step. */
@Composable
private fun StepProgress(number: Int, total: Int) {
    val accent = LocalAccent.current.accent
    Column(Modifier.padding(top = 12.dp)) {
        SectionLabel(stringResource(R.string.ob_step, number, total))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            for (i in 1..total) {
                val color by animateColorAsState(
                    if (i <= number) accent else MaterialTheme.colorScheme.outlineVariant,
                    label = "stepBar",
                )
                Box(Modifier.weight(1f).height(4.dp).background(color, CircleShape))
            }
        }
    }
}

/**
 * A step that needs the user to switch PrayerGate on in a system settings page.
 * While they are there, PrayerGate checks twice a second; as soon as the switch
 * is on, it brings itself back to the front and moves to the next step.
 */
@Composable
private fun SettingsStep(
    number: Int,
    total: Int,
    title: String,
    body: String,
    intents: List<Intent>,
    isGranted: () -> Boolean,
    onGranted: () -> Unit,
) {
    val context = LocalContext.current
    var waiting by remember { mutableStateOf(false) }

    LaunchedEffect(waiting) {
        if (!waiting) return@LaunchedEffect
        while (!isGranted()) delay(500)
        bringPrayerGateBack(context)
        onGranted()
    }
    // Also check whenever the user comes back by pressing Back themselves.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (isGranted()) onGranted() }

    StepLayout(number, total, title, body, how = stringResource(R.string.ob_find_switch)) {
        PrimaryButton(stringResource(R.string.ob_open_settings), onClick = {
            waiting = true
            openSettings(context, *intents.toTypedArray())
        })
    }
}

/** Notifications: one tap on Android's own pop-up. The step continues either way. */
@Composable
private fun NotificationStep(number: Int, total: Int, onDone: () -> Unit) {
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }
    StepLayout(
        number, total,
        title = stringResource(R.string.ob_notifications_title),
        body = stringResource(R.string.ob_notifications_body),
        how = stringResource(R.string.ob_tap_allow),
    ) {
        PrimaryButton(stringResource(R.string.ob_continue), onClick = {
            request.launch(Manifest.permission.POST_NOTIFICATIONS)
        })
    }
}

/** Battery: one tap on Android's own pop-up. The step continues either way. */
@Composable
private fun BatteryStep(number: Int, total: Int, onDone: () -> Unit) {
    val context = LocalContext.current
    var asked by remember { mutableStateOf(false) }
    // The pop-up covers PrayerGate; when it closes, PrayerGate resumes and the step ends.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (asked) onDone() }
    StepLayout(
        number, total,
        title = stringResource(R.string.ob_battery_title),
        body = stringResource(R.string.ob_battery_body),
        how = stringResource(R.string.ob_tap_allow),
    ) {
        PrimaryButton(stringResource(R.string.ob_continue), onClick = {
            asked = true
            openSettings(context, Permissions.batteryIntent(context))
        })
    }
}

/** Choose the apps. "Done" stays off until at least one app is switched on. */
@Composable
private fun AppsStep(number: Int, total: Int, onDone: () -> Unit) {
    val context = LocalContext.current
    val dao = (context.applicationContext as PrayerGateApp).database.selectedAppDao()
    val selected by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())

    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Box(Modifier.readableWidth().padding(horizontal = 28.dp)) { StepProgress(number, total) }
        Box(Modifier.weight(1f)) { AppPickerScreen() }
        Box(Modifier.readableWidth().padding(horizontal = 28.dp, vertical = 16.dp)) {
            PrimaryButton(
                if (selected.isEmpty()) stringResource(R.string.ob_apps_pick_one) else stringResource(R.string.ob_apps_done),
                onClick = onDone,
                enabled = selected.isNotEmpty(),
            )
        }
    }
}

/** The last screen: what happens next, named after an app the user chose. */
@Composable
private fun ReadyStep(onFinished: () -> Unit) {
    val context = LocalContext.current
    val dao = (context.applicationContext as PrayerGateApp).database.selectedAppDao()
    val selected by dao.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
    val firstApp = selected.firstOrNull()?.packageName?.let { appLabel(context, it) } ?: ""

    PageGround(Modifier.fillMaxSize()) {
        val softWhite = MaterialTheme.colorScheme.onSurfaceVariant
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .readableWidth()
                .padding(horizontal = 36.dp, vertical = 20.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                Cross(breathing = true, size = 44.dp)
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.ob_ready_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(16.dp))
                if (firstApp.isNotEmpty()) {
                    Text(
                        stringResource(R.string.ob_ready_body, firstApp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(stringResource(R.string.ob_ready_tip), style = MaterialTheme.typography.bodyLarge, color = softWhite)
            }
            PrimaryButton(stringResource(R.string.ob_finish), onClick = onFinished, modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}

/**
 * Opens the first of [intents] that this phone supports. If none works,
 * opens the main Settings app, so the button never does nothing.
 */
private fun openSettings(context: Context, vararg intents: Intent) {
    for (intent in intents) {
        try {
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            // This phone does not have that page; try the next one.
        }
    }
    context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
}

/**
 * Brings PrayerGate back in front of the system settings page, closing that
 * page. Android allows this once "display over other apps" is on.
 */
private fun bringPrayerGateBack(context: Context) {
    try {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    } catch (e: Exception) {
        // Not allowed on this phone: the user presses Back, and ON_RESUME continues.
    }
}

/** "com.instagram.android" -> "Instagram". */
private fun appLabel(context: Context, packageName: String): String = try {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
} catch (e: Exception) {
    ""
}

/**
 * Step 0: the first thing a new user sees, set like the title page of a
 * prayer book: the cross, the name as a red rubric, one sentence, and the
 * verse the app is built on (Matthew 26:41). The language switch sits at the
 * top, for people who need the other language before they can find Settings.
 */
@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    PageGround(Modifier.fillMaxSize()) {
        val soft = MaterialTheme.colorScheme.onSurfaceVariant
        val korean = LocalConfiguration.current.locales[0].language == "ko"
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .readableWidth()
                .padding(horizontal = 36.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.End) {
                LanguageToggle()
            }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                val tight = LocalConfiguration.current.let { it.screenHeightDp < 700 || it.fontScale > 1.15f }
                Cross(breathing = true, size = if (tight) 40.dp else 56.dp)
                Spacer(Modifier.height(if (tight) 8.dp else 14.dp))
                SectionLabel(stringResource(R.string.app_name))
                Spacer(Modifier.height(20.dp))
                // The headline is a little smaller on narrow screens (Z Fold cover, small phones).
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val narrow = maxWidth < 300.dp || tight
                    Text(
                        stringResource(R.string.onboarding_title),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = if (narrow) 36.sp else 44.sp,
                            lineHeight = if (narrow) 42.sp else 50.sp,
                            letterSpacing = if (korean) 0.sp else (-0.6).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Box(Modifier.width(44.dp).height(1.dp).background(LocalAccent.current.accent.copy(alpha = 0.8f)))
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.onboarding_verse),
                    style = MaterialTheme.typography.labelSmall,
                    color = soft,
                    textAlign = TextAlign.Center,
                )
                SectionLabel(stringResource(R.string.onboarding_verse_ref), modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(26.dp))
                // Three short paragraphs: what happens, what you decide, where the data lives.
                for (text in listOf(R.string.onboarding_body_1, R.string.onboarding_body_2, R.string.onboarding_body_3)) {
                    Text(
                        stringResource(text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = soft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }

            PrimaryButton(stringResource(R.string.onboarding_next), onClick = onNext, modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
        }
    }
}

