package com.prayergate.app.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.HapticFeedbackConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.R
import com.prayergate.app.data.AppLanguage
import com.prayergate.app.data.Engine
import com.prayergate.app.data.Prayer
import com.prayergate.app.data.PrayerBook
import com.prayergate.app.data.SavedPrayer
import com.prayergate.app.data.TriggerLog
import com.prayergate.app.data.continueDelaySeconds
import com.prayergate.app.data.pickPrayer
import com.prayergate.app.data.readingMillis
import com.prayergate.app.detection.GateController
import com.prayergate.app.ui.theme.LocalAccent
import com.prayergate.app.ui.theme.PrayerGateTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * THE PAUSE: the full-screen prayer shown before a chosen app.
 *
 * One short prayer. The Amen button fills slowly while the prayer is read,
 * then the user presses and holds it to pray it. After Amen, "Close" and
 * "Continue" appear.
 *
 * This file has two parts:
 *  1. PrayerActivity - loads the prayer and saves the results (no layout code).
 *  2. PrayerScreen   - draws everything (no database code).
 */
class PrayerActivity : ComponentActivity() {

    private val app get() = application as PrayerGateApp

    /** Gives this screen the language chosen in Settings (see AppLanguage). */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    // What triggered this screen (read from the Intent).
    private var targetPackage = ""
    private var openedAt = 0L
    private var engine = Engine.USAGE
    private var reason = TriggerLog.REASON_OPEN
    private var minutesUsed by mutableIntStateOf(0)

    // Quiet mode: opened from the Saved tab or the widget, not by an app.
    // Nothing is written to the log and no app is waiting underneath.
    private var quiet = false
    private var requestedPrayerId: String? = null
    private var fromWidget by mutableStateOf(false)

    // Gentle escalation: opening the same app again and again within an hour
    // makes "Continue" wait a few seconds (see continueDelaySeconds).
    private var opensThisHour by mutableIntStateOf(0)
    private var continueDelay by mutableIntStateOf(0)

    private var korean = false

    // What the screen is showing. Compose redraws when these change.
    private var prayer by mutableStateOf<Prayer?>(null)
    private var prayed by mutableStateOf(false)
    private var saved by mutableStateOf(false)

    /** The database row for this pause. Only touched on app.dbThread. */
    private var log: TriggerLog? = null
    private var shownLogged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        korean = resources.configuration.locales[0].language == "ko"
        readIntent(intent)
        loadPrayer()

