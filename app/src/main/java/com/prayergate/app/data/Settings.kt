package com.prayergate.app.data

import android.content.Context

/** The two ways PrayerGate can detect that an app was opened. */
enum class Engine { USAGE, ACCESSIBILITY }

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

    /** The themes to pray through. All of them by default; never empty. */
    var themes: Set<PrayerTheme>
        get() {
            val names = prefs.getStringSet("themes", null) ?: return PrayerTheme.entries.toSet()
            return names.mapNotNull { name -> PrayerTheme.entries.firstOrNull { it.name == name } }
                .toSet().ifEmpty { PrayerTheme.entries.toSet() }
        }
        set(value) = prefs.edit().putStringSet("themes", value.map { it.name }.toSet()).apply()

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

    /** Has the user read and accepted the accessibility disclosure? */
    var accessibilityDisclosureAccepted: Boolean
        get() = prefs.getBoolean("accessibilityDisclosureAccepted", false)
        set(value) = prefs.edit().putBoolean("accessibilityDisclosureAccepted", value).apply()
}
