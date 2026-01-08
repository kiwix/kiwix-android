/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.kiwix.kiwixmobile.core.dao.AppUpdateDao
import org.kiwix.kiwixmobile.core.dao.DownloadApkDao
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDao
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDaoCoverts
import org.kiwix.kiwixmobile.core.dao.NotesRoomDao
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.dao.WebViewHistoryRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.AppUpdateEntity
import org.kiwix.kiwixmobile.core.dao.entities.BundleRoomConverter
import org.kiwix.kiwixmobile.core.dao.entities.DownloadApkEntity
import org.kiwix.kiwixmobile.core.dao.entities.DownloadRoomEntity
import org.kiwix.kiwixmobile.core.dao.entities.ErrorConverter
import org.kiwix.kiwixmobile.core.dao.entities.HistoryRoomEntity
import org.kiwix.kiwixmobile.core.dao.entities.NotesRoomEntity
import org.kiwix.kiwixmobile.core.dao.entities.PauseReasonConverter
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchRoomEntity
import org.kiwix.kiwixmobile.core.dao.entities.StatusConverter
import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity
import org.kiwix.kiwixmobile.core.dao.entities.ZimSourceRoomConverter

@Suppress("UnnecessaryAbstractClass")
@Database(
  entities = [
    RecentSearchRoomEntity::class,
    HistoryRoomEntity::class,
    NotesRoomEntity::class,
    DownloadRoomEntity::class,
    WebViewHistoryEntity::class,
    AppUpdateEntity::class,
    DownloadApkEntity::class
  ],
  version = 12,
  exportSchema = false
)
@TypeConverters(
  HistoryRoomDaoCoverts::class,
  ZimSourceRoomConverter::class,
  BundleRoomConverter::class,
  StatusConverter::class,
  ErrorConverter::class,
  PauseReasonConverter::class
)
abstract class KiwixRoomDatabase : RoomDatabase() {
  abstract fun recentSearchRoomDao(): RecentSearchRoomDao
  abstract fun historyRoomDao(): HistoryRoomDao
  abstract fun notesRoomDao(): NotesRoomDao
  abstract fun downloadRoomDao(): DownloadRoomDao
  abstract fun webViewHistoryRoomDao(): WebViewHistoryRoomDao
  abstract fun appUpdateDao(): AppUpdateDao
  abstract fun downloadApkDao(): DownloadApkDao

  companion object {
    private var db: KiwixRoomDatabase? = null
    fun getInstance(context: Context): KiwixRoomDatabase {
      return db ?: synchronized(KiwixRoomDatabase::class) {
        return@getInstance db
          ?: Room.databaseBuilder(context, KiwixRoomDatabase::class.java, "KiwixRoom.db")
            // We have already database name called kiwix.db in order to avoid complexity we named
            // as kiwixRoom.db
            .addMigrations(
              MIGRATION_1_2,
              MIGRATION_2_3,
              MIGRATION_3_4,
              MIGRATION_4_5,
              MIGRATION_5_6,
              MIGRATION_6_7,
              MIGRATION_7_8,
              MIGRATION_8_9,
              MIGRATION_9_10
            )
            .build().also { db = it }
      }
    }

    private val MIGRATION_1_2: Migration =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `HistoryRoomEntity`(
            `id` INTEGER NOT NULL,
            `timeStamp` INTEGER NOT NULL,
            `zimId` TEXT NOT NULL,
            `historyUrl` TEXT NOT NULL,
            `zimName` TEXT NOT NULL,
            `favicon` TEXT,
            `historyTitle` TEXT NOT NULL,
            `dateString` TEXT NOT NULL,
            `zimFilePath` TEXT NOT NULL,
            PRIMARY KEY (`id`)
          )
          """
          )
        }
      }

    @Suppress("MagicNumber")
    private val MIGRATION_2_3 =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `NotesRoomEntity`(
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `zimId` TEXT NOT NULL,
              `zimFilePath` TEXT,
              `zimUrl` TEXT NOT NULL,
              `noteTitle` TEXT NOT NULL,
              `noteFilePath` TEXT NOT NULL,
              `favicon` TEXT
            )
          """
          )

