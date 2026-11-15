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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayergate.app.R
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.data.Engine
import com.prayergate.app.data.Question
import com.prayergate.app.data.QuestionBank
import com.prayergate.app.data.QuestionProgress
import com.prayergate.app.data.QuestionType
import com.prayergate.app.data.TriggerLog
import com.prayergate.app.data.continueDelaySeconds
import com.prayergate.app.data.pickQuestion
import com.prayergate.app.detection.GateController
import com.prayergate.app.ui.theme.LocalAccent
import com.prayergate.app.ui.theme.PrayerGateTheme
import kotlinx.coroutines.delay

/** A question together with its four choices in the (shuffled) order shown on screen. */
data class ShownQuestion(val question: Question, val choices: List<String>)

/**
 * THE INTERVENTION: the full-screen pause shown before a chosen app.
 *
 * One question, four answers, instant feedback, then "Close" or "Continue".
 *
 * This file has two parts:
 *  1. QuestionActivity - loads the question and saves the results (no layout code).
 *  2. QuestionScreen   - draws everything (no database code).
 */
class QuestionActivity : ComponentActivity() {

    private val app get() = application as PrayerGateApp

    // What triggered this screen (read from the Intent).
    private var targetPackage = ""
    private var openedAt = 0L
    private var engine = Engine.USAGE
    private var reason = TriggerLog.REASON_OPEN
    private var minutesUsed by mutableIntStateOf(0)

    // Practice mode: opened from the Review tab (or the widget), not by an app.
    // Nothing is written to the trigger log, but answers still update the
    // review list: a question answered correctly leaves it.
    private var practice = false
    private var practiceQuestionId: String? = null
    private var fromWidget by mutableStateOf(false)
    private var reviewLeft by mutableIntStateOf(0)

    // Gentle escalation: opening the same app again and again within an hour
    // makes "Continue" wait a few seconds (see continueDelaySeconds).
    private var opensThisHour by mutableIntStateOf(0)
    private var continueDelay by mutableIntStateOf(0)

    private var showKorean = false

    // What the screen is showing. Compose redraws when these change.
    private var shown by mutableStateOf<ShownQuestion?>(null)
    private var selectedChoice by mutableStateOf<String?>(null)
    private var correctInARow by mutableIntStateOf(0)

    /** The database row for this trigger. Only touched on app.dbThread. */
    private var log: TriggerLog? = null
    private var shownLogged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        showKorean = app.settings.koreanExplanations
        readIntent(intent)
        loadQuestion()

