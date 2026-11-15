package com.prayergate.app.detection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.data.Engine

/** Starts or stops the detection engines so they match the user's settings. */
object Engines {

    /**
     * Call this whenever something may have changed: the app was opened,
     * the engine was switched, a permission was granted, or the phone rebooted.
     *
     * Engine A (the polling service) is started and stopped here.
     * Engine B (accessibility) is switched on by the user in system settings;
     * Android keeps it running, and it simply ignores events when it is not
     * the selected engine.
     */
    fun apply(context: Context) {
        val settings = (context.applicationContext as PrayerGateApp).settings
        val serviceIntent = Intent(context, UsagePollingService::class.java)

        val shouldRunPolling = settings.onboardingDone &&
            settings.engine == Engine.USAGE &&
            Permissions.hasUsageAccess(context)

        if (shouldRunPolling) {
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                // Android can refuse to start a service from the background.
                // It will be started the next time the app is opened.
                Log.w("PrayerGate", "Could not start the polling service", e)
            }
        } else {
            context.stopService(serviceIntent)
        }
    }
}

/** Android sends this a message after a reboot (and after an app update). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Engines.apply(context)
        }
    }
}
