package com.prayergate.app.detection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.prayergate.app.R
import com.prayergate.app.data.AppLanguage
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.data.Engine
import com.prayergate.app.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ENGINE A: a foreground service that asks Android "which app was opened?"
 * about every 300 ms, using UsageStatsManager.
 *
 * A "foreground service" is a service with a permanent notification.
 * Android keeps it alive much longer than a normal background service.
 */
class UsagePollingService : Service() {

    /** Coroutines started in this scope are cancelled when the service stops. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        showNotification()
        GateController.watchSelectedApps(this, scope)
        scope.launch { pollLoop() }
    }

    /** START_STICKY asks Android to restart the service if it ever gets killed. */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** Other apps cannot connect to this service. */
    override fun onBind(intent: Intent?): IBinder? = null

    /** The loop that runs for as long as the service is alive. */
    private suspend fun pollLoop() {
        val usageStats = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        val settings = (application as PrayerGateApp).settings

        // We only care about events newer than this timestamp.
        var lastEventTime = System.currentTimeMillis()

        while (scope.isActive) {
            // Save battery: when the screen is off nobody can open an app.
            if (!power.isInteractive) {
                delay(SCREEN_OFF_INTERVAL_MS)
                continue
            }

            // Ask for recent events. We look back a little further than needed
            // because Android sometimes records an event slightly late.
            val now = System.currentTimeMillis()
            val events = usageStats.queryEvents(lastEventTime - LOOK_BACK_MS, now)
            val event = UsageEvents.Event()

            // Find the newest "an activity came to the foreground" event.
            var newestPackage: String? = null
            var newestTime = lastEventTime
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && event.timeStamp > newestTime) {
                    newestPackage = event.packageName
                    newestTime = event.timeStamp
                }
            }

            if (newestPackage != null) {
                lastEventTime = newestTime
                // Only act if this engine is the one selected in Settings.
                if (settings.engine == Engine.USAGE) {
                    GateController.onForegroundApp(this, newestPackage, newestTime, Engine.USAGE)
                }
            }

            delay(POLL_INTERVAL_MS)
        }
    }

    /** Creates the permanent notification that every foreground service must show. */
    private fun showNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            AppLanguage.wrap(this).getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_MIN, // silent, and collapsed at the bottom of the list
        )
        manager.createNotificationChannel(channel)

        // Tapping the notification opens PrayerGate.
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(AppLanguage.wrap(this).getString(R.string.notification_title))
            .setContentText(AppLanguage.wrap(this).getString(R.string.notification_text))
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()

        // From Android 14 a foreground service must say what type it is.
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    companion object {
        private const val CHANNEL_ID = "detection"
        private const val NOTIFICATION_ID = 1
        private const val POLL_INTERVAL_MS = 300L
        private const val SCREEN_OFF_INTERVAL_MS = 1500L
        private const val LOOK_BACK_MS = 1000L
    }
}
