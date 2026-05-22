/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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
import android.os.Build
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class KiwixRoomDatabaseMigrationTest {
  private lateinit var db: SupportSQLiteDatabase
  private lateinit var roomDb: KiwixRoomDatabase

  private val allMigrations = listOf(
    KiwixRoomDatabase.MIGRATION_1_2,
    KiwixRoomDatabase.MIGRATION_2_3,
    KiwixRoomDatabase.MIGRATION_3_4,
    KiwixRoomDatabase.MIGRATION_4_5,
    KiwixRoomDatabase.MIGRATION_5_6,
    KiwixRoomDatabase.MIGRATION_6_7,
    KiwixRoomDatabase.MIGRATION_7_8,
    KiwixRoomDatabase.MIGRATION_8_9,
    KiwixRoomDatabase.MIGRATION_9_10,
  )

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    roomDb = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    db = roomDb.openHelper.writableDatabase
    dropAllTables()
    createVersion1Schema()
  }

  @After
  fun tearDown() {
    roomDb.close()
  }

  private fun dropAllTables() {
    val tables = mutableListOf<String>()
    db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")
      .use { cursor ->
        while (cursor.moveToNext()) tables.add(cursor.getString(0))
      }
    tables.forEach { db.execSQL("DROP TABLE IF EXISTS `$it`") }
  }

  private fun createVersion1Schema() {
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS RecentSearchRoomEntity (
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        searchTerm TEXT NOT NULL,
        zimId TEXT NOT NULL,
        url TEXT NOT NULL
      )
      """
    )
  }

  private fun migrateRange(fromVersion: Int, toVersion: Int) {
    allMigrations
      .drop(fromVersion - 1)
      .take(toVersion - fromVersion)
      .forEach { it.migrate(db) }
  }

  private fun insertRecentSearch(id: Int, searchTerm: String) {
    db.execSQL(
      """
      INSERT INTO RecentSearchRoomEntity (id, searchTerm, zimId, url)
      VALUES ($id, '$searchTerm', 'zim-1', 'https://kiwix.org.com')
      """
    )
  }

  private fun insertHistory(zimId: String, title: String) {
    db.execSQL(
      """
      INSERT INTO HistoryRoomEntity (
        zimId, zimName, zimFilePath, favicon,
        historyUrl, historyTitle, dateString, timeStamp
      ) VALUES (
        '$zimId', 'testZim', '/data/test.zim', NULL,
        'https://kiwix.org.com', '$title', '22 Aug 2005', 1000
      )
      """
    )
  }

  private fun insertNote(zimId: String, noteTitle: String) {
    db.execSQL(
      """
      INSERT INTO NotesRoomEntity (
        zimId, zimFilePath, zimUrl, noteTitle, noteFilePath, favicon
      ) VALUES (
        '$zimId', NULL, 'https://kiwix.org.com', '$noteTitle', '/notes/test.txt', NULL
      )
      """
    )
  }

  private fun insertDownloadWithTextStatus(downloadId: Long, bookId: String, title: String) {
    db.execSQL(
      """
      INSERT INTO DownloadRoomEntity (
        downloadId, file, etaInMilliSeconds, bytesDownloaded, totalSizeOfDownload,
        status, error, progress, bookId, title, description, language, creator,
        publisher, date, url, articleCount, mediaCount, size, name, favIcon, tags
      ) VALUES (
        $downloadId, 'test.zim', 1000, 500, 1000,
        'NONE', 'NONE', 50, '$bookId', '$title', 'Test description', 'en', 'DevDocs',
        'Kiwix', '2005-08-22', 'https://kiwix.org.com', '10', '5', '10MB', '$title', 'icon.png', 'tag1'
      )
      """
    )
  }

  private fun insertDownloadWithIntStatus(downloadId: Long, bookId: String, title: String) {
    db.execSQL(
      """
      INSERT INTO DownloadRoomEntity (
        downloadId, file, etaInMilliSeconds, bytesDownloaded, totalSizeOfDownload,
        status, error, progress, bookId, title, description, language, creator,
        publisher, date, url, articleCount, mediaCount, size, name, favIcon, tags
      ) VALUES (
        $downloadId, 'test.zim', 1000, 500, 1000,
        0, 0, 50, '$bookId', '$title', 'Test description', 'en', 'DevDocs',
        'Kiwix', '2005-08-22', 'https://kiwix.org.com', '10', '5', '10MB', '$title', 'icon.png', 'tag1'
      )
      """
    )
  }

  @Test
  fun migration1To2_recentSearchDataSurvives() {
    // V2 adds: HistoryRoomEntity
    insertRecentSearch(id = 1, searchTerm = "kotlin")

    migrateRange(fromVersion = 1, toVersion = 2)

    val recentSearchCursor = db.query("SELECT * FROM RecentSearchRoomEntity WHERE id = 1")
    recentSearchCursor.moveToFirst()
    assertEquals(
      "kotlin",
      recentSearchCursor.getString(recentSearchCursor.getColumnIndexOrThrow("searchTerm"))
    )
    assertEquals(
      "zim-1",
      recentSearchCursor.getString(recentSearchCursor.getColumnIndexOrThrow("zimId"))
    )
    assertEquals(
      "https://kiwix.org.com",
      recentSearchCursor.getString(recentSearchCursor.getColumnIndexOrThrow("url"))
    )
    recentSearchCursor.close()
  }

  @Test
  fun migration2To3_recentSearchAndHistoryDataSurvives() {
    // V3 adds: NotesRoomEntity + unique index on noteTitle
    migrateRange(fromVersion = 1, toVersion = 2)
    insertRecentSearch(id = 1, searchTerm = "kotlin")
    insertHistory(zimId = "zim-1", title = "History At V2")

    migrateRange(fromVersion = 2, toVersion = 3)

    val recentSearchCursor = db.query("SELECT * FROM RecentSearchRoomEntity WHERE id = 1")
    recentSearchCursor.moveToFirst()
    assertEquals(
      "kotlin",
      recentSearchCursor.getString(recentSearchCursor.getColumnIndexOrThrow("searchTerm"))
    )
    recentSearchCursor.close()

    // History
    val historyCursor = db.query("SELECT * FROM HistoryRoomEntity WHERE zimId = 'zim-1'")
    historyCursor.moveToFirst()
    assertEquals(
      "History At V2",
      historyCursor.getString(historyCursor.getColumnIndexOrThrow("historyTitle"))
    )
    assertEquals(
      "https://kiwix.org.com",
      historyCursor.getString(historyCursor.getColumnIndexOrThrow("historyUrl"))
    )
    assertEquals(1000L, historyCursor.getLong(historyCursor.getColumnIndexOrThrow("timeStamp")))
    historyCursor.close()
  }

  @Test
  fun migration3To4_existingDataSurvives() {
    // V4 adds: DownloadRoomEntity (status, error as TEXT)
    migrateRange(fromVersion = 1, toVersion = 3)
    insertRecentSearch(id = 1, searchTerm = "kotlin")
    insertHistory(zimId = "zim-1", title = "History At V3")
    insertNote(zimId = "zim-1", noteTitle = "Note At V3")

    migrateRange(fromVersion = 3, toVersion = 4)

    val recentSearchCursor = db.query("SELECT * FROM RecentSearchRoomEntity WHERE id = 1")
    recentSearchCursor.moveToFirst()
    assertEquals(
      "kotlin",
      recentSearchCursor.getString(recentSearchCursor.getColumnIndexOrThrow("searchTerm"))
    )
    recentSearchCursor.close()

    val historyCursor = db.query("SELECT * FROM HistoryRoomEntity WHERE zimId = 'zim-1'")
    historyCursor.moveToFirst()
    assertEquals(
      "History At V3",
      historyCursor.getString(historyCursor.getColumnIndexOrThrow("historyTitle"))
    )
    historyCursor.close()

    val notesCursor = db.query("SELECT * FROM NotesRoomEntity WHERE zimId = 'zim-1'")
    notesCursor.moveToFirst()
    assertEquals(
      "Note At V3",
      notesCursor.getString(notesCursor.getColumnIndexOrThrow("noteTitle"))
    )
    notesCursor.close()
  }

  @Test
  fun migration4To5_existingDataSurvivesHistoryTableRebuild() {
    // V5 rebuilds: HistoryRoomEntity + adds zimReaderSource to History and Notes
    migrateRange(fromVersion = 1, toVersion = 4)
    insertRecentSearch(id = 1, searchTerm = "kotlin")
    insertHistory(zimId = "zim-1", title = "History At V4")
    insertNote(zimId = "zim-1", noteTitle = "Note At V4")
    insertDownloadWithTextStatus(downloadId = 1, bookId = "book-1", title = "Download At V4")

    migrateRange(fromVersion = 4, toVersion = 5)

    val recentSearchCursor = db.query("SELECT * FROM RecentSearchRoomEntity WHERE id = 1")
    recentSearchCursor.moveToFirst()
    assertEquals(
      "kotlin",
      recentSearchCursor.getString(recentSearchCursor.getColumnIndexOrThrow("searchTerm"))
    )
    recentSearchCursor.close()

    val historyCursor = db.query("SELECT * FROM HistoryRoomEntity WHERE zimId = 'zim-1'")
    historyCursor.moveToFirst()
    assertEquals("zim-1", historyCursor.getString(historyCursor.getColumnIndexOrThrow("zimId")))
    assertEquals(
      "History At V4",
      historyCursor.getString(historyCursor.getColumnIndexOrThrow("historyTitle"))
    )
    assertEquals(
      "/data/test.zim",
      historyCursor.getString(historyCursor.getColumnIndexOrThrow("zimFilePath"))
    )
    assertEquals(
      "https://kiwix.org.com",
      historyCursor.getString(historyCursor.getColumnIndexOrThrow("historyUrl"))
    )
    assertEquals(1000L, historyCursor.getLong(historyCursor.getColumnIndexOrThrow("timeStamp")))
    historyCursor.close()

    val notesCursor = db.query("SELECT * FROM NotesRoomEntity WHERE zimId = 'zim-1'")
    notesCursor.moveToFirst()
    assertEquals(
      "Note At V4",
      notesCursor.getString(notesCursor.getColumnIndexOrThrow("noteTitle"))
    )
    notesCursor.close()

    val downloadCursor = db.query("SELECT * FROM DownloadRoomEntity WHERE downloadId = 1")
    downloadCursor.moveToFirst()
    assertEquals("book-1", downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("bookId")))
    assertEquals(
      "Download At V4",
      downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("title"))
    )
    downloadCursor.close()
  }

  @Test
  fun migration5To6_existingDataSurvives() {
    // V6 adds: pausedByUser column to DownloadRoomEntity
    migrateRange(fromVersion = 1, toVersion = 5)
    insertRecentSearch(id = 1, searchTerm = "kotlin")
    insertHistory(zimId = "zim-1", title = "History At V5")
    insertNote(zimId = "zim-1", noteTitle = "Note At V5")
    insertDownloadWithTextStatus(downloadId = 1, bookId = "book-1", title = "Download At V5")

    migrateRange(fromVersion = 5, toVersion = 6)

    val recentSearchCursor = db.query("SELECT * FROM RecentSearchRoomEntity WHERE id = 1")
    recentSearchCursor.moveToFirst()
    assertEquals(
      "kotlin",
      recentSearchCursor.getString(recentSearchCursor.getColumnIndexOrThrow("searchTerm"))
    )
    recentSearchCursor.close()

    val historyCursor = db.query("SELECT * FROM HistoryRoomEntity WHERE zimId = 'zim-1'")
    historyCursor.moveToFirst()
    assertEquals(
      "History At V5",
      historyCursor.getString(historyCursor.getColumnIndexOrThrow("historyTitle"))
    )
    historyCursor.close()

    val notesCursor = db.query("SELECT * FROM NotesRoomEntity WHERE zimId = 'zim-1'")
    notesCursor.moveToFirst()
    assertEquals(
      "Note At V5",
      notesCursor.getString(notesCursor.getColumnIndexOrThrow("noteTitle"))
    )
    notesCursor.close()

    val downloadCursor = db.query("SELECT * FROM DownloadRoomEntity WHERE downloadId = 1")
    downloadCursor.moveToFirst()
    assertEquals("book-1", downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("bookId")))
    assertEquals(
      "Download At V5",
      downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("title"))
    )
    assertEquals(
      500L,
      downloadCursor.getLong(downloadCursor.getColumnIndexOrThrow("bytesDownloaded"))
    )
    assertEquals(50, downloadCursor.getInt(downloadCursor.getColumnIndexOrThrow("progress")))
    assertEquals("en", downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("language")))
    assertEquals(
      "DevDocs",
      downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("creator"))
    )
    downloadCursor.close()
  }

  @Test
  fun migration6To7_existingDataSurvivesDownloadTableRebuild() {
    // V7 rebuilds: DownloadRoomEntity which drops pausedByUser column added in V6
    migrateRange(fromVersion = 1, toVersion = 6)
    insertRecentSearch(id = 1, searchTerm = "kotlin")
    insertHistory(zimId = "zim-1", title = "History At V6")
    insertNote(zimId = "zim-1", noteTitle = "Note At V6")
    insertDownloadWithTextStatus(downloadId = 1, bookId = "book-2", title = "Download At V6")

    migrateRange(fromVersion = 6, toVersion = 7)

    val recentSearchCursor = db.query("SELECT * FROM RecentSearchRoomEntity WHERE id = 1")
    recentSearchCursor.moveToFirst()
    assertEquals(
      "kotlin",
      recentSearchCursor.getString(recentSearchCursor.getColumnIndexOrThrow("searchTerm"))
    )
    recentSearchCursor.close()

    val historyCursor = db.query("SELECT * FROM HistoryRoomEntity WHERE zimId = 'zim-1'")
    historyCursor.moveToFirst()
    assertEquals(
      "History At V6",
      historyCursor.getString(historyCursor.getColumnIndexOrThrow("historyTitle"))
    )
    historyCursor.close()

    val notesCursor = db.query("SELECT * FROM NotesRoomEntity WHERE zimId = 'zim-1'")
    notesCursor.moveToFirst()
    assertEquals(
      "Note At V6",
      notesCursor.getString(notesCursor.getColumnIndexOrThrow("noteTitle"))
    )
    notesCursor.close()

    val downloadCursor = db.query("SELECT * FROM DownloadRoomEntity WHERE downloadId = 1")
    downloadCursor.moveToFirst()
    assertEquals("book-2", downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("bookId")))
    assertEquals(
      "Download At V6",
      downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("title"))
    )
    assertEquals(
      500L,
      downloadCursor.getLong(downloadCursor.getColumnIndexOrThrow("bytesDownloaded"))
    )
    assertEquals(
      1000L,
      downloadCursor.getLong(downloadCursor.getColumnIndexOrThrow("totalSizeOfDownload"))
    )
    assertEquals(50, downloadCursor.getInt(downloadCursor.getColumnIndexOrThrow("progress")))
    assertEquals("en", downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("language")))
    assertEquals(
      "DevDocs",
      downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("creator"))
    )
    assertEquals(
      "https://kiwix.org.com",
      downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("url"))
    )
    downloadCursor.close()
  }

  @Test
  fun migration7To8_existingDataSurvives() {
    // V8 adds: WebViewHistoryEntity
    migrateRange(fromVersion = 1, toVersion = 7)
    insertRecentSearch(id = 1, searchTerm = "kotlin")
    insertHistory(zimId = "zim-1", title = "History At V7")
    insertNote(zimId = "zim-1", noteTitle = "Note At V7")
    insertDownloadWithTextStatus(downloadId = 1, bookId = "book-3", title = "Download At V7")

    migrateRange(fromVersion = 7, toVersion = 8)

    val recentSearchCursor = db.query("SELECT * FROM RecentSearchRoomEntity WHERE id = 1")
    recentSearchCursor.moveToFirst()
    assertEquals(
      "kotlin",
      recentSearchCursor.getString(recentSearchCursor.getColumnIndexOrThrow("searchTerm"))
    )
    recentSearchCursor.close()

    val historyCursor = db.query("SELECT * FROM HistoryRoomEntity WHERE zimId = 'zim-1'")
    historyCursor.moveToFirst()
    assertEquals(
      "History At V7",
      historyCursor.getString(historyCursor.getColumnIndexOrThrow("historyTitle"))
    )
    assertEquals(
      "https://kiwix.org.com",
      historyCursor.getString(historyCursor.getColumnIndexOrThrow("historyUrl"))
    )
    historyCursor.close()

    val notesCursor = db.query("SELECT * FROM NotesRoomEntity WHERE zimId = 'zim-1'")
    notesCursor.moveToFirst()
    assertEquals(
      "Note At V7",
      notesCursor.getString(notesCursor.getColumnIndexOrThrow("noteTitle"))
    )
    notesCursor.close()

    val downloadCursor = db.query("SELECT * FROM DownloadRoomEntity WHERE downloadId = 1")
    downloadCursor.moveToFirst()
    assertEquals("book-3", downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("bookId")))
    assertEquals(
      "Download At V7",
      downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("title"))
    )
    downloadCursor.close()
  }

  @Test
  fun migration8To9_existingDataSurvivesAndTextStatusBecomesInteger() {
    // V9 rebuilds: DownloadRoomEntity — status,error data-type changed from TEXT 'NONE' → INTEGER 0
    migrateRange(fromVersion = 1, toVersion = 8)
    insertRecentSearch(id = 1, searchTerm = "kotlin")
    insertHistory(zimId = "zim-1", title = "History At V8")
    insertNote(zimId = "zim-1", noteTitle = "Note At V8")
    insertDownloadWithTextStatus(downloadId = 1, bookId = "book-4", title = "Download At V8")

    migrateRange(fromVersion = 8, toVersion = 9)

    val recentSearchCursor = db.query("SELECT * FROM RecentSearchRoomEntity WHERE id = 1")
    recentSearchCursor.moveToFirst()
    assertEquals(
      "kotlin",
      recentSearchCursor.getString(recentSearchCursor.getColumnIndexOrThrow("searchTerm"))
    )
    recentSearchCursor.close()

    val historyCursor = db.query("SELECT * FROM HistoryRoomEntity WHERE zimId = 'zim-1'")
    historyCursor.moveToFirst()
    assertEquals(
      "History At V8",
      historyCursor.getString(historyCursor.getColumnIndexOrThrow("historyTitle"))
    )
    historyCursor.close()

    val notesCursor = db.query("SELECT * FROM NotesRoomEntity WHERE zimId = 'zim-1'")
    notesCursor.moveToFirst()
    assertEquals(
      "Note At V8",
      notesCursor.getString(notesCursor.getColumnIndexOrThrow("noteTitle"))
    )
    notesCursor.close()

    val downloadCursor = db.query("SELECT * FROM DownloadRoomEntity WHERE downloadId = 1")
    downloadCursor.moveToFirst()
    assertEquals("book-4", downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("bookId")))
    assertEquals(
      "Download At V8",
      downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("title"))
    )
    assertEquals(
      500L,
      downloadCursor.getLong(downloadCursor.getColumnIndexOrThrow("bytesDownloaded"))
    )
    assertEquals(
      1000L,
      downloadCursor.getLong(downloadCursor.getColumnIndexOrThrow("totalSizeOfDownload"))
    )
    assertEquals(50, downloadCursor.getInt(downloadCursor.getColumnIndexOrThrow("progress")))
    assertEquals("en", downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("language")))
    assertEquals(
      "DevDocs",
      downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("creator"))
    )
    assertEquals(
      "https://kiwix.org.com",
      downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("url"))
    )
    assertEquals(0, downloadCursor.getInt(downloadCursor.getColumnIndexOrThrow("status")))
    assertEquals(0, downloadCursor.getInt(downloadCursor.getColumnIndexOrThrow("error")))
    downloadCursor.close()
  }

  @Test
  fun migration9To10_existingDataSurvivesAndPauseReasonDefaultsToZero() {
    // V10 adds: pauseReason column to DownloadRoomEntity with DEFAULT 0
    migrateRange(fromVersion = 1, toVersion = 9)
    insertRecentSearch(id = 1, searchTerm = "kotlin")
    insertHistory(zimId = "zim-1", title = "History At V9")
    insertNote(zimId = "zim-1", noteTitle = "Note At V9")
    insertDownloadWithIntStatus(downloadId = 1, bookId = "book-5", title = "Download At V9")

    migrateRange(fromVersion = 9, toVersion = 10)

    val recentSearchCursor = db.query("SELECT * FROM RecentSearchRoomEntity WHERE id = 1")
    recentSearchCursor.moveToFirst()
    assertEquals(
      "kotlin",
      recentSearchCursor.getString(recentSearchCursor.getColumnIndexOrThrow("searchTerm"))
    )
    recentSearchCursor.close()

    val historyCursor = db.query("SELECT * FROM HistoryRoomEntity WHERE zimId = 'zim-1'")
    historyCursor.moveToFirst()
    assertEquals(
      "History At V9",
      historyCursor.getString(historyCursor.getColumnIndexOrThrow("historyTitle"))
    )
    historyCursor.close()

    val notesCursor = db.query("SELECT * FROM NotesRoomEntity WHERE zimId = 'zim-1'")
    notesCursor.moveToFirst()
    assertEquals(
      "Note At V9",
      notesCursor.getString(notesCursor.getColumnIndexOrThrow("noteTitle"))
    )
    notesCursor.close()

    val downloadCursor = db.query("SELECT * FROM DownloadRoomEntity WHERE downloadId = 1")
    downloadCursor.moveToFirst()
    assertEquals("book-5", downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("bookId")))
    assertEquals(
      "Download At V9",
      downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("title"))
    )
    assertEquals(
      500L,
      downloadCursor.getLong(downloadCursor.getColumnIndexOrThrow("bytesDownloaded"))
    )
    assertEquals(
      1000L,
      downloadCursor.getLong(downloadCursor.getColumnIndexOrThrow("totalSizeOfDownload"))
    )
    assertEquals(50, downloadCursor.getInt(downloadCursor.getColumnIndexOrThrow("progress")))
    assertEquals("en", downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("language")))
    assertEquals(
      "DevDocs",
      downloadCursor.getString(downloadCursor.getColumnIndexOrThrow("creator"))
    )
    assertEquals(0, downloadCursor.getInt(downloadCursor.getColumnIndexOrThrow("status")))
    assertEquals(0, downloadCursor.getInt(downloadCursor.getColumnIndexOrThrow("error")))
    assertEquals(0, downloadCursor.getInt(downloadCursor.getColumnIndexOrThrow("pauseReason")))
    downloadCursor.close()
  }
}
