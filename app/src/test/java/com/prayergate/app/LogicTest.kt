package com.prayergate.app

import com.prayergate.app.data.PrayerTheme
import com.prayergate.app.data.TriggerLog
import com.prayergate.app.data.computeStats
import com.prayergate.app.data.computeStreak
import com.prayergate.app.data.continueDelaySeconds
import com.prayergate.app.data.parsePrayers
import com.prayergate.app.data.percentile
import com.prayergate.app.data.pickPrayer
import com.prayergate.app.data.prayerOfTheDay
import com.prayergate.app.data.readingMillis
import com.prayergate.app.data.weekByApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Tests for the plain-Kotlin logic. Run them with:  ./gradlew test
 * They run on your computer; no phone is needed.
 */
class LogicTest {

    private val prayers = parsePrayers(File("src/main/assets/prayers.json").readText())

    @Test
    fun prayerFile_has80WellFormedPrayers() {
        assertEquals(80, prayers.size)
        assertEquals(80, prayers.map { it.id }.toSet().size)
        assertEquals(80, prayers.map { it.text }.toSet().size)
        for (theme in PrayerTheme.entries) {
            assertEquals("10 prayers in $theme", 10, prayers.count { it.theme == theme })
        }
        val prefix = mapOf(
            PrayerTheme.TEMPTATION to "tm", PrayerTheme.JUDGING to "jd", PrayerTheme.NEIGHBOR to "nb",
            PrayerTheme.INFLUENCE to "in", PrayerTheme.DISCERNMENT to "ds", PrayerTheme.COMPARISON to "cp",
            PrayerTheme.WORDS to "wd", PrayerTheme.TIME to "tt",
        )
        for (p in prayers) {
            assertTrue("id matches theme: ${p.id}", p.id.startsWith(prefix.getValue(p.theme)))
            assertTrue("has Korean: ${p.id}", p.textKo.isNotBlank() && p.referenceKo.isNotBlank())
            assertTrue("has a reference: ${p.id}", Regex("""\d+:\d+""").containsMatchIn(p.reference))
            assertTrue("no em dash: ${p.id}", '—' !in p.text && '—' !in p.textKo)
        }
    }

    @Test
    fun pickPrayer_dealsEveryPrayerOnceBeforeRepeating() {
        val themes = setOf(PrayerTheme.WORDS)
        val pool = prayers.filter { it.theme == PrayerTheme.WORDS }
        val shown = ArrayList<String>() // newest first, like the log
        val random = Random(7)
        repeat(pool.size) {
            val next = pickPrayer(prayers, shown, themes, random)
            assertEquals(PrayerTheme.WORDS, next.theme)
            assertTrue("repeated ${next.id} before the round ended", next.id !in shown)
            shown.add(0, next.id)
        }
        assertEquals(pool.size, shown.toSet().size)
        // A new round starts, but never with the prayer shown last.
        repeat(200) {
            assertNotEquals(shown.first(), pickPrayer(prayers, shown, themes, Random(it)).id)
        }
    }

    @Test
    fun pickPrayer_ignoresHistoryFromOtherThemes_andFallsBackToAll() {
        val history = prayers.filter { it.theme == PrayerTheme.TIME }.map { it.id }
        val next = pickPrayer(prayers, history, setOf(PrayerTheme.JUDGING))
        assertEquals(PrayerTheme.JUDGING, next.theme)
        // No theme chosen: every prayer is possible.
        val any = (0 until 300).map { pickPrayer(prayers, emptyList(), emptySet(), Random(it)).theme }.toSet()
        assertTrue(any.size > 4)
    }

    @Test
    fun prayerOfTheDay_isStableForADayAndChangesDaily() {
        val themes = setOf(PrayerTheme.COMPARISON, PrayerTheme.NEIGHBOR)
        val first = prayerOfTheDay(prayers, themes, epochDay = 20_000)
        assertEquals(first, prayerOfTheDay(prayers, themes, epochDay = 20_000))
        assertTrue(first.theme in themes)
        val week = (20_000L until 20_007L).map { prayerOfTheDay(prayers, themes, it).id }
        assertEquals(7, week.toSet().size) // a different prayer every day of the week
    }