        setContent {
            PrayerGateTheme {
                if (quiet) {
                    PrayerScreen(
                        header = stringResource(if (fromWidget) R.string.widget_today else R.string.quiet_header),
                        prayer = prayer,
                        korean = korean,
                        prayed = prayed,
                        saved = saved,
                        onShown = {},
                        onAmen = ::onAmen,
                        onToggleSaved = ::onToggleSaved,
                        primaryText = stringResource(R.string.quiet_done),
                        onPrimary = ::finish,
                        secondaryText = stringResource(R.string.quiet_another),
                        onSecondary = ::nextQuietPrayer,
                        onBack = ::finish,
                    )
                } else {
                    val label = appLabel(targetPackage)
                    PrayerScreen(
                        // Why this screen appeared, shown small at the top.
                        header = when {
                            minutesUsed > 0 -> stringResource(R.string.prayer_header_time, minutesUsed, label)
                            opensThisHour >= 3 -> stringResource(R.string.prayer_header_repeat, label, opensThisHour)
                            else -> stringResource(R.string.prayer_header, label)
                        },
                        prayer = prayer,
                        korean = korean,
                        prayed = prayed,
                        saved = saved,
                        onShown = ::onPrayerShown,
                        onAmen = ::onAmen,
                        onToggleSaved = ::onToggleSaved,
                        // "Close" is the solid button on purpose: it is the choice
                        // the user installed PrayerGate to make more often.
                        primaryText = stringResource(R.string.prayer_close),
                        onPrimary = ::onClose,
                        secondaryText = stringResource(R.string.prayer_continue, label),
                        onSecondary = ::onContinue,
                        secondaryDelaySeconds = continueDelay,
                        secondaryWaitingText = { seconds ->
                            stringResource(R.string.prayer_continue_wait, label, seconds)
                        },
                        onBack = ::onClose,
                    )
                }
            }
        }
    }

    /**
     * Called if a new trigger arrives while this screen already exists.
     * Another report for the app this screen is already pausing is the same
     * visit, so the prayer being read stays and nothing is logged twice.
     * A trigger for a different app (or a time check) starts over.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val samePause = !quiet &&
            intent.getStringExtra(EXTRA_PACKAGE) == targetPackage &&
            intent.getStringExtra(EXTRA_REASON) == reason
        if (samePause) return
        setIntent(intent)
        readIntent(intent)
        prayer = null
        prayed = false
        shownLogged = false
        app.dbThread.execute { log = null }
        loadPrayer()
    }

    private fun readIntent(intent: Intent) {
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        openedAt = intent.getLongExtra(EXTRA_OPENED_AT, System.currentTimeMillis())
        engine = Engine.valueOf(intent.getStringExtra(EXTRA_ENGINE) ?: Engine.USAGE.name)
        reason = intent.getStringExtra(EXTRA_REASON) ?: TriggerLog.REASON_OPEN
        minutesUsed = intent.getIntExtra(EXTRA_MINUTES, 0)
        quiet = intent.getBooleanExtra(EXTRA_QUIET, false)
        requestedPrayerId = intent.getStringExtra(EXTRA_PRAYER_ID)
        fromWidget = intent.getBooleanExtra(EXTRA_FROM_WIDGET, false)
    }

    /** Picks a prayer on the background thread, then hands it to the screen. */
    private fun loadPrayer() {
        val previousId = prayer?.id
        app.dbThread.execute {
            val all = PrayerBook.load(this)
            val logDao = app.database.triggerLogDao()

            // 1. A specific prayer (from the widget or the Saved tab), the first time only.
            // 2. Otherwise the next card from the shuffled deck of the chosen themes.
            val requested = requestedPrayerId?.let { id -> all.firstOrNull { it.id == id } }
            requestedPrayerId = null
            val recent = listOfNotNull(previousId) + logDao.recentPrayerIds()
            val next = requested ?: pickPrayer(all, recent, app.settings.themes)
            val isSaved = app.database.savedPrayerDao().isSaved(next.id) > 0

            // Escalation, only for "app opened" pauses. This pause is not in
            // the log yet, so add one to include it.
            val opens = if (!quiet && reason == TriggerLog.REASON_OPEN) {
                logDao.countOpensSince(targetPackage, System.currentTimeMillis() - 60 * 60_000L) + 1
            } else {
                0
            }
            runOnUiThread {
                prayer = next
                saved = isSaved
                opensThisHour = opens
                continueDelay = continueDelaySeconds(opens)
            }
        }
    }

    /**
     * Called once, right after the prayer's first frame is drawn.
     * This is the moment we measure the delay and create the log row.
     */
    private fun onPrayerShown() {
        if (quiet) return // a quiet prayer is not a pause, so it is not logged
        val shown = prayer ?: return
        if (shownLogged) return
        shownLogged = true
        val delayMs = (System.currentTimeMillis() - openedAt).coerceAtLeast(0)
        app.dbThread.execute {
            val row = TriggerLog(
                packageName = targetPackage,
                openedAt = openedAt,
                delayMs = delayMs,
                engine = engine.name,
                prayerId = shown.id,
                reason = reason,
            )
            log = row.copy(id = app.database.triggerLogDao().insert(row))
        }
    }

    /** The user held Amen to the end. */
    private fun onAmen() {
        if (prayed) return
        prayed = true
        if (quiet) return
        app.dbThread.execute {
            log?.let {
                log = it.copy(prayed = true)
                app.database.triggerLogDao().update(log!!)
            }
            // The streak may have changed: redraw the home-screen widget.
            PrayerGateWidget.refresh(this)
        }
    }

    /** The heart: keep this prayer on the Saved tab, or take it off again. */
    private fun onToggleSaved() {
        val shown = prayer ?: return
        val nowSaved = !saved
        saved = nowSaved
        app.dbThread.execute {
            val dao = app.database.savedPrayerDao()
            if (nowSaved) dao.save(SavedPrayer(shown.id, System.currentTimeMillis())) else dao.remove(shown.id)
        }
    }

    /** Quiet mode: show another prayer on the same screen. */
    private fun nextQuietPrayer() {
        fromWidget = false
        prayed = false
        loadPrayer()
    }

    /** "Continue to [app]": start the cooldown and step out of the way. */
    private fun onContinue() {
        GateController.startCooldown(targetPackage, app.settings.cooldownMinutes)
        saveAction(TriggerLog.ACTION_CONTINUE)
        finish() // the chosen app is right underneath this screen
    }

    /** "Close": send the user to the home screen instead of the app. */
    private fun onClose() {
        saveAction(TriggerLog.ACTION_CLOSE)
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    private fun saveAction(action: String) {
        app.dbThread.execute {
            log?.let {
                log = it.copy(action = action)
                app.database.triggerLogDao().update(log!!)
            }
        }
    }

    /**
     * If the user leaves another way (Home button, recent apps), close this
     * screen so a stale prayer is not waiting later. We keep it when the
     * screen simply turned off, so the prayer is still there after unlocking.
     */
    override fun onStop() {
        super.onStop()
        val screenIsOn = (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
        if (!isFinishing && !isChangingConfigurations && screenIsOn) finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!quiet && !isChangingConfigurations) GateController.prayerClosed(targetPackage)
    }

    /** "com.instagram.android" -> "Instagram". */
    private fun appLabel(packageName: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (e: Exception) {
        packageName
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_OPENED_AT = "openedAt"
        private const val EXTRA_ENGINE = "engine"
        private const val EXTRA_REASON = "reason"
        private const val EXTRA_MINUTES = "minutesUsed"
        private const val EXTRA_QUIET = "quiet"
        private const val EXTRA_PRAYER_ID = "prayerId"
        private const val EXTRA_FROM_WIDGET = "fromWidget"

        /**
         * Opens the prayer screen on its own, not in front of an app: from the
         * Saved tab or the widget. [prayerId] starts with that prayer.
         */
        fun quietIntent(context: Context, prayerId: String? = null, fromWidget: Boolean = false): Intent =
            Intent(context, PrayerActivity::class.java)
                .putExtra(EXTRA_QUIET, true)
                .putExtra(EXTRA_PRAYER_ID, prayerId)
                .putExtra(EXTRA_FROM_WIDGET, fromWidget)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        /** Builds the Intent the detection engines use to open this screen. */
        fun createIntent(
            context: Context,
            packageName: String,
            openedAt: Long,
            engine: Engine,
            reason: String = TriggerLog.REASON_OPEN,
            minutesUsed: Int = 0,
        ): Intent =
            Intent(context, PrayerActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .putExtra(EXTRA_OPENED_AT, openedAt)
                .putExtra(EXTRA_ENGINE, engine.name)
                .putExtra(EXTRA_REASON, reason)
                .putExtra(EXTRA_MINUTES, minutesUsed)
                // NEW_TASK is required when starting a screen from a service.
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}

// ---------------------------------------------------------------------------
// The layout
// ---------------------------------------------------------------------------

/** How long Amen must be held, in milliseconds. */
private const val HOLD_MILLIS = 1500

/** Internal (not private) so AmenHoldTest can draw it and press Amen. */
@Composable
internal fun PrayerScreen(
    header: String,
    prayer: Prayer?,
    korean: Boolean,
    prayed: Boolean,
    saved: Boolean,
    onShown: () -> Unit,
    onAmen: () -> Unit,
    onToggleSaved: () -> Unit,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    onBack: () -> Unit,
    secondaryDelaySeconds: Int = 0,
    secondaryWaitingText: @Composable (Int) -> String = { "" },
) {
    val view = LocalView.current
    val landscape = isLandscapeWide()

    // Where the glow should rise from: the middle of the Amen button.
    var glowOrigin by remember { mutableStateOf(Offset.Zero) }

    // The phone's Back button behaves like "Close".
    BackHandler(onBack = onBack)

    // Tell the Activity once the prayer has really been drawn (for the delay log).
    LaunchedEffect(prayer?.id) {
        if (prayer != null) {
            withFrameNanos { }
            onShown()
        }
    }

    // One soft, firm buzz when Amen is complete.
    LaunchedEffect(prayed) {
        if (prayed) {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.LONG_PRESS
            )
        }
    }

    // The deep olive ground with its large flame (see Ground.kt).
    // Inside it, text is white and the accent is gold.
    OliveGround(Modifier.fillMaxSize()) {
        val softWhite = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)

        // The reward: a slow wash of lamplight that rises from the Amen button.
        LampGlow(play = prayed, origin = glowOrigin)

        Column(Modifier.fillMaxSize().systemBarsPadding().readableWidth(maxWidth = if (landscape) 960.dp else 560.dp).padding(horizontal = 28.dp)) {

            // Top line: the breathing flame and why this screen appeared.
            // The line has the whole width to itself and may wrap to a second
            // line, so long app names are never cut off or squeezed.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = if (landscape) 44.dp else 64.dp)) {
                Flame(breathing = !prayed, celebrate = prayed)
                Spacer(Modifier.width(8.dp))
                SectionLabel(header, Modifier.weight(1f), color = softWhite, maxLines = 2)
            }

            if (landscape) {
                // Sideways phone: the prayer on the left, Amen and the buttons on the right.
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                    Column(
                        Modifier.weight(1.3f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (prayer != null) key(prayer.id) {
                            PrayerBody(prayer, korean, prayed, saved, onToggleSaved, compact = true)
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Exits(
                            prayer, prayed, onAmen, { glowOrigin = it }, primaryText, onPrimary,
                            secondaryText, onSecondary, secondaryDelaySeconds, secondaryWaitingText, softWhite,
                        )
                    }
                }
            } else {
                // Middle: the prayer.
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) {
                    // "key" restarts the entrance animation if a new prayer arrives.
                    if (prayer != null) key(prayer.id) {
                        PrayerBody(prayer, korean, prayed, saved, onToggleSaved, compact = false)
                    }
                }
                // Bottom: Amen, then the two ways out in the same place.
                Exits(
                    prayer, prayed, onAmen, { glowOrigin = it }, primaryText, onPrimary,
                    secondaryText, onSecondary, secondaryDelaySeconds, secondaryWaitingText, softWhite,
                    modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                )
            }
        }
    }
}

