package com.prayergate.app.ui

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.prayergate.app.R
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.data.AppLanguage
import com.prayergate.app.data.PrayerBook
import com.prayergate.app.data.computeStats
import com.prayergate.app.data.prayerOfTheDay
import java.time.LocalDate

/**
 * The home-screen widget: the daily streak and today's prayer.
 * Tapping it opens that prayer to pray (not in front of any app).
 *
 * Widgets are drawn by the launcher, not by PrayerGate, so they use an XML
 * layout (res/layout/widget_prayergate.xml) filled in with RemoteViews,
 * instead of Compose.
 *
 * Android refreshes it every hour (res/xml/prayergate_widget_info.xml), and
 * PrayerGate also refreshes it after every Amen (see [refresh]).
 */
class PrayerGateWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        // The database must not be read on the main thread. goAsync() lets the
        // work finish on PrayerGate's background thread.
        val pending = goAsync()
        val app = context.applicationContext as PrayerGateApp
        app.dbThread.execute {
            try {
                val views = buildViews(context, app)
                for (id in widgetIds) manager.updateAppWidget(id, views)
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildViews(base: Context, app: PrayerGateApp): RemoteViews {
        // The widget is drawn by the launcher, in the phone's language, so every
        // text is set here from the language chosen in the app.
        val context = AppLanguage.wrap(base)
        val today = LocalDate.now()
        val streak = computeStats(app.database.triggerLogDao().getAll(), today).streakDays
        val prayer = prayerOfTheDay(PrayerBook.load(context), app.settings.themes, today.toEpochDay())
        val korean = context.resources.configuration.locales[0].language == "ko"

        val views = RemoteViews(context.packageName, R.layout.widget_prayergate)
        views.setTextViewText(R.id.widget_streak, streak.toString())
        views.setTextViewText(
            R.id.widget_streak_label,
            context.resources.getQuantityString(R.plurals.stats_streak_label, streak),
        )
        views.setTextViewText(R.id.widget_prayer, prayer.text(korean))
        views.setTextViewText(R.id.widget_label, context.getString(R.string.widget_today))
        views.setTextViewText(R.id.widget_hint, context.getString(R.string.widget_tap))

        // Tap anywhere: pray today's prayer.
        val tap = PendingIntent.getActivity(
            context,
            0,
            PrayerActivity.quietIntent(context, prayer.id, fromWidget = true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.widget_root, tap)
        return views
    }

    companion object {
        /** Redraws every PrayerGate widget on the home screen (for example after an Amen). */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PrayerGateWidget::class.java))
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(context, PrayerGateWidget::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }
    }
}