    @Test
    fun readingTime_isAtLeastFourSeconds_andGrowsWithLength() {
        val shortest = prayers.minBy { it.text.length }
        val longest = prayers.maxBy { it.text.length }
        assertEquals(4_000L, readingMillis(shortest.copy(text = "Lord, help me.")))
        assertTrue(readingMillis(longest) > readingMillis(shortest))
        assertTrue("no prayer should take over 10 s to unlock", prayers.all { readingMillis(it) <= 10_000L })
    }

    @Test
    fun percentile_medianAndP90() {
        val values = (1L..10L).map { it * 100 } // 100, 200, ... 1000
        assertEquals(500L, percentile(values, 50))
        assertEquals(900L, percentile(values, 90))
        assertEquals(42L, percentile(listOf(42L), 90))
        assertNull(percentile(emptyList(), 50))
    }

    @Test
    fun streak_countsDaysInARow() {
        val today = LocalDate.of(2026, 3, 10)
        fun days(vararg ago: Long) = ago.map { today.minusDays(it) }.toSet()
        assertEquals(0, computeStreak(emptySet(), today))
        assertEquals(3, computeStreak(days(0, 1, 2), today))
        assertEquals(2, computeStreak(days(1, 2), today))     // nothing yet today: still alive
        assertEquals(0, computeStreak(days(2, 3), today))     // missed yesterday: broken
        assertEquals(1, computeStreak(days(0, 2, 3), today))  // gap after today
    }

    @Test
    fun weekByApp_groupsAndSortsBusiestFirst() {
        val day = 24 * 60 * 60 * 1000L
        val now = 100 * day
        fun row(app: String, action: String, at: Long = now - day) =
            TriggerLog(packageName = app, openedAt = at, delayMs = 1, engine = "USAGE", prayerId = "tm01", action = action)
        val week = weekByApp(
            listOf(
                row("insta", TriggerLog.ACTION_CLOSE), row("insta", TriggerLog.ACTION_CONTINUE), row("insta", TriggerLog.ACTION_CLOSE),
                row("tiktok", TriggerLog.ACTION_CONTINUE),
                row("youtube", TriggerLog.ACTION_CLOSE, at = now - 8 * day), // older than a week
            ),
            now,
        )
        assertEquals(listOf("insta", "tiktok"), week.map { it.packageName })
        assertEquals(3, week[0].pauses)
        assertEquals(2, week[0].closes)
    }

    @Test
    fun escalation_waitsLongerTheMoreOftenTheAppIsOpened() {
        assertEquals(0, continueDelaySeconds(1))
        assertEquals(0, continueDelaySeconds(2))
        assertEquals(5, continueDelaySeconds(3))
        assertEquals(5, continueDelaySeconds(4))
        assertEquals(10, continueDelaySeconds(5))
        assertEquals(10, continueDelaySeconds(12))
    }

    @Test
    fun stats_countPrayersThisWeekAndCloses() {
        val zone = ZoneId.of("UTC")
        val today = LocalDate.of(2026, 3, 10)
        fun at(day: LocalDate) = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        fun log(prayed: Boolean?, action: String, day: LocalDate = today) =
            TriggerLog(packageName = "a", openedAt = at(day), delayMs = 1, engine = "USAGE", prayerId = "tm01", prayed = prayed, action = action)
        val stats = computeStats(
            listOf(
                log(true, TriggerLog.ACTION_CONTINUE),
                log(true, TriggerLog.ACTION_CLOSE),
                log(null, TriggerLog.ACTION_NONE),
                log(true, TriggerLog.ACTION_CLOSE, today.minusDays(1)),
                log(true, TriggerLog.ACTION_CONTINUE, today.minusDays(9)), // older than a week
            ),
            today, zone,
        )
        assertEquals(4, stats.prayed)
        assertEquals(3, stats.prayedThisWeek)
        assertEquals(2, stats.closes)
        assertEquals(2, stats.streakDays)
    }
}