        setContent {
            PrayerGateTheme {
                if (practice) {
                    QuestionScreen(
                        header = when {
                            fromWidget -> stringResource(R.string.widget_today)
                            reviewLeft > 0 -> pluralStringResource(R.plurals.practice_header, reviewLeft, reviewLeft)
                            else -> stringResource(R.string.practice_header_none)
                        },
                        shown = shown,
                        showKorean = showKorean,
                        selectedChoice = selectedChoice,
                        correctInARow = 0,
                        onShown = {},
                        onAnswer = ::onAnswer,
                        primaryText = stringResource(R.string.practice_next),
                        onPrimary = ::nextPracticeQuestion,
                        secondaryText = stringResource(R.string.practice_done),
                        onSecondary = ::finish,
                        onBack = ::finish,
                    )
                } else {
                    val label = appLabel(targetPackage)
                    QuestionScreen(
                        // Why this screen appeared, shown small at the top.
                        header = when {
                            minutesUsed > 0 -> stringResource(R.string.question_header_time, minutesUsed, label)
                            opensThisHour >= 3 -> stringResource(R.string.question_header_repeat, label, opensThisHour)
                            else -> stringResource(R.string.question_header, label)
                        },
                        shown = shown,
                        showKorean = showKorean,
                        selectedChoice = selectedChoice,
                        correctInARow = correctInARow,
                        onShown = ::onQuestionShown,
                        onAnswer = ::onAnswer,
                        // "Close" is the solid button on purpose: it is the choice
                        // the user installed PrayerGate to make more often.
                        primaryText = stringResource(R.string.question_close),
                        onPrimary = ::onClose,
                        secondaryText = stringResource(R.string.question_continue, label),
                        onSecondary = ::onContinue,
                        secondaryDelaySeconds = continueDelay,
                        secondaryWaitingText = { seconds ->
                            stringResource(R.string.question_continue_wait, label, seconds)
                        },
                        onBack = ::onClose,
                    )
                }
            }
        }
    }

    /** Called if a new trigger arrives while this screen already exists: start over. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
        shown = null
        selectedChoice = null
        correctInARow = 0
        shownLogged = false
        app.dbThread.execute { log = null }
        loadQuestion()
    }

    private fun readIntent(intent: Intent) {
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        openedAt = intent.getLongExtra(EXTRA_OPENED_AT, System.currentTimeMillis())
        engine = Engine.valueOf(intent.getStringExtra(EXTRA_ENGINE) ?: Engine.USAGE.name)
        reason = intent.getStringExtra(EXTRA_REASON) ?: TriggerLog.REASON_OPEN
        minutesUsed = intent.getIntExtra(EXTRA_MINUTES, 0)
        practice = intent.getBooleanExtra(EXTRA_PRACTICE, false)
        practiceQuestionId = intent.getStringExtra(EXTRA_QUESTION_ID)
        fromWidget = intent.getBooleanExtra(EXTRA_FROM_WIDGET, false)
    }

    /** Picks a question on the background thread, then hands it to the screen. */
    private fun loadQuestion() {
        val previousId = shown?.question?.id
        app.dbThread.execute {
            val all = QuestionBank.load(this)
            val progress = app.database.questionProgressDao().getAll()
            val toReview = progress.filter { it.lastAnswerWrong }.map { it.questionId }.toSet()

            val question = if (practice) {
                // 1. A specific question (from the widget), the first time only.
                // 2. Otherwise one from the review list, not the one just shown.
                // 3. If the review list is empty, a normal question.
                val requested = practiceQuestionId?.let { id -> all.firstOrNull { it.id == id } }
                practiceQuestionId = null
                val reviewPool = all.filter { it.id in toReview && it.id != previousId }
                requested
                    ?: reviewPool.randomOrNull()
                    ?: pickQuestion(all, progress, app.settings.difficulty, app.settings.questionType)
            } else {
                pickQuestion(
                    all = all,
                    progress = progress,
                    difficulty = app.settings.difficulty,
                    type = app.settings.questionType,
                )
            }
            val left = toReview.size

            // Escalation, only for "app opened" triggers. This trigger is not in
            // the log yet, so add one to include it.
            val opens = if (!practice && reason == TriggerLog.REASON_OPEN) {
                app.database.triggerLogDao().countOpensSince(targetPackage, System.currentTimeMillis() - 60 * 60_000L) + 1
            } else {
                0
            }
            // Shuffle the choices so the right answer is not always in the same place.
            val shuffled = ShownQuestion(question, question.choices.shuffled())
            runOnUiThread {
                shown = shuffled
                reviewLeft = left
                opensThisHour = opens
                continueDelay = continueDelaySeconds(opens)
            }
        }
    }

    /**
     * Called once, right after the question's first frame is drawn.
     * This is the moment we measure the delay and create the log row.
     */
    private fun onQuestionShown() {
        if (practice) return // practice is not a trigger, so it is not logged
        val question = shown?.question ?: return
        if (shownLogged) return
        shownLogged = true
        val delayMs = (System.currentTimeMillis() - openedAt).coerceAtLeast(0)
        app.dbThread.execute {
            val row = TriggerLog(
                packageName = targetPackage,
                openedAt = openedAt,
                delayMs = delayMs,
                engine = engine.name,
                questionId = question.id,
                reason = reason,
            )
            log = row.copy(id = app.database.triggerLogDao().insert(row))
        }
    }

    private fun onAnswer(choice: String) {
        val question = shown?.question ?: return
        if (selectedChoice != null) return // already answered
        selectedChoice = choice
        val correct = choice == question.correctAnswer

        app.dbThread.execute {
            // 1. Remember how the user did on this question (for "wrong ones come back sooner").
            val progressDao = app.database.questionProgressDao()
            val old = progressDao.get(question.id)
            progressDao.save(
                QuestionProgress(
                    questionId = question.id,
                    timesSeen = (old?.timesSeen ?: 0) + 1,
                    timesWrong = (old?.timesWrong ?: 0) + if (correct) 0 else 1,
                    lastAnswerWrong = !correct,
                    lastSeenAt = System.currentTimeMillis(),
                )
            )

            // In practice mode, that is all: update the review count and stop.
            if (practice) {
                val left = progressDao.getAll().count { it.lastAnswerWrong }
                runOnUiThread { reviewLeft = left }
                return@execute
            }

            // The streak may have changed: redraw the home-screen widget.
            PrayerGateWidget.refresh(this)

            // 2. Write right/wrong into the log row.
            val logDao = app.database.triggerLogDao()
            log?.let {
                log = it.copy(correct = correct)
                logDao.update(log!!)
            }

            // 3. Count the run of correct answers, for the "3 in a row" badge.
            if (correct) {
                val run = logDao.recentAnswers().takeWhile { it }.size
                runOnUiThread { correctInARow = run }
            }
        }
    }

    /** Practice mode: show the next question on the same screen. */
    private fun nextPracticeQuestion() {
        fromWidget = false // after today's question, carry on with the review list
        selectedChoice = null
        loadQuestion()
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
     * screen so a stale question is not waiting later. We keep it when the
     * screen simply turned off, so the question is still there after unlocking.
     */
    override fun onStop() {
        super.onStop()
        val screenIsOn = (getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive
        if (!isFinishing && !isChangingConfigurations && screenIsOn) finish()
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
        private const val EXTRA_PRACTICE = "practice"
        private const val EXTRA_QUESTION_ID = "questionId"
        private const val EXTRA_FROM_WIDGET = "fromWidget"

        /**
         * Opens the question screen in practice mode, to work through the
         * questions the user got wrong. [questionId] starts with that question.
         */
        fun practiceIntent(context: Context, questionId: String? = null, fromWidget: Boolean = false): Intent =
            Intent(context, QuestionActivity::class.java)
                .putExtra(EXTRA_PRACTICE, true)
                .putExtra(EXTRA_QUESTION_ID, questionId)
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
            Intent(context, QuestionActivity::class.java)
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

/** How one answer card should look. */
private enum class AnswerState { IDLE, CORRECT, WRONG, DIMMED }

@Composable
private fun QuestionScreen(
    header: String,
    shown: ShownQuestion?,
    showKorean: Boolean,
    selectedChoice: String?,
    correctInARow: Int,
    onShown: () -> Unit,
    onAnswer: (String) -> Unit,
    primaryText: String,
    onPrimary: () -> Unit,
    secondaryText: String,
    onSecondary: () -> Unit,
    onBack: () -> Unit,
    secondaryDelaySeconds: Int = 0,
    secondaryWaitingText: @Composable (Int) -> String = { "" },
) {
    val view = LocalView.current
    val answered = selectedChoice != null
    val wasCorrect = answered && selectedChoice == shown?.question?.correctAnswer

    // Where the confetti should burst from: the middle of the tapped card.
    var burstOrigin by remember { mutableStateOf(Offset.Zero) }

    // The phone's Back button behaves like "Close".
    BackHandler(onBack = onBack)

    // Tell the Activity once the question has really been drawn (for the delay log).
    LaunchedEffect(shown?.question?.id) {
        if (shown != null) {
            withFrameNanos { }
            onShown()
        }
    }

    // A short buzz when the answer lands: a happy one for right, a soft one for wrong.
    LaunchedEffect(answered) {
        if (answered) {
            val buzz = when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> HapticFeedbackConstants.LONG_PRESS
                wasCorrect -> HapticFeedbackConstants.CONFIRM
                else -> HapticFeedbackConstants.REJECT
            }
            view.performHapticFeedback(buzz)
        }
    }

    // The deep blue ground with its giant full stop (see Ground.kt).
    // Inside it, text is white and the accent is citron.
    LapisGround(Modifier.fillMaxSize()) {
        val softWhite = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)

        // The reward wash: a ring of citron light that spreads from the right answer.
        RewardRipple(play = wasCorrect, origin = burstOrigin)

        Column(Modifier.fillMaxSize().systemBarsPadding().readableWidth(maxWidth = if (isLandscapeWide()) 960.dp else 560.dp).padding(horizontal = 28.dp)) {

            // Top line: the breathing full stop and why this screen appeared.
            // The line has the whole width to itself and may wrap to a second
            // line, so long app names are never cut off or squeezed.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = if (isLandscapeWide()) 44.dp else 64.dp)) {
                FullStop(breathing = !answered, celebrate = wasCorrect)
                Spacer(Modifier.width(8.dp))
                SectionLabel(header, Modifier.weight(1f), color = softWhite, maxLines = 2)
            }

            // Middle: the question and its answers.
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
            ) {
                if (shown != null) {
                    // "key" restarts the entrance animation if a new question arrives.
                    key(shown.question.id) {
                        QuestionBody(
                            correctInARow = correctInARow,
                            shown = shown,
                            showKorean = showKorean,
                            selectedChoice = selectedChoice,
                            wasCorrect = wasCorrect,
                            onAnswer = onAnswer,
                            onChosenPosition = { burstOrigin = it },
                        )
                    }
                }
            }

            // Bottom: the two ways out. They are always laid out (so nothing
            // jumps when they appear) but stay invisible and untappable
            // until the user has answered. Before that, a quiet note sits in
            // their place so the user knows how to get past this screen.
            val buttonsShown by animateFloatAsState(
                targetValue = if (answered) 1f else 0f,
                animationSpec = tween(320, delayMillis = if (answered) 380 else 0),
                label = "buttons",
            )
            val hintShown by animateFloatAsState(
                targetValue = if (answered || shown == null) 0f else 1f,
                animationSpec = tween(if (answered) 120 else 400, delayMillis = if (answered) 0 else 900),
                label = "hint",
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)) {
                Text(
                    stringResource(R.string.question_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = softWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { alpha = hintShown },
                )
                Column(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = buttonsShown
                            translationY = (1f - buttonsShown) * 16.dp.toPx()
                        }
                        // Hide the invisible buttons from screen readers too.
                        .then(if (answered) Modifier else Modifier.clearAndSetSemantics { }),
                ) {
                    // The countdown starts when the user answers. Until it reaches
                    // zero, the second button shows the seconds left and does nothing.
                    var waitLeft by remember(shown?.question?.id) { mutableIntStateOf(secondaryDelaySeconds) }
                    LaunchedEffect(answered, secondaryDelaySeconds) {
                        if (answered) {
                            waitLeft = secondaryDelaySeconds
                            while (waitLeft > 0) {
                                delay(1000)
                                waitLeft--
                            }
                        }
                    }
                    val secondaryLabel = if (waitLeft > 0) secondaryWaitingText(waitLeft) else secondaryText
                    if (isLandscapeWide()) {
                        // Sideways phone: the two buttons share one row to save height.
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            PrimaryButton(primaryText, onPrimary, enabled = answered, modifier = Modifier.weight(1f))
                            QuietButton(secondaryLabel, onSecondary, enabled = answered && waitLeft == 0, modifier = Modifier.weight(1f))
                        }
                    } else {
                        PrimaryButton(primaryText, onPrimary, enabled = answered)
                        QuietButton(secondaryLabel, onSecondary, enabled = answered && waitLeft == 0)
                    }
                }
            }
        }

        // The celebration, drawn on top of everything. Only for correct answers.
        ConfettiBurst(
            burstKey = if (wasCorrect) shown?.question?.id else null,
            origin = burstOrigin,
        )
    }
}

