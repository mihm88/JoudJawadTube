package com.joudjawadtube.player.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VideoEntity::class,
        PlaybackProgressEntity::class,
        SavedSafFolderEntity::class,
        SavedPlaylistEntity::class,
        SavedChannelEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_channels` (
                        `channelId` TEXT NOT NULL PRIMARY KEY,
                        `handle` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `avatarUrl` TEXT NOT NULL,
                        `uploadsPlaylistId` TEXT NOT NULL,
                        `importedAt` INTEGER NOT NULL,
                        `lastSyncedAt` INTEGER NOT NULL,
                        `videoCap` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `saved_channels` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'General'")
                db.execSQL("ALTER TABLE `saved_youtube_playlists` ADD COLUMN `category` TEXT NOT NULL DEFAULT 'General'")
                db.execSQL("ALTER TABLE `saved_youtube_playlists` ADD COLUMN `channelId` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `saved_youtube_playlists` ADD COLUMN `isAutoCreatedFromChannel` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "joud_jawad_tube.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
