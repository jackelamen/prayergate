package com.prayergate.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 * Each class in this file is one table in the Room database.
 * Each property is one column.
 */

/** An app the user has chosen. Opening it triggers a question. */
@Entity(tableName = "selected_apps")
data class SelectedApp(
    @PrimaryKey val packageName: String,
)

/** One row for every time the question screen was triggered. */
@Entity(tableName = "trigger_logs")
data class TriggerLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The app that was opened, e.g. "com.instagram.android". */
    val packageName: String,
    /** When the app came to the foreground (milliseconds since 1970). */
    val openedAt: Long,
    /** Milliseconds between the app opening and the question appearing on screen. */
    val delayMs: Long,
    /** Which detection engine caught it: "USAGE" or "ACCESSIBILITY". */
    val engine: String,
    /** Which question was shown. */
    val questionId: String,
    /** true = right, false = wrong, null = the user left without answering. */
    val correct: Boolean? = null,
    /** "CONTINUE", "CLOSE", or "NONE" if the user left another way. */
    val action: String = ACTION_NONE,
    /**
     * Why the question appeared: "OPEN" (the app was opened) or "TIME_CHECK"
     * (the user stayed in the app for the time set in Settings).
     */
    @ColumnInfo(defaultValue = REASON_OPEN)
    val reason: String = REASON_OPEN,
) {
    companion object {
        const val REASON_OPEN = "OPEN"
        const val REASON_TIME_CHECK = "TIME_CHECK"

        const val ACTION_NONE = "NONE"
        const val ACTION_CONTINUE = "CONTINUE"
        const val ACTION_CLOSE = "CLOSE"
    }
}

/** Remembers how the user did on each question, so wrong ones come back sooner. */
@Entity(tableName = "question_progress")
data class QuestionProgress(
    @PrimaryKey val questionId: String,
    val timesSeen: Int,
    val timesWrong: Int,
    /** true if the most recent answer to this question was wrong. */
    val lastAnswerWrong: Boolean,
    val lastSeenAt: Long,
)
