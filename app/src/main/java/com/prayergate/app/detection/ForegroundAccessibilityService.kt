package com.prayergate.app.detection

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.prayergate.app.PrayerGateApp
import com.prayergate.app.data.Engine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * ENGINE B: an accessibility service.
 *
 * Android tells this service every time a new window comes to the front.
 * The ONLY thing we read from that message is the package name of the app
 * (for example "com.instagram.android").
 *
 * We never read screen content: res/xml/accessibility_service_config.xml sets
 * canRetrieveWindowContent="false", so Android does not even give us access.
 *
 * Android restarts enabled accessibility services by itself after a reboot.
 */
class ForegroundAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Remembers the answer of [isRealApp] for each package, so we only check once. */
    private val realAppCache = HashMap<String, Boolean>()

    /** Called by Android when the user turns the service on (and after a reboot). */
    override fun onServiceConnected() {
        super.onServiceConnected()
        GateController.watchSelectedApps(this, scope)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        // Only act if this engine is the one selected in Settings.
        val settings = (application as PrayerGateApp).settings
        if (settings.engine != Engine.ACCESSIBILITY) return

        // The one and only piece of data we read from the event.
        val packageName = event.packageName?.toString() ?: return

        // Keyboards, the notification shade and other system pop-ups also send
        // this event. They are not "the user opened another app", so skip them.
        if (!isRealApp(packageName)) return

        // event.eventTime counts from when the phone booted. Convert it to
        // normal clock time so both engines measure delay the same way.
        val ageMs = SystemClock.uptimeMillis() - event.eventTime
        val openedAt = System.currentTimeMillis() - ageMs

        GateController.onForegroundApp(this, packageName, openedAt, Engine.ACCESSIBILITY)
    }

    /**
     * True for apps the user can actually "open": apps with a launcher icon,
     * and home screens. False for keyboards and system pop-ups.
     */
    private fun isRealApp(packageName: String): Boolean = realAppCache.getOrPut(packageName) {
        val isKeyboard = (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .enabledInputMethodList.any { it.packageName == packageName }

        val hasLauncherIcon = packageManager.getLaunchIntentForPackage(packageName) != null

        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val isHomeScreen = packageManager.queryIntentActivities(homeIntent, 0)
            .any { it.activityInfo.packageName == packageName }

        !isKeyboard && (hasLauncherIcon || isHomeScreen)
    }

    /** Required by Android. We have nothing to interrupt. */
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
