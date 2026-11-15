package com.csejudge.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AttemptedQuestionEntity::class,
        BookmarkedQuestionEntity::class,
        TestEntity::class,
        TestQuestionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun practiceDao(): PracticeDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun testDao(): TestDao

    companion object {
        private const val DATABASE_NAME = "cse_judge_database.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bookmarked_questions` (
                        `question_id` TEXT NOT NULL,
                        `bookmarked_at` INTEGER NOT NULL,
                        PRIMARY KEY(`question_id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tests` (
                        `test_id` TEXT NOT NULL,
                        `test_type` TEXT NOT NULL,
                        `started_at` INTEGER NOT NULL,
                        `completed_at` INTEGER NOT NULL,
                        `duration_seconds` INTEGER NOT NULL,
                        `total_questions` INTEGER NOT NULL,
                        `attempted_count` INTEGER NOT NULL,
                        `correct_count` INTEGER NOT NULL,
                        `incorrect_count` INTEGER NOT NULL,
                        `unanswered_count` INTEGER NOT NULL,
                        `score` REAL NOT NULL,
                        `max_score` REAL NOT NULL,
                        `accuracy` REAL NOT NULL,
                        `status` TEXT NOT NULL,
                        PRIMARY KEY(`test_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `test_questions` (
                        `test_id` TEXT NOT NULL,
                        `question_order` INTEGER NOT NULL,
                        `question_id` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `microtheme` TEXT NOT NULL,
                        `question_statement_json` TEXT NOT NULL,
                        `options_json` TEXT NOT NULL,
                        `correct_answer` TEXT NOT NULL,
                        `solution_json` TEXT NOT NULL,
                        `selected_answer` TEXT,
                        `is_marked_for_review` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        PRIMARY KEY(`test_id`, `question_order`),
                        FOREIGN KEY(`test_id`) REFERENCES `tests`(`test_id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_test_questions_test_id` ON `test_questions` (`test_id`)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_test_questions_test_id_question_id` ON `test_questions` (`test_id`, `question_id`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