          db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_NotesRoomEntity_noteTitle` ON `NotesRoomEntity` (`noteTitle`)
            """
          )
        }
      }

    @Suppress("MagicNumber")
    private val MIGRATION_3_4 =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `DownloadRoomEntity` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `downloadId` INTEGER NOT NULL,
                `file` TEXT,
                `etaInMilliSeconds` INTEGER NOT NULL DEFAULT -1,
                `bytesDownloaded` INTEGER NOT NULL DEFAULT -1,
                `totalSizeOfDownload` INTEGER NOT NULL DEFAULT -1,
                `status` TEXT NOT NULL DEFAULT 'NONE',
                `error` TEXT NOT NULL DEFAULT 'NONE',
                `progress` INTEGER NOT NULL DEFAULT -1,
                `bookId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `description` TEXT,
                `language` TEXT NOT NULL,
                `creator` TEXT NOT NULL,
                `publisher` TEXT NOT NULL,
                `date` TEXT NOT NULL,
                `url` TEXT,
                `articleCount` TEXT,
                `mediaCount` TEXT,
                `size` TEXT NOT NULL,
                `name` TEXT,
                `favIcon` TEXT NOT NULL,
                `tags` TEXT
            )
            """
          )
        }
      }

    @Suppress("MagicNumber")
    private val MIGRATION_4_5 =
      object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE HistoryRoomEntity ADD COLUMN zimReaderSource TEXT")
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS HistoryRoomEntity_temp (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                zimId TEXT NOT NULL,
                zimName TEXT NOT NULL,
                zimFilePath TEXT,
                favicon TEXT,
                historyUrl TEXT NOT NULL,
                historyTitle TEXT NOT NULL,
                dateString TEXT NOT NULL,
                timeStamp INTEGER NOT NULL,
                zimReaderSource TEXT
            )
        """
          )
          db.execSQL(
            """
            INSERT INTO HistoryRoomEntity_temp (
            id,
            zimId,
            zimName,
            zimFilePath, 
            favicon, 
            historyUrl,
            historyTitle, 
            dateString, 
            timeStamp,
            zimReaderSource
            )
            SELECT 
            id,
            zimId,
            zimName,
            zimFilePath,
            favicon,
            historyUrl,
            historyTitle,
            dateString,
            timeStamp,
            zimReaderSource
            FROM HistoryRoomEntity
        """
          )
          db.execSQL("DROP TABLE HistoryRoomEntity")
          db.execSQL("ALTER TABLE HistoryRoomEntity_temp RENAME TO HistoryRoomEntity")
          db.execSQL("ALTER TABLE NotesRoomEntity ADD COLUMN zimReaderSource TEXT")
        }
      }

    @Suppress("MagicNumber")
    private val MIGRATION_5_6 =
      object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "ALTER TABLE DownloadRoomEntity ADD COLUMN pausedByUser INTEGER NOT NULL DEFAULT 0"
          )
        }
      }

    @Suppress("MagicNumber")
    private val MIGRATION_6_7 =
      object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS DownloadRoomEntity_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                downloadId INTEGER NOT NULL,
                file TEXT,
                etaInMilliSeconds INTEGER NOT NULL DEFAULT -1,
                bytesDownloaded INTEGER NOT NULL DEFAULT -1,
                totalSizeOfDownload INTEGER NOT NULL DEFAULT -1,
                status TEXT NOT NULL DEFAULT 'NONE',
                error TEXT NOT NULL DEFAULT 'NONE',
                progress INTEGER NOT NULL DEFAULT -1,
                bookId TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                language TEXT NOT NULL,
                creator TEXT NOT NULL,
                publisher TEXT NOT NULL,
                date TEXT NOT NULL,
                url TEXT,
                articleCount TEXT,
                mediaCount TEXT,
                size TEXT NOT NULL,
                name TEXT,
                favIcon TEXT NOT NULL,
                tags TEXT
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            INSERT INTO DownloadRoomEntity_new (
                id, downloadId, file, etaInMilliSeconds, bytesDownloaded,
                totalSizeOfDownload, status, error, progress, bookId, title, description,
                language, creator, publisher, date, url, articleCount, mediaCount, size,
                name, favIcon, tags
            )
            SELECT id, downloadId, file, etaInMilliSeconds, bytesDownloaded,
                totalSizeOfDownload, status, error, progress, bookId, title, description,
                language, creator, publisher, date, url, articleCount, mediaCount, size,
                name, favIcon, tags
            FROM DownloadRoomEntity
            """.trimIndent()
          )

          db.execSQL("DROP TABLE DownloadRoomEntity")
          db.execSQL("ALTER TABLE DownloadRoomEntity_new RENAME TO DownloadRoomEntity")
        }
      }

    @Suppress("MagicNumber")
    private val MIGRATION_7_8 =
      object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
          database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `WebViewHistoryEntity` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `zimId` TEXT NOT NULL,
                `webViewIndex` INTEGER NOT NULL,
                `webViewCurrentPosition` INTEGER NOT NULL,
                `webViewBackForwardListBundle` BLOB NULL
            )
            """
          )
        }
      }

    @Suppress("MagicNumber")
    private val MIGRATION_8_9 =
      object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS DownloadRoomEntity_new (
                 id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                 downloadId INTEGER NOT NULL,
                 file TEXT,
                 etaInMilliSeconds INTEGER NOT NULL DEFAULT -1,
                 bytesDownloaded INTEGER NOT NULL DEFAULT -1,
                 totalSizeOfDownload INTEGER NOT NULL DEFAULT -1,
                 status INTEGER NOT NULL DEFAULT 0,
                 error INTEGER NOT NULL DEFAULT 0,
                 progress INTEGER NOT NULL DEFAULT -1,
                 bookId TEXT NOT NULL,
                 title TEXT NOT NULL,
                 description TEXT,
                 language TEXT NOT NULL,
                 creator TEXT NOT NULL,
                 publisher TEXT NOT NULL,
                 date TEXT NOT NULL,
                 url TEXT,
                 articleCount TEXT,
                 mediaCount TEXT,
                 size TEXT NOT NULL,
                 name TEXT,
                 favIcon TEXT NOT NULL,
                 tags TEXT
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            INSERT INTO DownloadRoomEntity_new (
                 id, downloadId, file, etaInMilliSeconds, bytesDownloaded,
                 totalSizeOfDownload, status, error, progress, bookId, title,
                 description, language, creator, publisher, date, url,
                 articleCount, mediaCount, size, name, favIcon, tags
            )
            SELECT id, downloadId, file, etaInMilliSeconds, bytesDownloaded,
                 totalSizeOfDownload, status, error, progress, bookId, title, description,
                 language, creator, publisher, date, url, articleCount,
                 mediaCount, size, name, favIcon, tags
                 FROM DownloadRoomEntity
            """.trimIndent()
          )

          db.execSQL("DROP TABLE DownloadRoomEntity")
          db.execSQL("ALTER TABLE DownloadRoomEntity_new RENAME TO DownloadRoomEntity")
        }
      }

    @Suppress("MagicNumber")
    private val MIGRATION_9_10 =
      object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "ALTER TABLE DownloadRoomEntity ADD COLUMN pauseReason INTEGER NOT NULL DEFAULT 0"
          )
        }
      }

    fun destroyInstance() {
      db = null
    }
  }
}
