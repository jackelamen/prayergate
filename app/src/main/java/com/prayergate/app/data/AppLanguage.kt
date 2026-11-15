package com.prayergate.app.data

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * The language of Prayer Gate: follow the phone, or always English or Korean.
 * The choice is made in Settings (or on the welcome screen) and is kept in the
 * same preferences file as the other settings.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    KOREAN("ko");

    companion object {
        private const val KEY = "language"

        /** The saved choice. Read straight from the preferences file, so it works before the Application exists. */
        fun saved(context: Context): AppLanguage {
            val name = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString(KEY, null)
            return entries.firstOrNull { it.name == name } ?: SYSTEM
        }

        fun save(context: Context, language: AppLanguage) {
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString(KEY, language.name).apply()
        }

        /**
         * Returns a Context whose resources use the saved language. Every screen
         * (and the widget and notification) runs its text through this:
         * activities call it from attachBaseContext. With "follow the phone"
         * the context is returned untouched, so Android's own per-app language
         * setting (Android 13+) still works.
         */
        fun wrap(base: Context): Context {
            val tag = saved(base).tag ?: return base
            val locale = Locale.forLanguageTag(tag)
            val config = Configuration(base.resources.configuration)
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
            return base.createConfigurationContext(config)
        }
    }
}
