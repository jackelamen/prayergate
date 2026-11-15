package com.prayergate.app

import android.content.Context
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prayergate.app.data.AppLanguage
import com.prayergate.app.ui.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Korean text is stored with an invisible word joiner between syllables. */
private fun glue(s: String) = s.replace(Regex("(?<=[가-힣])(?=[가-힣])"), "⁠")

@RunWith(AndroidJUnit4::class)
class LanguageTest {
    @get:Rule val rule = createEmptyComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<PrayerGateApp>()

    private fun wrapped(): Context = AppLanguage.wrap(app)

    @Test fun savedChoiceDecidesTheLanguageOfEveryContext() {
        assertEquals("Stats", wrapped().getString(R.string.tab_stats)) // phone default is English
        AppLanguage.save(app, AppLanguage.KOREAN)
        assertEquals(glue("통계"), wrapped().getString(R.string.tab_stats))
        assertEquals(glue("오늘의 기도문"), wrapped().getString(R.string.widget_today))
        AppLanguage.save(app, AppLanguage.ENGLISH)
        assertEquals("Stats", wrapped().getString(R.string.tab_stats))
        AppLanguage.save(app, AppLanguage.SYSTEM)
        assertEquals("Stats", wrapped().getString(R.string.tab_stats))
    }

    @Test fun settingsSwitchChangesTheWholeScreenAndIsRemembered() {
        app.settings.onboardingDone = true
        ActivityScenario.launch(MainActivity::class.java)
        rule.onAllNodesWithText("SETTINGS").onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("한국어").onFirst().performClick()
        rule.waitForIdle()
        // The screen was recreated in Korean and is still on the Settings tab.
        rule.onAllNodesWithText(glue("설정")).onFirst().assertExists()
        rule.onAllNodesWithText(glue("주제")).onFirst().assertExists()
        assertEquals(AppLanguage.KOREAN, AppLanguage.saved(app))

        // A fresh launch (a restart of the app) opens in Korean too.
        ActivityScenario.launch(MainActivity::class.java)
        rule.waitForIdle()
        rule.onAllNodesWithText(glue("설정")).onFirst().assertExists()

        // And back: Phone default follows the phone (English here).
        rule.onAllNodesWithText(glue("기기 설정")).onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("THEMES").onFirst().assertExists()
        assertEquals(AppLanguage.SYSTEM, AppLanguage.saved(app))
    }

    @Test fun welcomeScreenSwitchWorksBeforeSetup() {
        app.settings.onboardingDone = false
        ActivityScenario.launch(MainActivity::class.java)
        rule.waitForIdle()
        rule.onAllNodesWithText("Pray before you scroll.").onFirst().assertExists()
        rule.onAllNodesWithText("한국어").onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText(glue("시작하기")).onFirst().assertExists()
        rule.onAllNodesWithText("English").onFirst().performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Get started").onFirst().assertExists()
    }
}
