package com.prayergate.app.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayergate.app.R
import com.prayergate.app.data.AppAppearance
import com.prayergate.app.data.AppLanguage
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.detection.Engines
import com.prayergate.app.detection.Permissions
import com.prayergate.app.ui.theme.LocalAccent
import com.prayergate.app.ui.theme.PrayerGateTheme

/**
 * The main window of the app. It holds every screen except the prayer
 * screen (that one is PrayerActivity, because it opens on top of other apps).
 */
class MainActivity : ComponentActivity() {

    /** Gives this screen the language and light/dark mode chosen in Settings. */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppAppearance.wrapScreen(newBase))
    }

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
private enum class Tab { APPS, SAVED, STATS, SETTINGS }

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
                Tab.SAVED -> SavedScreen()
                Tab.STATS -> StatsScreen()
                Tab.SETTINGS -> SettingsScreen(onRequestAccessibility, onOpenDebug, onOpenManual, onOpenLegal, onReset)
            }
        }

        // The bottom bar: four equal columns, each an icon over a word. The icons are
        // all the same size, so the columns look evenly spaced however long the words
        // are, and the four words share one size (see tabLabelStyle).
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding(),
        ) {
            val tabs = listOf(
                Triple(Tab.APPS, TabIcon.APPS, stringResource(R.string.tab_apps)),
                Triple(Tab.SAVED, TabIcon.SAVED, stringResource(R.string.tab_saved)),
                Triple(Tab.STATS, TabIcon.STATS, stringResource(R.string.tab_stats)),
                Triple(Tab.SETTINGS, TabIcon.SETTINGS, stringResource(R.string.tab_settings)),
            )
            val barWidth = minOf(maxWidth, 560.dp)
            val label = tabLabelStyle(tabs.map { it.third.uppercase() }, barWidth / tabs.size - 8.dp)
            Row(Modifier.readableWidth()) {
                for ((value, icon, text) in tabs) {
                    TabButton(text.uppercase(), text, icon, label, tab == value, Modifier.weight(1f)) { onTabChange(value) }
                }
            }
        }
    }
}

/**
 * One text style for all four tab words: the normal size, or, if the longest
 * word would not fit its column (a narrow screen, large text), the same smaller
 * size for every word, so the bar never looks uneven. The words are set in red
 * small capitals like the headings on the pages, a little smaller.
 */
@Composable
private fun tabLabelStyle(labels: List<String>, columnWidth: Dp): TextStyle {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val base = MaterialTheme.typography.labelMedium.copy(
        fontSize = 13.sp,
        letterSpacing = if (isKoreanShown()) 0.2.sp else 0.8.sp,
    )
    return remember(labels, columnWidth, base, density) {
        val available = with(density) { columnWidth.toPx() }
        val widest = labels.maxOf { measurer.measure(it, base, maxLines = 1, softWrap = false).size.width }
        if (widest <= available) base else base.copy(fontSize = (13f * available / widest).coerceAtLeast(9f).sp)
    }
}

/** The four icons, drawn as fine rounded lines, like the rules of a printed page. */
private enum class TabIcon { APPS, SAVED, STATS, SETTINGS }

/**
 * One tab: an icon over its word. The selected tab turns rubric red and a soft
 * red pill grows behind its icon.
 *
 * @param label the word as shown (capitals, for the small-capitals style).
 * @param description the word as a screen reader should say it.
 */
@Composable
private fun TabButton(
    label: String,
    description: String,
    icon: TabIcon,
    style: TextStyle,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current.accent
    val color by animateColorAsState(
        if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabColor",
    )
    val pill by animateFloatAsState(if (selected) 1f else 0f, label = "tabPill")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(top = 8.dp, bottom = 8.dp)
            .semantics { contentDescription = description; this.selected = selected },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(width = 56.dp, height = 32.dp)) {
            Box(
                Modifier
                    .size(width = 56.dp * pill, height = 32.dp)
                    .background(accent.copy(alpha = 0.13f * pill), MaterialTheme.shapes.large),
            )
            Canvas(Modifier.size(24.dp)) { drawTabIcon(icon, color) }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Draws [icon] in a 24 by 24 box with a 1.8 dp rounded line. */
private fun DrawScope.drawTabIcon(icon: TabIcon, color: Color) {
    val u = size.width / 24f
    val line = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    when (icon) {
        // Four rounded tiles: your apps.
        TabIcon.APPS -> for ((x, y) in listOf(4f to 4f, 13.5f to 4f, 4f to 13.5f, 13.5f to 13.5f)) {
            drawRoundRect(color, p(x, y), Size(6.5f * u, 6.5f * u), CornerRadius(2f * u), style = line)
        }
        // A bookmark ribbon with a swallow-tail, as in a prayer book: the prayers you kept.
        TabIcon.SAVED -> drawPath(
            Path().apply {
                moveTo(7f * u, 3.5f * u); lineTo(17f * u, 3.5f * u); lineTo(17f * u, 20.5f * u)
                lineTo(12f * u, 16f * u); lineTo(7f * u, 20.5f * u); close()
            },
            color, style = line,
        )
        // Three bars of different heights: the week.
        TabIcon.STATS -> {
            drawLine(color, p(5f, 20f), p(5f, 13f), line.width, StrokeCap.Round)
            drawLine(color, p(12f, 20f), p(12f, 4f), line.width, StrokeCap.Round)
            drawLine(color, p(19f, 20f), p(19f, 9f), line.width, StrokeCap.Round)
        }
        // Three sliders.
        TabIcon.SETTINGS -> for ((y, knob) in listOf(6f to 15f, 12f to 8f, 18f to 16f)) {
            drawLine(color, p(3.5f, y), p(20.5f, y), line.width, StrokeCap.Round)
            drawCircle(color, 2.6f * u, p(knob, y), style = line)
        }
    }
}