@Composable
private fun QuestionBody(
    correctInARow: Int,
    shown: ShownQuestion,
    showKorean: Boolean,
    selectedChoice: String?,
    wasCorrect: Boolean,
    onAnswer: (String) -> Unit,
    onChosenPosition: (Offset) -> Unit,
) {
    val question = shown.question
    val answered = selectedChoice != null
    // Short screens, such as a phone in landscape.
    val configuration = LocalConfiguration.current
    val compact = configuration.screenHeightDp < 560
    val sideBySide = compact && configuration.screenWidthDp > configuration.screenHeightDp && configuration.screenWidthDp >= 600

    // Entrance: everything fades up into place, one item after another.
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    // The three parts of the screen. On a phone held upright they stack;
    // on a wide, short screen (a phone turned sideways) the question sits on
    // the left and the answers on the right, so everything fits without scrolling.
    val questionPart: @Composable () -> Unit = {
        // The line above the question. Before answering it names the question type
        // as one large, pale italic word. After answering, the verdict takes its
        // place ("Correct." / "Not quite.").
        AnimatedContent(
            targetState = answered,
            transitionSpec = {
                (fadeIn(tween(160)) + scaleIn(
                    initialScale = 0.7f,
                    animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
                )) togetherWith fadeOut(tween(100))
            },
            contentAlignment = Alignment.BottomStart,
            label = "verdict",
            // A fixed height, so swapping the large word for the verdict moves nothing.
            // On short screens (a phone turned sideways) the large word is left out
            // and the space shrinks, so more of the question and answers fit.
            modifier = Modifier.fillMaxWidth().height(if (compact) 52.dp else 88.dp).enterAnimation(entered, index = 0),
        ) { isAnswered ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
                if (!isAnswered && !compact) {
                    GhostWord(
                        text = stringResource(
                            if (question.type == QuestionType.VOCABULARY) R.string.ghost_vocabulary else R.string.ghost_grammar
                        ),
                        fontSize = 76.sp,
                        fitWidth = true,
                        // Nudged left so the slanted first letter lines up with the question.
                        modifier = Modifier.offset(x = (-3).dp),
                    )
                } else if (isAnswered) {
                    // The verdict, and beside it the streak of correct answers
                    // (from two in a row). The streak sits with the moment it rewards.
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (wasCorrect) withAccentFullStop(stringResource(R.string.question_right))
                            else withAccentFullStop(stringResource(R.string.question_wrong)),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                        )
                        AnimatedVisibility(
                            visible = wasCorrect && correctInARow >= 2,
                            enter = fadeIn(tween(200, delayMillis = 250)) +
                                slideInVertically(spring(dampingRatio = 0.5f)) { it / 2 },
                            modifier = Modifier.padding(start = 14.dp, bottom = 9.dp),
                        ) {
                            SectionLabel(
                                stringResource(R.string.question_in_a_row, correctInARow),
                                color = LocalAccent.current.accent,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            question.question,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth().enterAnimation(entered, index = 1),
        )
    }
    val answersPart: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)) {
            shown.choices.forEachIndexed { index, choice ->
                val state = when {
                    !answered -> AnswerState.IDLE
                    choice == question.correctAnswer -> AnswerState.CORRECT
                    choice == selectedChoice -> AnswerState.WRONG
                    else -> AnswerState.DIMMED
                }
                AnswerCard(
                    compact = compact,
                    letter = ('a' + index).toString(),
                    text = choice,
                    state = state,
                    wasChosen = choice == selectedChoice,
                    onClick = { onAnswer(choice) },
                    modifier = Modifier
                        .enterAnimation(entered, index = 2 + index)
                        .onGloballyPositioned {
                            // Remember where the chosen card is, for the confetti.
                            if (choice == selectedChoice) onChosenPosition(it.boundsInRoot().center)
                        },
                )
            }
        }

    }
    val explanationPart: @Composable () -> Unit = {
        // The one-line explanation, in italics like a dictionary note. Its space
        // is reserved from the start, so nothing moves when it fades in.
        val explanationShown by animateFloatAsState(
            targetValue = if (answered) 1f else 0f,
            animationSpec = tween(320, delayMillis = if (answered) 220 else 0),
            label = "explanation",
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 22.dp, bottom = 4.dp)
                .graphicsLayer { alpha = explanationShown }
                .then(if (answered) Modifier else Modifier.clearAndSetSemantics { }),
        ) {
            Text(
                question.explanation,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            )
            // The same explanation in Korean, if switched on in Settings.
            val korean = question.explanationKo
            if (showKorean && korean != null) {
                Text(
                    keepKoreanWordsTogether(korean),
                    style = koreanStyle(),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }

    if (sideBySide) {
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f)) {
                questionPart()
                explanationPart()
            }
            Column(Modifier.weight(1f).padding(top = 12.dp)) { answersPart() }
        }
    } else {
        questionPart()
        Spacer(Modifier.height(28.dp))
        answersPart()
        explanationPart()
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
        animationSpec = tween(durationMillis = 380, delayMillis = 60 + index * 65),
        label = "enter",
    )
    return this.graphicsLayer {
        alpha = amount
        translationY = (1f - amount) * 18.dp.toPx()
    }
}

