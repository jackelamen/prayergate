package com.prayergate.app

import com.prayergate.app.data.Difficulty
import com.prayergate.app.data.QuestionProgress
import com.prayergate.app.data.QuestionType
import com.prayergate.app.data.TriggerLog
import com.prayergate.app.data.computeStats
import com.prayergate.app.data.adviseDifficulty
import com.prayergate.app.data.computeStreak
import com.prayergate.app.data.continueDelaySeconds
import com.prayergate.app.data.questionOfTheDay
import com.prayergate.app.data.weekByApp
import com.prayergate.app.data.difficultyOfId
import com.prayergate.app.data.parseQuestions
import com.prayergate.app.data.percentile
import com.prayergate.app.data.pickQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Tests for the plain-Kotlin logic. Run them with:  ./gradlew test
 * They run on your computer; no phone is needed.
 */
class LogicTest {

    private val questions = parseQuestions(File("src/main/assets/questions.json").readText())

    @Test
    fun questionFile_has300WellFormedQuestions() {
        assertEquals(300, questions.size)
        assertEquals(300, questions.map { it.id }.toSet().size)
        assertEquals(300, questions.map { it.question }.toSet().size)
        for (difficulty in Difficulty.entries) {
            val group = questions.filter { it.difficulty == difficulty }
            assertEquals(100, group.size)
            assertEquals(50, group.count { it.type == QuestionType.VOCABULARY })
            assertEquals(50, group.count { it.type == QuestionType.GRAMMAR })
        }
        for (q in questions) {
            assertEquals("id matches level: ${q.id}", q.difficulty, difficultyOfId(q.id))
            assertTrue("has Korean explanation: ${q.id}", !q.explanationKo.isNullOrBlank())
        }
        for (q in questions) {
            assertEquals("4 choices: ${q.id}", 4, q.choices.size)
            assertEquals("no duplicate choices: ${q.id}", 4, q.choices.toSet().size)
            assertTrue("valid answer: ${q.id}", q.answerIndex in 0..3)
            assertTrue("has explanation: ${q.id}", q.explanation.isNotBlank())
        }
    }

    @Test
    fun pickQuestion_respectsSettings_andNeverRepeatsTheLastOne() {
        val last = questions.first { it.difficulty == Difficulty.HARD && it.type == QuestionType.GRAMMAR }
        val progress = listOf(QuestionProgress(last.id, 1, 0, lastAnswerWrong = false, lastSeenAt = 10))
        repeat(500) {
            val picked = pickQuestion(questions, progress, Difficulty.HARD, QuestionType.GRAMMAR)
            assertEquals(Difficulty.HARD, picked.difficulty)
            assertEquals(QuestionType.GRAMMAR, picked.type)
            assertTrue(picked.id != last.id)
        }
    }

    @Test
    fun pickQuestion_showsWrongAnswersMoreOften() {
        val pool = questions.filter { it.difficulty == Difficulty.EASY }
        // Every easy question was answered correctly, except one.
        val missed = pool[3]
        val progress = pool.mapIndexed { index, q ->
            QuestionProgress(q.id, 1, 0, lastAnswerWrong = q.id == missed.id, lastSeenAt = index.toLong())
        }
        val random = Random(1)
        val picks = List(5000) { pickQuestion(questions, progress, Difficulty.EASY, QuestionType.BOTH, random) }
        val missedCount = picks.count { it.id == missed.id }
        val typicalCount = picks.count { it.id == pool[5].id }
        assertTrue("missed=$missedCount typical=$typicalCount", missedCount > typicalCount * 3)
    }

    @Test
    fun percentile_medianAndP90() {
        val values = (1L..10L).map { it * 100 } // 100, 200, ... 1000
        assertEquals(500L, percentile(values, 50))
        assertEquals(900L, percentile(values, 90))
        assertEquals(42L, percentile(listOf(42L), 90))
        assertNull(percentile(emptyList(), 50))
    }

    @Test
    fun streak_countsDaysInARow() {
        val today = LocalDate.of(2026, 3, 10)
        fun days(vararg ago: Long) = ago.map { today.minusDays(it) }.toSet()
        assertEquals(0, computeStreak(emptySet(), today))
        assertEquals(3, computeStreak(days(0, 1, 2), today))
        assertEquals(2, computeStreak(days(1, 2), today))     // nothing yet today: still alive
        assertEquals(0, computeStreak(days(2, 3), today))     // missed yesterday: broken
        assertEquals(1, computeStreak(days(0, 2, 3), today))  // gap after today
    }

