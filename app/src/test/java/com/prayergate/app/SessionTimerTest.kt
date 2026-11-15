package com.prayergate.app

import com.prayergate.app.detection.SessionTimer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the time check's clock. Run with:  ./gradlew test */
class SessionTimerTest {

    private val min = 60_000L
    private val limit = 15 * min
    private val app = "com.instagram.android"

    @Test
    fun shortBreak_carriesOn() {
        val timer = SessionTimer()
        timer.start(app, now = 0)
        timer.leave(app, now = 10 * min)                  // 10 min used
        val left = timer.enter(app, now = 10 * min + 30_000, limitMs = limit) // back after 30 s
        assertEquals(5 * min, left)
    }

    @Test
    fun breakOfOneMinute_startsFromZero() {
        val timer = SessionTimer()
        timer.start(app, now = 0)
        timer.leave(app, now = 10 * min)
        val left = timer.enter(app, now = 11 * min, limitMs = limit) // back after exactly 1 min
        assertEquals(limit, left)
    }

    @Test
    fun timeAway_doesNotCount() {
        val timer = SessionTimer()
        timer.start(app, now = 0)
        timer.leave(app, now = 4 * min)
        timer.enter(app, now = 4 * min + 50_000, limitMs = limit)
        assertEquals(6 * min, timer.usedMs(app, now = 6 * min + 50_000)) // 4 + 2, not 6:50
    }

    @Test
    fun manyShortBreaks_stillAddUp() {
        // Popping out for a few seconds again and again must not dodge the time check.
        val timer = SessionTimer()
        timer.start(app, now = 0)
        var now = 0L
        repeat(5) {
            now += 3 * min
            timer.leave(app, now)
            now += 20_000
            timer.enter(app, now, limit)
        }
        assertEquals(0L, timer.enter(app, now, limit)) // 15 min used: time is up
    }

    @Test
    fun enteringTwice_isHarmless() {
        val timer = SessionTimer()
        timer.start(app, now = 0)
        timer.enter(app, now = 2 * min, limitMs = limit)
        assertEquals(10 * min, timer.enter(app, now = 5 * min, limitMs = limit))
    }

    @Test
    fun reset_forgetsTheApp() {
        val timer = SessionTimer()
        timer.start(app, now = 0)
        assertTrue(timer.has(app))
        timer.reset(app)
        assertFalse(timer.has(app))
        assertEquals(0L, timer.usedMs(app, now = 5 * min))
    }

    @Test
    fun clocksSurviveSavingAndLoading() {
        val timer = SessionTimer()
        timer.start(app, now = 0)
        timer.leave(app, now = 6 * min)
        timer.start("com.zhiliaoapp.musically", now = 6 * min)

        val restored = SessionTimer()
        restored.restore(timer.toJson())
        assertEquals("com.zhiliaoapp.musically", restored.runningPackage())
        assertEquals(6 * min, restored.usedMs(app, now = 99 * min)) // paused: does not grow
        // Back to Instagram 30 s after leaving: the break rule still applies.
        assertEquals(9 * min, restored.enter(app, now = 6 * min + 30_000, limitMs = limit))
    }

    @Test
    fun badSavedText_isIgnored() {
        val timer = SessionTimer()
        timer.restore("not json")
        assertFalse(timer.has(app))
        timer.restore(null)
        assertFalse(timer.has(app))
    }

    @Test
    fun eachAppHasItsOwnClock() {
        val timer = SessionTimer()
        timer.start(app, now = 0)
        timer.leave(app, now = 8 * min)
        timer.enter("com.zhiliaoapp.musically", now = 8 * min, limitMs = limit)
        timer.leave("com.zhiliaoapp.musically", now = 8 * min + 40_000)
        // Back to Instagram 40 s later: its 8 minutes are still there.
        assertEquals(7 * min, timer.enter(app, now = 8 * min + 40_000, limitMs = limit))
    }
}
