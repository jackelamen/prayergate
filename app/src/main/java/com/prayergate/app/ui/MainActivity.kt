package com.prayergate.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayergate.app.R
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.detection.Engines
import com.prayergate.app.detection.Permissions
import com.prayergate.app.ui.theme.LocalAccent
import com.prayergate.app.ui.theme.PrayerGateTheme

/**
 * The main window of the app. It holds every screen except the question
 * screen (that one is QuestionActivity, because it opens on top of other apps).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrayerGateTheme { AppRoot() }
        }
    }

    /** Every time the app comes back to the front, make sure the right engine is running. */
    override fun onResume() {
        super.onResume()
        Engines.apply(this)
    }
}

/** The screens that take over the whole window. */
private enum class Screen { ONBOARDING, HOME, DISCLOSURE, DEBUG, MANUAL, LEGAL }

/** The four tabs inside HOME. */
private enum class Tab { APPS, REVIEW, STATS, SETTINGS }

/**
 * Decides which screen is visible. Navigation here is just one variable:
 * change [screen] and Compose shows the matching screen.
 */
@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as PrayerGateApp

    var screen by rememberSaveable {
        mutableStateOf(if (app.settings.onboardingDone) Screen.HOME else Screen.ONBOARDING)
    }
    // Where to return to after the accessibility disclosure.
    var screenBeforeDisclosure by rememberSaveable { mutableStateOf(Screen.HOME) }
    var tab by rememberSaveable { mutableStateOf(Tab.APPS) }

    // Shows the disclosure first; only after "I agree" do we open system settings.
    val requestAccessibility = {
        screenBeforeDisclosure = screen
        screen = Screen.DISCLOSURE
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
            label = "screen",
        ) { current ->
            when (current) {
                Screen.ONBOARDING -> OnboardingScreen(
                    onFinished = {
                        app.settings.onboardingDone = true
                        Engines.apply(context)
                        tab = Tab.APPS
                        screen = Screen.HOME
                    },
                )

                Screen.HOME -> HomeScreen(
                    tab = tab,
                    onTabChange = { tab = it },
                    onRequestAccessibility = requestAccessibility,
                    onOpenDebug = { screen = Screen.DEBUG },
                    onOpenManual = { screen = Screen.MANUAL },
                    onOpenLegal = { screen = Screen.LEGAL },
                    onReset = {
                        // Everything is deleted: start again from the first setup screen.
                        tab = Tab.APPS
                        screen = Screen.ONBOARDING
                    },
                )

                Screen.DISCLOSURE -> {
                    BackHandler { screen = screenBeforeDisclosure }
                    DisclosureScreen(
                        onAgree = {
                            app.settings.accessibilityDisclosureAccepted = true
                            screen = screenBeforeDisclosure
                            context.startActivity(Permissions.accessibilityIntent())
                        },
                        onDecline = { screen = screenBeforeDisclosure },
                    )
                }

                Screen.DEBUG -> {
                    BackHandler { screen = Screen.HOME }
                    DebugScreen(onBack = { screen = Screen.HOME })
                }

                Screen.MANUAL -> {
                    BackHandler { screen = Screen.HOME }
                    ManualScreen(onBack = { screen = Screen.HOME })
                }

                // LegalScreen handles Back itself (it has pages inside it).
                Screen.LEGAL -> LegalScreen(onBack = { screen = Screen.HOME })
            }
        }
    }
}

@Composable
private fun HomeScreen(
    tab: Tab,
    onTabChange: (Tab) -> Unit,
    onRequestAccessibility: () -> Unit,
    onOpenDebug: () -> Unit,
    onOpenManual: () -> Unit,
    onOpenLegal: () -> Unit,
    onReset: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // The current tab. A new tab slides in from the side it lives on.
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (fadeIn(tween(200)) + slideInHorizontally(tween(260)) { direction * it / 12 }) togetherWith
                    fadeOut(tween(100))
            },
            label = "tab",
            modifier = Modifier.weight(1f).statusBarsPadding(),
        ) { current ->
            when (current) {
                Tab.APPS -> AppPickerScreen()
                Tab.REVIEW -> ReviewScreen()
                Tab.STATS -> StatsScreen()
                Tab.SETTINGS -> SettingsScreen(onRequestAccessibility, onOpenDebug, onOpenManual, onOpenLegal, onReset)
            }
        }

        // The bottom bar: four words, no icons to decode.
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding(),
        ) {
            Row(Modifier.readableWidth()) {
                TabButton(stringResource(R.string.tab_apps), tab == Tab.APPS, Modifier.weight(1f)) { onTabChange(Tab.APPS) }
                TabButton(stringResource(R.string.tab_review), tab == Tab.REVIEW, Modifier.weight(1f)) { onTabChange(Tab.REVIEW) }
                TabButton(stringResource(R.string.tab_stats), tab == Tab.STATS, Modifier.weight(1f)) { onTabChange(Tab.STATS) }
                TabButton(stringResource(R.string.tab_settings), tab == Tab.SETTINGS, Modifier.weight(1f)) { onTabChange(Tab.SETTINGS) }
            }
        }
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabColor",
    )
    // The selected tab is marked by a small full stop that grows from nothing.
    val dotSize by animateDpAsState(if (selected) 5.dp else 0.dp, label = "tabDot")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(top = 18.dp, bottom = 12.dp),
    ) {
        FitText(text.uppercase(), MaterialTheme.typography.labelMedium, color, minFontSize = 8.sp)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.height(5.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(dotSize).background(LocalAccent.current.accent, CircleShape))
        }
    }
}
