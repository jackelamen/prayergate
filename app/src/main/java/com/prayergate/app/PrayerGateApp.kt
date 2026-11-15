package com.prayergate.app

import android.app.Application
import com.prayergate.app.data.AppDatabase
import com.prayergate.app.data.Settings
import com.prayergate.app.detection.GateController
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The Application object is created once, before any screen or service.
 * We use it to hold the things every part of the app needs.
 */
class PrayerGateApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Load the cooldowns and time-check clocks saved before the last restart.
        GateController.init(this)
    }

    /** The Room database. "by lazy" means it is opened the first time it is used. */
    val database: AppDatabase by lazy { AppDatabase.create(this) }

    /** The user's settings (difficulty, cooldown, engine...). */
    val settings: Settings by lazy { Settings(this) }

    /**
     * One background thread for the question screen's database work.
     * Jobs given to it run one at a time, in the order they were added,
     * and they still finish after the question screen has closed.
     */
    val dbThread: ExecutorService = Executors.newSingleThreadExecutor()
}