/**
 * One answer: a white card with a small italic letter (a, b, c, d).
 *
 *  - The right answer fills with citron and lifts off the page.
 *  - A wrong pick is struck through, like a crossed-out word.
 *  - The others fade back.
 */
@Composable
private fun AnswerCard(
    compact: Boolean,
    letter: String,
    text: String,
    state: AnswerState,
    wasChosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium

    val container by animateColorAsState(
        if (state == AnswerState.CORRECT) accent.accent else scheme.surface,
        animationSpec = tween(260),
        label = "answerContainer",
    )
    val textColor by animateColorAsState(
        when (state) {
            AnswerState.CORRECT -> accent.onAccent
            AnswerState.WRONG -> scheme.onSurfaceVariant
            else -> scheme.onSurface
        },
        label = "answerText",
    )
    // The cards that were not chosen fade back into the blue.
    val contentAlpha by animateFloatAsState(
        when (state) {
            AnswerState.DIMMED -> 0.22f
            AnswerState.WRONG -> 0.7f
            else -> 1f
        },
        label = "answerAlpha",
    )
    // The right answer rises; the faded ones sink flat onto the page.
    val elevation by animateDpAsState(
        when (state) {
            AnswerState.CORRECT -> 22.dp
            AnswerState.DIMMED, AnswerState.WRONG -> 0.dp
            else -> 10.dp
        },
        label = "answerElevation",
    )

    // The "pop" for a correct pick, and the "shake" for a wrong one.
    val scale = remember { Animatable(1f) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state == AnswerState.CORRECT && wasChosen) {
            scale.animateTo(1.06f, tween(110))
            scale.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessMedium))
        }
        if (state == AnswerState.WRONG) {
            for (target in listOf(-9f, 8f, -5f, 3f, 0f)) shake.animateTo(target, tween(55))
        }
    }

    Surface(
        onClick = onClick,
        enabled = state == AnswerState.IDLE,
        shape = shape,
        color = container,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = shake.value.dp.toPx()
                alpha = contentAlpha
            }
            .shadow(
                elevation = elevation,
                shape = shape,
                // The shadow under the right answer is tinted citron, so it seems to glow.
                ambientColor = if (state == AnswerState.CORRECT) accent.accent else Color.Black.copy(alpha = 0.35f),
                spotColor = if (state == AnswerState.CORRECT) accent.accent else Color.Black.copy(alpha = 0.35f),
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            // Slightly shorter cards on short screens, so all four answers fit.
            modifier = Modifier.heightIn(min = if (compact) 50.dp else 62.dp).padding(horizontal = 20.dp, vertical = if (compact) 9.dp else 14.dp),
        ) {
            Text(
                letter,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.55f),
                modifier = Modifier.width(26.dp),
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                textDecoration = if (state == AnswerState.WRONG) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f),
            )
            // A tick or cross, so colour is never the only signal.
            if (state == AnswerState.CORRECT) ResultMark(isTick = true, color = accent.onAccent)
            if (state == AnswerState.WRONG) ResultMark(isTick = false, color = scheme.onSurfaceVariant)
        }
    }
}

