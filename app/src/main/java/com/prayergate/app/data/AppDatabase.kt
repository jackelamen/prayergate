package com.prayergate.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/*
 * DAO = "data access object". Each interface lists the database operations
 * for one table. Room writes the real code for us at build time.
 *
 * Functions that return a Flow keep sending new results whenever the table changes,
 * so the screens update by themselves.
 *
 * "suspend" functions are called from coroutines. The plain (non-suspend) ones
 * block until the database is done, so they must only be called from a
 * background thread. The prayer screen uses PrayerGateApp.dbThread for that.
 */

@Dao
interface SelectedAppDao {
    @Query("SELECT * FROM selected_apps")
    fun observeAll(): Flow<List<SelectedApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: SelectedApp)

    @Delete
    suspend fun delete(app: SelectedApp)
}

@Dao
interface TriggerLogDao {
    /** Newest first. */
    @Query("SELECT * FROM trigger_logs ORDER BY openedAt DESC")
    fun observeAll(): Flow<List<TriggerLog>>

    /** Returns the id of the new row, so we can update it later. */
    @Insert
    fun insert(log: TriggerLog): Long

    @Update
    fun update(log: TriggerLog)

    /** Every row, for background work such as the widget. */
    @Query("SELECT * FROM trigger_logs")
    fun getAll(): List<TriggerLog>

    /** How many times [packageName] was opened (not time checks) since [since]. */
    @Query("SELECT COUNT(*) FROM trigger_logs WHERE packageName = :packageName AND reason = 'OPEN' AND openedAt >= :since")
    fun countOpensSince(packageName: String, since: Long): Int

    /** The prayers shown most recently, newest first. Used to deal the next one. */
    @Query("SELECT prayerId FROM trigger_logs ORDER BY openedAt DESC LIMIT 500")
    fun recentPrayerIds(): List<String>

    @Query("DELETE FROM trigger_logs")
    suspend fun deleteAll()
}

@Dao
interface SavedPrayerDao {
    /** Newest first. Updates by itself. */
    @Query("SELECT * FROM saved_prayers ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedPrayer>>

    @Query("SELECT COUNT(*) FROM saved_prayers WHERE prayerId = :prayerId")
    fun isSaved(prayerId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(prayer: SavedPrayer)

    @Query("DELETE FROM saved_prayers WHERE prayerId = :prayerId")
    fun remove(prayerId: String)
}

/** The database itself: a list of tables and a way to reach each DAO. */
@Database(
    entities = [SelectedApp::class, TriggerLog::class, SavedPrayer::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun selectedAppDao(): SelectedAppDao
    abstract fun triggerLogDao(): TriggerLogDao
    abstract fun savedPrayerDao(): SavedPrayerDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "prayergate.db")
                .build()

        /*
         * Version 1 is the first released schema. Any change to a table from
         * now on needs a version bump and a Migration added above with
         * .addMigrations(...), tested against a version 1 database.
         */
    }
}