    @Test
    fun weekByApp_groupsAndSortsBusiestFirst() {
        val day = 24 * 60 * 60 * 1000L
        val now = 100 * day
        fun row(app: String, action: String, at: Long = now - day) =
            TriggerLog(packageName = app, openedAt = at, delayMs = 1, engine = "USAGE", questionId = "ev01", action = action)
        val week = weekByApp(
            listOf(
                row("insta", TriggerLog.ACTION_CLOSE), row("insta", TriggerLog.ACTION_CONTINUE), row("insta", TriggerLog.ACTION_CLOSE),
                row("tiktok", TriggerLog.ACTION_CONTINUE),
                row("youtube", TriggerLog.ACTION_CLOSE, at = now - 8 * day), // older than a week
            ),
            now,
        )
        assertEquals(listOf("insta", "tiktok"), week.map { it.packageName })
        assertEquals(3, week[0].pauses)
        assertEquals(2, week[0].closes)
    }

    @Test
    fun questionOfTheDay_isStableForADayAndChangesDaily() {
        val first = questionOfTheDay(questions, Difficulty.MEDIUM, QuestionType.BOTH, epochDay = 20_000)
        assertEquals(first, questionOfTheDay(questions, Difficulty.MEDIUM, QuestionType.BOTH, epochDay = 20_000))
        assertEquals(Difficulty.MEDIUM, first.difficulty)
        val week = (20_000L until 20_007L).map { questionOfTheDay(questions, Difficulty.MEDIUM, QuestionType.BOTH, it).id }
        assertEquals(7, week.toSet().size) // a different question every day of the week
        val grammarOnly = questionOfTheDay(questions, Difficulty.HARD, QuestionType.GRAMMAR, epochDay = 5)
        assertEquals(QuestionType.GRAMMAR, grammarOnly.type)
    }

    @Test
    fun escalation_waitsLongerTheMoreOftenTheAppIsOpened() {
        assertEquals(0, continueDelaySeconds(1))
        assertEquals(0, continueDelaySeconds(2))
        assertEquals(5, continueDelaySeconds(3))
        assertEquals(5, continueDelaySeconds(4))
        assertEquals(10, continueDelaySeconds(5))
        assertEquals(10, continueDelaySeconds(12))
    }

    @Test
    fun advice_suggestsMovingUpOrDown() {
        val day = 24 * 60 * 60 * 1000L
        val now = 100 * day
        fun answers(prefix: String, right: Int, wrong: Int, at: Long = now - day) =
            List(right) { TriggerLog(packageName = "a", openedAt = at, delayMs = 1, engine = "USAGE", questionId = "${prefix}01", correct = true) } +
                List(wrong) { TriggerLog(packageName = "a", openedAt = at, delayMs = 1, engine = "USAGE", questionId = "${prefix}01", correct = false) }

        // 18 of 20 right on Easy (90%): move up to Medium.
        val up = adviseDifficulty(answers("ev", 18, 2), Difficulty.EASY, now)!!
        assertEquals(Difficulty.MEDIUM, up.suggested)
        assertEquals(90, up.percentCorrect)

        // 9 of 20 right on Medium (45%): move down to Easy.
        assertEquals(Difficulty.EASY, adviseDifficulty(answers("mg", 9, 11), Difficulty.MEDIUM, now)!!.suggested)

        // In between, too few answers, already at the top, or answers too old: no advice.
        assertNull(adviseDifficulty(answers("ev", 14, 6), Difficulty.EASY, now))
        assertNull(adviseDifficulty(answers("ev", 10, 0), Difficulty.EASY, now))
        assertNull(adviseDifficulty(answers("hv", 20, 0), Difficulty.HARD, now))
        assertNull(adviseDifficulty(answers("ev", 20, 0, at = now - 8 * day), Difficulty.EASY, now))
        // Answers at another level do not count.
        assertNull(adviseDifficulty(answers("mv", 20, 0), Difficulty.EASY, now))
    }

    @Test
    fun stats_countAnswersAndCloses() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 3, 10)
        val noon = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        fun log(correct: Boolean?, action: String) =
            TriggerLog(packageName = "a", openedAt = noon, delayMs = 1, engine = "USAGE", questionId = "q", correct = correct, action = action)
        val stats = computeStats(
            listOf(
                log(true, TriggerLog.ACTION_CONTINUE),
                log(true, TriggerLog.ACTION_CLOSE),
                log(false, TriggerLog.ACTION_CLOSE),
                log(null, TriggerLog.ACTION_NONE),
            ),
            today, zone,
        )
        assertEquals(3, stats.answered)
        assertEquals(67, stats.percentCorrect)
        assertEquals(2, stats.closes)
        assertEquals(1, stats.streakDays)
    }
}
