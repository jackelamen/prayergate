package com.prayergate.app.data

import android.content.Context
import java.util.Locale

/** The two ways PrayerGate can detect that an app was opened. */
enum class Engine { USAGE, ACCESSIBILITY }

enum class Difficulty { EASY, MEDIUM, HARD }

enum class QuestionType { VOCABULARY, GRAMMAR, BOTH }

/**
 * The user's choices from the Settings screen.
 *
 * These are a handful of small values, so they are stored in SharedPreferences
 * (a simple key/value file on the phone) instead of the database. That also
 * lets the background services read them instantly.
 */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Has the user finished the first-run walkthrough? */
    var onboardingDone: Boolean
        get() = prefs.getBoolean("onboardingDone", false)
        set(value) = prefs.edit().putBoolean("onboardingDone", value).apply()

    var engine: Engine
        get() = Engine.valueOf(prefs.getString("engine", Engine.USAGE.name)!!)
        set(value) = prefs.edit().putString("engine", value.name).apply()

    var difficulty: Difficulty
        get() = Difficulty.valueOf(prefs.getString("difficulty", Difficulty.EASY.name)!!)
        set(value) = prefs.edit().putString("difficulty", value.name).apply()

    var questionType: QuestionType
        get() = QuestionType.valueOf(prefs.getString("questionType", QuestionType.BOTH.name)!!)
        set(value) = prefs.edit().putString("questionType", value.name).apply()

    /** After "Continue", don't ask again for the same app for this many minutes (1, 5 or 15). */
    var cooldownMinutes: Int
        get() = prefs.getInt("cooldownMinutes", 5)
        set(value) = prefs.edit().putInt("cooldownMinutes", value).apply()

    /**
     * Time check: after "Continue", ask again if the user is still in the app
     * after this many minutes. 0 means off.
     */
    var sessionLimitMinutes: Int
        get() = prefs.getInt("sessionLimitMinutes", 0)
        set(value) = prefs.edit().putInt("sessionLimitMinutes", value).apply()

    /**
     * Show each explanation in Korean too. On by default when the phone's
     * language is Korean.
     */
    var koreanExplanations: Boolean
        get() = prefs.getBoolean("koreanExplanations", Locale.getDefault().language == "ko")
        set(value) = prefs.edit().putBoolean("koreanExplanations", value).apply()

    /** The difficulty suggestion on the Stats screen stays hidden until this time (ms). */
    var adviceHiddenUntil: Long
        get() = prefs.getLong("adviceHiddenUntil", 0L)
        set(value) = prefs.edit().putLong("adviceHiddenUntil", value).apply()

    /** Has the user read and accepted the accessibility disclosure? */
    var accessibilityDisclosureAccepted: Boolean
        get() = prefs.getBoolean("accessibilityDisclosureAccepted", false)
        set(value) = prefs.edit().putBoolean("accessibilityDisclosureAccepted", value).apply()
}
