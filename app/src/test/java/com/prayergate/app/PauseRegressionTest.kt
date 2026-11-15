package com.prayergate.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prayergate.app.data.Engine
import com.prayergate.app.data.parsePrayers
import com.prayergate.app.detection.GateController
import com.prayergate.app.ui.PrayerScreen
import com.prayergate.app.ui.theme.PrayerGateTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File

/*
 * Regression tests for two bugs found on a real phone:
 *  - AmenHoldTest: Close and Continue, hidden but stacked on top of Amen,
 *    swallowed every press, so Amen could never be held.
 *  - OpenCountTest: while the prayer screen was up, the app behind it
 *    reporting itself again counted as a new open, so one open was logged
 *    two or three times ("opened 5 times this hour" after 2 opens).
 * They run with Robolectric, on the computer.
 */

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w412dp-h915dp-xxhdpi")
class AmenHoldTest {
    @get:Rule val rule = createComposeRule()

    private fun screen(onAmen: () -> Unit) {
        val p = parsePrayers(File("src/main/assets/prayers.json").readText()).first { it.id == "in10" }
        rule.mainClock.autoAdvance = false
        rule.setContent {
            PrayerGateTheme {
                var prayed by remember { mutableStateOf(false) }
                PrayerScreen(
                    header = "A prayer before Instagram", prayer = p, korean = false, prayed = prayed, saved = false,
                    onShown = {}, onAmen = { prayed = true; onAmen() }, onToggleSaved = {},
                    primaryText = "Close", onPrimary = {}, secondaryText = "Continue to Instagram",
                    onSecondary = {}, onBack = {},
                )
            }
        }
        rule.mainClock.advanceTimeBy(11_000) // the reading time is over
    }

    @Test fun holdingAmenForOneAndAHalfSecondsPrays() {
        var amen = 0
        screen { amen++ }
        rule.onNodeWithContentDescription("Hold to say Amen").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(1_700)
        // After Amen the button is hidden from screen readers, so lift the finger from the root.
        rule.onRoot().performTouchInput { up() }
        rule.mainClock.advanceTimeBy(200)
        assertEquals(1, amen)
    }

    @Test fun aShortPressDoesNotPray() {
        var amen = 0
        screen { amen++ }
        rule.onNodeWithContentDescription("Hold to say Amen").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithContentDescription("Hold to say Amen").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(1_500)
        assertEquals(0, amen)
    }
}

@RunWith(AndroidJUnit4::class)
class OpenCountTest {
    private val app get() = ApplicationProvider.getApplicationContext<PrayerGateApp>()

    private fun pausesOpened(): Int {
        var n = 0
        while (shadowOf(app).nextStartedActivity != null) n++
        return n
    }

    @Test fun theAppBehindThePrayerScreenIsNotANewOpen() {
        GateController.init(app)
        GateController::class.java.getDeclaredField("selectedPackages").apply { isAccessible = true }
            .set(GateController, setOf("com.instagram.android"))
        pausesOpened() // clear anything from start-up
        val insta = "com.instagram.android"
        var t = System.currentTimeMillis()

        // 1. Instagram opens: one pause.
        GateController.onForegroundApp(app, insta, t, Engine.USAGE)
        // The prayer screen comes up in front, and Instagram behind it reports
        // itself again (splash screen handing over to its main screen).
        GateController.onForegroundApp(app, app.packageName, ++t, Engine.USAGE)
        GateController.onForegroundApp(app, insta, ++t, Engine.USAGE)
        GateController.onForegroundApp(app, app.packageName, ++t, Engine.USAGE)
        assertEquals("one open, one pause", 1, pausesOpened())

        // 2. The user taps Close: the prayer screen closes, the home screen shows.
        GateController.prayerClosed(insta)
        GateController.onForegroundApp(app, "com.android.launcher", ++t, Engine.USAGE)
        // 3. Instagram opens again: a second pause.
        GateController.onForegroundApp(app, insta, ++t, Engine.USAGE)
        assertEquals("second open, second pause", 1, pausesOpened())
    }
}
