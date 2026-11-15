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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
 * background thread. The question screen uses PrayerGateApp.dbThread for that.
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

    /** The user's latest answers, newest first: true = right, false = wrong. */
    @Query("SELECT correct FROM trigger_logs WHERE correct IS NOT NULL ORDER BY openedAt DESC LIMIT 100")
    fun recentAnswers(): List<Boolean>

    @Query("DELETE FROM trigger_logs")
    suspend fun deleteAll()
}

@Dao
interface QuestionProgressDao {
    @Query("SELECT * FROM question_progress")
    fun getAll(): List<QuestionProgress>

    @Query("SELECT * FROM question_progress WHERE questionId = :questionId")
    fun get(questionId: String): QuestionProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(progress: QuestionProgress)

    /** Questions whose last answer was wrong, newest first. Updates by itself. */
    @Query("SELECT * FROM question_progress WHERE lastAnswerWrong = 1 ORDER BY lastSeenAt DESC")
    fun observeToReview(): Flow<List<QuestionProgress>>
}

/** The database itself: a list of tables and a way to reach each DAO. */
@Database(
    entities = [SelectedApp::class, TriggerLog::class, QuestionProgress::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun selectedAppDao(): SelectedAppDao
    abstract fun triggerLogDao(): TriggerLogDao
    abstract fun questionProgressDao(): QuestionProgressDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "prayergate.db")
                .addMigrations(MIGRATION_1_2)
                .build()

        /**
         * Version 2 added the "reason" column to the trigger log.
         * A migration keeps the user's existing data when they update the app.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trigger_logs ADD COLUMN reason TEXT NOT NULL DEFAULT 'OPEN'")
            }
        }
    }
}
