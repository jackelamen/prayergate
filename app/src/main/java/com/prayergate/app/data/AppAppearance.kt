package com.prayergate.app.data

import android.content.Context
import android.content.res.Configuration

/**
 * Light or dark pages: follow the phone, or always light or always dark.
 * Chosen in Settings and kept in the same preferences file as the other settings.
 */
enum class AppAppearance {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        private const val KEY = "appearance"

        /** The saved choice. Read straight from the preferences file, so it works before the Application exists. */
        fun saved(context: Context): AppAppearance {
            val name = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString(KEY, null)
            return entries.firstOrNull { it.name == name } ?: SYSTEM
        }

        fun save(context: Context, appearance: AppAppearance) {
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString(KEY, appearance.name).apply()
        }

        /**
         * Returns a Context whose configuration says "night" or "not night" as
         * chosen. Because it changes the configuration itself (not only the
         * Compose colours), everything follows: the theme, the status bar and
         * navigation bar icons, and the window background from values-night.
         * With "follow the phone" the context is returned untouched.
         */
        fun wrap(base: Context): Context {
            val night = when (saved(base)) {
                SYSTEM -> return base
                LIGHT -> Configuration.UI_MODE_NIGHT_NO
                DARK -> Configuration.UI_MODE_NIGHT_YES
            }
            val config = Configuration(base.resources.configuration)
            config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
            return base.createConfigurationContext(config)
        }

        /** The language and the appearance chosen in Settings, for a screen. */
        fun wrapScreen(base: Context): Context = wrap(AppLanguage.wrap(base))
    }
}
