package com.prayergate.app.detection

import org.json.JSONObject

/**
 * How long a short break may last before the time check starts over.
 * Fixed on purpose, not a setting: if a quick switch to another app reset
 * the clock, the time check would be too easy to dodge.
 */
const val BREAK_RESETS_AFTER_MS = 60_000L

/**
 * Keeps track of how long the user has been using each chosen app, for the
 * time check. It is plain Kotlin with no Android code (the current time is
 * passed in), so it can be unit tested.
 *
 * The rules:
 *  - Time only counts while the app is in front.
 *  - Leaving the app pauses the clock.
 *  - Coming back within [breakResetMs] carries on from where it was.
 *  - Coming back after [breakResetMs] or longer starts again from zero.
 *  - Seeing a prayer (any prayer) for the app starts it from zero.
 */
class SessionTimer(private val breakResetMs: Long = BREAK_RESETS_AFTER_MS) {

    /** The clock for one app. */
    private class Session {
        var usedMs = 0L           // time counted so far, not including the current stretch
        var runningSince: Long? = null // when the current stretch began, or null if paused
        var leftAt: Long? = null  // when the user last left, or null
    }

    private val sessions = HashMap<String, Session>()

    /** Starts the clock for [packageName] from zero (after the user prayed). */
    fun start(packageName: String, now: Long) {
        sessions[packageName] = Session().apply { runningSince = now }
    }

    /**
     * The user is in [packageName] (again). Resumes its clock, or starts it
     * from zero if the break was long enough. Doing this twice in a row is harmless.
     *
     * @return how many milliseconds are left before the time check is due.
     */
    fun enter(packageName: String, now: Long, limitMs: Long): Long {
        val session = sessions.getOrPut(packageName) { Session() }
        if (session.runningSince == null) {
            val leftAt = session.leftAt
            if (leftAt != null && now - leftAt >= breakResetMs) session.usedMs = 0
            session.leftAt = null
            session.runningSince = now
        }
        return (limitMs - usedMs(packageName, now)).coerceAtLeast(0)
    }

    /** The user left [packageName] (another app, the home screen, or the screen went off). */
    fun leave(packageName: String, now: Long) {
        val session = sessions[packageName] ?: return
        val since = session.runningSince ?: return
        session.usedMs += now - since
        session.runningSince = null
        session.leftAt = now
    }

    /** Forgets [packageName]'s clock (a prayer is being shown for it). */
    fun reset(packageName: String) {
        sessions.remove(packageName)
    }

    /** Forgets every clock (used by "Reset PrayerGate"). */
    fun clear() {
        sessions.clear()
    }

    /** Is there a clock for [packageName], running or paused? */
    fun has(packageName: String): Boolean = packageName in sessions

    /** The app whose clock is running, if any (at most one app is in front at a time). */
    fun runningPackage(): String? = sessions.entries.firstOrNull { it.value.runningSince != null }?.key

    /**
     * Saves every clock as text, so it can be stored on the phone and survive
     * Android restarting PrayerGate in the background.
     */
    fun toJson(): String {
        val json = JSONObject()
        for ((packageName, session) in sessions) {
            json.put(packageName, JSONObject().apply {
                put("usedMs", session.usedMs)
                session.runningSince?.let { put("runningSince", it) }
                session.leftAt?.let { put("leftAt", it) }
            })
        }
        return json.toString()
    }

    /** Loads clocks saved by [toJson]. Bad or empty text is ignored. */
    fun restore(text: String?) {
        sessions.clear()
        if (text.isNullOrBlank()) return
        try {
            val json = JSONObject(text)
            for (packageName in json.keys()) {
                val item = json.getJSONObject(packageName)
                sessions[packageName] = Session().apply {
                    usedMs = item.optLong("usedMs", 0)
                    runningSince = if (item.has("runningSince")) item.getLong("runningSince") else null
                    leftAt = if (item.has("leftAt")) item.getLong("leftAt") else null
                }
            }
        } catch (e: Exception) {
            sessions.clear()
        }
    }

    /** Total time counted for [packageName], including the stretch running now. */
    fun usedMs(packageName: String, now: Long): Long {
        val session = sessions[packageName] ?: return 0
        val running = session.runningSince?.let { now - it } ?: 0
        return session.usedMs + running
    }
}
