package com.prayergate.app

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prayergate.app.data.AppAppearance
import com.prayergate.app.data.AppLanguage
import com.prayergate.app.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private fun Context.isNight() =
    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

@RunWith(AndroidJUnit4::class)
class AppearanceTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<PrayerGateApp>()

    @Test fun savedChoiceDecidesNightMode_andKeepsTheLanguage() {
        assertFalse(AppAppearance.wrap(app).isNight()) // the test phone is in light mode
        AppAppearance.save(app, AppAppearance.DARK)
        assertTrue(AppAppearance.wrap(app).isNight())
        AppAppearance.save(app, AppAppearance.LIGHT)
        assertFalse(AppAppearance.wrap(app).isNight())

        // Language and appearance together: neither cancels the other.
        AppAppearance.save(app, AppAppearance.DARK)
        AppLanguage.save(app, AppLanguage.KOREAN)
        val screen = AppAppearance.wrapScreen(app)
        assertTrue(screen.isNight())
        assertEquals("ko", screen.resources.configuration.locales[0].language)
    }

    @Test fun settingsSwitchTurnsTheScreenDark_andIsRemembered() {
        app.settings.onboardingDone = true
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { assertFalse(it.isNight()) }
        rule.onAllNodesWithText("SETTINGS").onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Dark").onFirst().performScrollTo().performClick()
        rule.waitForIdle()
        // The screen was recreated in dark mode, still on Settings.
        scenario.onActivity { assertTrue(it.isNight()) }
        rule.onAllNodesWithText("APPEARANCE").onFirst().assertExists()
        assertEquals(AppAppearance.DARK, AppAppearance.saved(app))

        // A fresh launch opens dark too; Phone default goes back to light.
        scenario.close()
        val again = ActivityScenario.launch(MainActivity::class.java)
        again.onActivity { assertTrue(it.isNight()) }
        rule.onAllNodesWithText("SETTINGS").onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Phone default")[1].performScrollTo().performClick()
        rule.waitForIdle()
        again.onActivity { assertFalse(it.isNight()) }
        assertEquals(AppAppearance.SYSTEM, AppAppearance.saved(app))
    }
}