/**
 * A soft ring of citron light that spreads from [origin] across the whole
 * screen once, when [play] becomes true. With the confetti, this is the
 * big "you got it" moment.
 */
@Composable
private fun RewardRipple(play: Boolean, origin: Offset) {
    val accent = LocalAccent.current.accent
    val progress = remember { Animatable(0f) }
    LaunchedEffect(play) {
        if (play) progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) else progress.snapTo(0f)
    }
    Canvas(Modifier.fillMaxSize()) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        // Big enough to pass every corner of the screen.
        val maxRadius = size.height * 1.1f
        val fade = 1f - t
        // A filled wash, and a brighter edge travelling in front of it.
        drawCircle(accent, radius = maxRadius * t, center = origin, alpha = 0.16f * fade)
        drawCircle(accent, radius = maxRadius * t, center = origin, alpha = 0.5f * fade, style = Stroke(2.dp.toPx()))
    }
}

/** A fine tick or cross that draws itself in. */
@Composable
private fun ResultMark(isTick: Boolean, color: Color) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(300, delayMillis = 80)) }

    Canvas(Modifier.padding(start = 12.dp).size(20.dp)) {
        val w = size.width
        val stroke = 1.8.dp.toPx()
        val p = progress.value
        if (isTick) {
            // Two strokes: the short one first (0 to 0.4), then the long one.
            val a = Offset(w * 0.12f, w * 0.55f)
            val b = Offset(w * 0.40f, w * 0.80f)
            val c = Offset(w * 0.90f, w * 0.22f)
            val first = (p / 0.4f).coerceIn(0f, 1f)
            val second = ((p - 0.4f) / 0.6f).coerceIn(0f, 1f)
            drawLine(color, a, a + (b - a) * first, stroke, StrokeCap.Round)
            if (second > 0f) drawLine(color, b, b + (c - b) * second, stroke, StrokeCap.Round)
        } else {
            val a = Offset(w * 0.22f, w * 0.22f)
            val b = Offset(w * 0.78f, w * 0.78f)
            val c = Offset(w * 0.78f, w * 0.22f)
            val d = Offset(w * 0.22f, w * 0.78f)
            drawLine(color, a, a + (b - a) * p, stroke, StrokeCap.Round)
            drawLine(color, c, c + (d - c) * p, stroke, StrokeCap.Round)
        }
    }
}
