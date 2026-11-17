package com.prayergate.app.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.detection.Engines
import com.prayergate.app.detection.GateController
import com.prayergate.app.ui.PrayerGateWidget

/**
 * "Reset PrayerGate" in Settings: deletes everything PrayerGate has stored and
 * sends the user back to the start of setup.
 *
 * Deleted: chosen apps, every prayer and pause, saved prayers, streak and
 * stats, all settings, cooldowns and time-check clocks.
 * Not changed: the language and light/dark choices (the screen is already
 * showing them), and
 * permissions the user granted in Android settings. An app
 * cannot switch those off itself; setup simply skips the ones already on.
 */
object AppReset {

    /** Does the work on the background thread, then calls [onDone] on the main thread. */
    fun resetEverything(context: Context, onDone: () -> Unit) {
        val app = context.applicationContext as PrayerGateApp
        val language = AppLanguage.saved(app)
        val appearance = AppAppearance.saved(app)
        app.dbThread.execute {
            app.database.clearAllTables()
            for (file in listOf("settings", "gate_state")) {
                app.getSharedPreferences(file, Context.MODE_PRIVATE).edit().clear().commit()
            }
            AppLanguage.save(app, language)
            AppAppearance.save(app, appearance)
            GateController.forgetEverything()
            Handler(Looper.getMainLooper()).post {
                Engines.apply(app)           // stops detection until setup is finished again
                PrayerGateWidget.refresh(app)  // the widget's streak goes back to 0
                onDone()
            }
        }
    }
}
