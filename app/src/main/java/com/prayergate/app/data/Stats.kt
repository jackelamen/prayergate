package com.prayergate.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/*
 * Small, plain calculations used by the Stats and Debug screens.
 * They take a list of log rows and return numbers. No Android code here,
 * so they are easy to read and easy to unit test.
 */

/** The numbers shown on the Stats screen. */
data class Stats(
    val answered: Int,        // questions answered
    val percentCorrect: Int,  // 0-100
    val closes: Int,          // times the user chose Close instead of Continue
    val streakDays: Int,      // days in a row with at least one answer
)

fun computeStats(
    logs: List<TriggerLog>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): Stats {
    val answeredLogs = logs.filter { it.correct != null }
    val correct = answeredLogs.count { it.correct == true }
    val percent = if (answeredLogs.isEmpty()) 0 else Math.round(correct * 100f / answeredLogs.size)

    val days = answeredDays(logs, zone)

    return Stats(
        answered = answeredLogs.size,
        percentCorrect = percent,
        closes = logs.count { it.action == TriggerLog.ACTION_CLOSE },
        streakDays = computeStreak(days, today),
    )
}

/** The set of calendar days on which the user answered at least one question. */
fun answeredDays(logs: List<TriggerLog>, zone: ZoneId = ZoneId.systemDefault()): Set<LocalDate> =
    logs.filter { it.correct != null }
        .map { Instant.ofEpochMilli(it.openedAt).atZone(zone).toLocalDate() }
        .toSet()

/**
 * Counts how many days in a row the user has answered a question.
 * The streak is still alive if the last answer was yesterday
 * (today is not over yet), but it is 0 if a whole day was missed.
 */
fun computeStreak(days: Set<LocalDate>, today: LocalDate): Int {
    var day = if (today in days) today else today.minusDays(1)
    var streak = 0
    while (day in days) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

/** One app's week: how many times PrayerGate paused it, and how often the user chose Close. */
data class AppWeek(val packageName: String, val pauses: Int, val closes: Int)

/**
 * Groups the last 7 days of triggers by app, busiest first.
 * Every trigger counts as a pause (app opened or time check).
 */
fun weekByApp(logs: List<TriggerLog>, now: Long): List<AppWeek> {
    val weekAgo = now - 7 * 24 * 60 * 60 * 1000L
    return logs.filter { it.openedAt >= weekAgo }
        .groupBy { it.packageName }
        .map { (packageName, rows) ->
            AppWeek(packageName, pauses = rows.size, closes = rows.count { it.action == TriggerLog.ACTION_CLOSE })
        }
        .sortedWith(compareByDescending<AppWeek> { it.pauses }.thenBy { it.packageName })
}

/**
 * Gentle escalation: the more often the user opens the same app within an
 * hour, the longer they wait before "Continue" can be tapped.
 * "Close" is always available straight away; this is a pause, not a block.
 *
 * @param opensThisHour how many times the app was opened in the last hour,
 *                      including this time.
 * @return seconds to wait before "Continue" works.
 */
fun continueDelaySeconds(opensThisHour: Int): Int = when {
    opensThisHour >= 5 -> 10
    opensThisHour >= 3 -> 5
    else -> 0
}

/** A suggestion to change the difficulty (see [adviseDifficulty]). */
data class DifficultyAdvice(
    val suggested: Difficulty,
    val percentCorrect: Int,
    val moveUp: Boolean,
)

/** How many answers at the current level are needed before giving advice. */
const val ADVICE_MIN_ANSWERS = 20

/**
 * Looks at the last 7 days of answers at the [current] difficulty.
 *  - 85% or more correct: suggest the next level up.
 *  - 50% or less correct: suggest the level below.
 * Returns null when there are fewer than [ADVICE_MIN_ANSWERS] answers,
 * when the score is in between, or when there is no level to move to.
 */
fun adviseDifficulty(logs: List<TriggerLog>, current: Difficulty, now: Long): DifficultyAdvice? {
    val weekAgo = now - 7 * 24 * 60 * 60 * 1000L
    val answers = logs.filter {
        it.correct != null && it.openedAt >= weekAgo && difficultyOfId(it.questionId) == current
    }
    if (answers.size < ADVICE_MIN_ANSWERS) return null
    val percent = Math.round(answers.count { it.correct == true } * 100f / answers.size)
    val levels = Difficulty.entries
    return when {
        percent >= 85 && current != Difficulty.HARD ->
            DifficultyAdvice(levels[current.ordinal + 1], percent, moveUp = true)
        percent <= 50 && current != Difficulty.EASY ->
            DifficultyAdvice(levels[current.ordinal - 1], percent, moveUp = false)
        else -> null
    }
}

/**
 * Returns the value below which [percent] % of the numbers fall
 * ("nearest rank" method). percentile(list, 50) is the median.
 * Returns null for an empty list.
 */
fun percentile(values: List<Long>, percent: Int): Long? {
    if (values.isEmpty()) return null
    val sorted = values.sorted()
    // Example: 10 values, 90th percentile -> rank 9 -> index 8.
    val rank = Math.ceil(percent / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
    return sorted[rank - 1]
}