/**
 * The prayer itself: the theme as one large pale word (replaced by "Amen"
 * once prayed), the prayer text, and its Bible reference with the heart.
 */
@Composable
private fun PrayerBody(
    prayer: Prayer,
    korean: Boolean,
    prayed: Boolean,
    saved: Boolean,
    onToggleSaved: () -> Unit,
    compact: Boolean,
) {
    // Entrance: everything fades up into place, one item after another.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val shortScreen = LocalConfiguration.current.screenHeightDp < 560

    AnimatedContent(
        targetState = prayed,
        transitionSpec = { fadeIn(tween(700, delayMillis = 200)) togetherWith fadeOut(tween(300)) },
        contentAlignment = Alignment.BottomStart,
        label = "themeOrAmen",
        // A fixed height, so swapping the theme word for "Amen" moves nothing.
        // On short screens the large word is left out to make room for the prayer.
        modifier = Modifier.fillMaxWidth().height(if (shortScreen) 48.dp else 80.dp).enterAnimation(entered, index = 0),
    ) { isPrayed ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
            if (isPrayed) {
                Text(
                    stringResource(R.string.amen_done),
                    style = MaterialTheme.typography.headlineLarge,
                    color = LocalAccent.current.accent,
                    maxLines = 1,
                )
            } else if (!shortScreen) {
                GhostWord(
                    text = stringResource(themeName(prayer.theme)),
                    fontSize = 64.sp,
                    fitWidth = true,
                    // Nudged left so the slanted first letter lines up with the prayer.
                    modifier = Modifier.offset(x = (-2).dp),
                )
            } else {
                SectionLabel(stringResource(themeName(prayer.theme)), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }
    }

    Spacer(Modifier.height(if (compact) 10.dp else 16.dp))
    Text(
        if (korean) keepKoreanWordsTogether(prayer.textKo) else prayer.text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth().enterAnimation(entered, index = 1),
    )
    Spacer(Modifier.height(if (compact) 12.dp else 18.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().enterAnimation(entered, index = 2),
    ) {
        Text(
            prayer.reference(korean),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            modifier = Modifier.weight(1f),
        )
        SaveHeart(saved, onToggleSaved)
    }
}

/**
 * The bottom of the screen. Before Amen: the Amen button and a short hint.
 * After Amen: Close and Continue, in the same place. Both are always laid
 * out, so nothing jumps when one replaces the other.
 */
@Composable
private fun Exits(
    prayer: Prayer?,
    prayed: Boolean,
    onAmen: () -> Unit,
    onAmenPosition: (Offset) -> Unit,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    secondaryDelaySeconds: Int,
    secondaryWaitingText: @Composable (Int) -> String,
    softWhite: Color,
    modifier: Modifier = Modifier,
) {
    val buttonsShown by animateFloatAsState(
        targetValue = if (prayed) 1f else 0f,
        animationSpec = tween(420, delayMillis = if (prayed) 900 else 0),
        label = "buttons",
    )
    val amenShown by animateFloatAsState(
        targetValue = if (prayed || prayer == null) 0f else 1f,
        animationSpec = tween(if (prayed) 500 else 400, delayMillis = if (prayed) 500 else 300),
        label = "amen",
    )
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        // The two layers share one place. Whichever is active sits on top
        // (zIndex), because the one on top receives the touches: a hidden,
        // disabled button still swallows a press, so Close and Continue must
        // be underneath Amen until Amen is done.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .zIndex(if (prayed) 0f else 1f)
                .graphicsLayer { alpha = amenShown }
                .then(if (prayed) Modifier.clearAndSetSemantics { } else Modifier),
        ) {
            if (prayer != null) key(prayer.id) {
                AmenButton(
                    readMillis = readingMillis(prayer),
                    enabled = !prayed,
                    onAmen = onAmen,
                    modifier = Modifier.onGloballyPositioned { onAmenPosition(it.boundsInRoot().center) },
                )
            }
            Text(
                stringResource(R.string.prayer_hint),
                style = MaterialTheme.typography.labelSmall,
                color = softWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        Column(
            modifier = Modifier
                .zIndex(if (prayed) 1f else 0f)
                .graphicsLayer {
                    alpha = buttonsShown
                    translationY = (1f - buttonsShown) * 16.dp.toPx()
                }
                // Hide the invisible buttons from screen readers too.
                .then(if (prayed) Modifier else Modifier.clearAndSetSemantics { }),
        ) {
            // The countdown starts after Amen. Until it reaches zero, the
            // second button shows the seconds left and does nothing.
            var waitLeft by remember(prayer?.id) { mutableIntStateOf(secondaryDelaySeconds) }
            LaunchedEffect(prayed, secondaryDelaySeconds) {
                if (prayed) {
                    waitLeft = secondaryDelaySeconds
                    while (waitLeft > 0) {
                        delay(1000)
                        waitLeft--
                    }
                }
            }
            val secondaryLabel = if (waitLeft > 0) secondaryWaitingText(waitLeft) else secondaryText
            PrimaryButton(primaryText, onPrimary, enabled = prayed)
            QuietButton(secondaryLabel, onSecondary, enabled = prayed && waitLeft == 0)
        }
    }
}

/**
 * The Amen button.
 *
 *  1. Reading: a faint white fill crosses the button over [readMillis], so
 *     the prayer is read before it is prayed. The button does nothing yet.
 *  2. Ready: the button turns solid white and says "Hold to say Amen".
 *  3. Holding: gold fills it from the left over [HOLD_MILLIS]. Letting go
 *     early drains it back.
 *  4. Full: [onAmen].
 *
 * Screen readers cannot hold easily, so for them a double tap counts as Amen
 * once the reading time is over.
 */
@Composable
private fun AmenButton(readMillis: Long, enabled: Boolean, onAmen: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val reading = remember { Animatable(0f) }
    val hold = remember { Animatable(0f) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        reading.animateTo(1f, tween(readMillis.toInt(), easing = LinearEasing))
    }
    val ready = reading.value >= 1f
    val gold = LocalAccent.current.accent
    val scheme = MaterialTheme.colorScheme
    val readyLabel = stringResource(R.string.amen_hold)
    val readingLabel = stringResource(R.string.amen_reading)
    val label = when {
        done -> stringResource(R.string.amen_done)
        ready -> readyLabel
        else -> readingLabel
    }

    fun complete() {
        if (done || !enabled) return
        done = true
        onAmen()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(CircleShape)
            .background(if (ready) scheme.primary else Color.White.copy(alpha = 0.12f))
            .pointerInput(ready, enabled) {
                if (!ready || !enabled) return@pointerInput
                detectTapGestures(onPress = {
                    if (done) return@detectTapGestures
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    val filling = scope.launch {
                        val remaining = ((1f - hold.value) * HOLD_MILLIS).toInt()
                        hold.animateTo(1f, tween(remaining, easing = LinearEasing))
                        complete()
                    }
                    tryAwaitRelease()
                    if (!done) {
                        filling.cancel()
                        scope.launch { hold.animateTo(0f, tween(260, easing = FastOutSlowInEasing)) }
                    }
                })
            }
            .semantics {
                role = Role.Button
                contentDescription = readyLabel
                if (!ready) stateDescription = readingLabel
                onClick(label = readyLabel) {
                    if (ready) complete()
                    ready
                }
            },
    ) {
        // The fills, drawn behind the label.
        Canvas(Modifier.matchParentSize()) {
            if (!ready) {
                drawRect(Color.White.copy(alpha = 0.16f), size = size.copy(width = size.width * reading.value))
            }
            if (hold.value > 0f) {
                drawRect(gold, size = size.copy(width = size.width * hold.value))
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                ready -> scheme.onPrimary
                else -> scheme.onBackground.copy(alpha = 0.75f)
            },
        )
    }
}

/** A heart outline, filled when the prayer is saved. */
@Composable
private fun SaveHeart(saved: Boolean, onToggle: () -> Unit) {
    val color = MaterialTheme.colorScheme.onBackground
    val description = stringResource(if (saved) R.string.prayer_unsave else R.string.prayer_save)
    val fill by animateFloatAsState(if (saved) 1f else 0f, tween(220), label = "heart")
    IconButton(onClick = onToggle, modifier = Modifier.semantics { contentDescription = description }) {
        Canvas(Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val heart = Path().apply {
                moveTo(w * 0.5f, h * 0.88f)
                cubicTo(w * 0.12f, h * 0.62f, w * 0.02f, h * 0.38f, w * 0.12f, h * 0.22f)
                cubicTo(w * 0.24f, h * 0.04f, w * 0.44f, h * 0.08f, w * 0.5f, h * 0.26f)
                cubicTo(w * 0.56f, h * 0.08f, w * 0.76f, h * 0.04f, w * 0.88f, h * 0.22f)
                cubicTo(w * 0.98f, h * 0.38f, w * 0.88f, h * 0.62f, w * 0.5f, h * 0.88f)
                close()
            }
            if (fill > 0f) drawPath(heart, color, alpha = fill)
            drawPath(heart, color.copy(alpha = 0.85f), style = Stroke(1.6.dp.toPx()))
        }
    }
}

/** True on a wide, short screen, such as a phone turned sideways. */
@Composable
private fun isLandscapeWide(): Boolean {
    val c = LocalConfiguration.current
    return c.screenHeightDp < 560 && c.screenWidthDp > c.screenHeightDp && c.screenWidthDp >= 600
}

/** Fades an item in and slides it up a little. Items with a higher [index] start later. */
@Composable
private fun Modifier.enterAnimation(entered: Boolean, index: Int): Modifier {
    val amount by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 520, delayMillis = 80 + index * 90),
        label = "enter",
    )
    return this.graphicsLayer {
        alpha = amount
        translationY = (1f - amount) * 14.dp.toPx()
    }
}

/**
 * A slow wash of gold lamplight that rises from [origin] (the Amen button)
 * and fades, once, when [play] becomes true. With the flame's glow and the
 * buzz, this is the whole celebration: calm, no confetti.
 */
@Composable
private fun LampGlow(play: Boolean, origin: Offset) {
    val gold = LocalAccent.current.accent
    val progress = remember { Animatable(0f) }
    LaunchedEffect(play) {
        if (play) progress.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) else progress.snapTo(0f)
    }
    Canvas(Modifier.fillMaxSize()) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        val radius = size.height * 0.9f * t
        val fade = 1f - t
        drawCircle(gold, radius = radius, center = origin, alpha = 0.14f * fade)
        drawCircle(gold, radius = radius * 0.6f, center = origin, alpha = 0.10f * fade)
    }
}
