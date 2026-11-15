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
import com.prayergate.app.data.QuestionBank
import com.prayergate.app.data.computeStats
import com.prayergate.app.data.questionOfTheDay
import java.time.LocalDate

/**
 * The home-screen widget: the daily streak and today's question.
 * Tapping it opens that question in practice mode.
 *
 * Widgets are drawn by the launcher, not by PrayerGate, so they use an XML
 * layout (res/layout/widget_prayergate.xml) filled in with RemoteViews,
 * instead of Compose.
 *
 * Android refreshes it every hour (res/xml/prayergate_widget_info.xml), and
 * PrayerGate also refreshes it after every answer (see [refresh]).
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

    private fun buildViews(context: Context, app: PrayerGateApp): RemoteViews {
        val today = LocalDate.now()
        val streak = computeStats(app.database.triggerLogDao().getAll(), today).streakDays
        val question = questionOfTheDay(
            QuestionBank.load(context), app.settings.difficulty, app.settings.questionType, today.toEpochDay(),
        )

        val views = RemoteViews(context.packageName, R.layout.widget_prayergate)
        views.setTextViewText(R.id.widget_streak, streak.toString())
        views.setTextViewText(
            R.id.widget_streak_label,
            context.resources.getQuantityString(R.plurals.stats_streak_label, streak),
        )
        views.setTextViewText(R.id.widget_question, question.question)

        // Tap anywhere: answer today's question in practice mode.
        val tap = PendingIntent.getActivity(
            context,
            0,
            QuestionActivity.practiceIntent(context, question.id, fromWidget = true),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.widget_root, tap)
        return views
    }

    companion object {
        /** Redraws every PrayerGate widget on the home screen (for example after an answer). */
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
