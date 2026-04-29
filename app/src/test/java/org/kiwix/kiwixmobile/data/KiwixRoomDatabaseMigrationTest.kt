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

package org.kiwix.kiwixmobile.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for all database migrations (MIGRATION_1_2 through MIGRATION_9_10)
 * in [KiwixRoomDatabase]. Verifies that each migration's SQL executes
 * successfully and produces the expected schema changes.
 *
 * We obtain a raw [SupportSQLiteDatabase] by opening the Room-managed
 * in-memory database, dropping all auto-created tables, and recreating
 * only the version-1 schema so that migrations can be applied incrementally.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class KiwixRoomDatabaseMigrationTest {
  private lateinit var db: SupportSQLiteDatabase
  private lateinit var roomDb: KiwixRoomDatabase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    roomDb = Room.inMemoryDatabaseBuilder(context, KiwixRoomDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    db = roomDb.openHelper.writableDatabase

    // Drop all Room-created tables so we start from a clean slate
    val tables = mutableListOf<String>()
    db.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")
      .use { cursor ->
        while (cursor.moveToNext()) {
          tables.add(cursor.getString(0))
        }
      }
    tables.forEach { db.execSQL("DROP TABLE IF EXISTS `$it`") }

    // Create only the version-1 schema
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS `RecentSearchRoomEntity` (
        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `searchTerm` TEXT NOT NULL,
        `zimId` TEXT NOT NULL,
        `url` TEXT NOT NULL
      )
      """
    )
  }

  @After
  fun tearDown() {
    roomDb.close()
  }

  /** Returns true when the given table exists in the database. */
  private fun tableExists(tableName: String): Boolean =
    db.query(
      "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
      arrayOf(tableName)
    ).use { it.count > 0 }

  /** Returns the column names of the given table. */
  private fun columnNames(tableName: String): Set<String> {
    val columns = mutableSetOf<String>()
    db.query("PRAGMA table_info($tableName)").use { cursor ->
      val nameIndex = cursor.getColumnIndex("name")
      while (cursor.moveToNext()) {
        columns.add(cursor.getString(nameIndex))
      }
    }
    return columns
  }

  @Test
  fun migration1to2_createsHistoryRoomEntity() {
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    assertTrue("HistoryRoomEntity table should exist", tableExists("HistoryRoomEntity"))
    val cols = columnNames("HistoryRoomEntity")
    assertTrue(
      cols.containsAll(
        listOf(
          "id",
          "timeStamp",
          "zimId",
          "historyUrl",
          "zimName",
          "favicon",
          "historyTitle",
          "dateString",
          "zimFilePath"
        )
      )
    )
  }

  @Test
  fun migration2to3_createsNotesRoomEntity() {
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    assertTrue("NotesRoomEntity table should exist", tableExists("NotesRoomEntity"))
    val cols = columnNames("NotesRoomEntity")
    assertTrue(
      cols.containsAll(
        listOf(
          "id",
          "zimId",
          "zimFilePath",
          "zimUrl",
          "noteTitle",
          "noteFilePath",
          "favicon"
        )
      )
    )
  }

  @Test
  fun migration2to3_createsUniqueIndexOnNoteTitle() {
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    db.query(
      "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='NotesRoomEntity'"
    ).use { cursor ->
      val indexNames = mutableListOf<String>()
      while (cursor.moveToNext()) {
        indexNames.add(cursor.getString(0))
      }
      assertTrue(
        "Unique index on noteTitle should exist",
        indexNames.any { it.contains("noteTitle", ignoreCase = true) }
      )
    }
  }

  @Test
  fun migration3to4_createsDownloadRoomEntity() {
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)
    assertTrue("DownloadRoomEntity table should exist", tableExists("DownloadRoomEntity"))
    val cols = columnNames("DownloadRoomEntity")
    assertTrue(
      cols.containsAll(
        listOf(
          "id",
          "downloadId",
          "bookId",
          "title",
          "status",
          "error",
          "progress",
          "size",
          "favIcon"
        )
      )
    )
  }

  @Test
  fun migration4to5_addsZimReaderSourceAndPreservesData() {
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)

    // Insert a history row before migration
    val cv = ContentValues().apply {
      put("id", 1)
      put("timeStamp", 1000L)
      put("zimId", "test-zim")
      put("historyUrl", "https://example.com")
      put("zimName", "testZim")
      put("historyTitle", "Test Title")
      put("dateString", "01 Jan 2024")
      put("zimFilePath", "/path/to/file.zim")
    }
    db.insert("HistoryRoomEntity", SQLiteDatabase.CONFLICT_REPLACE, cv)
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)

    val historyCols = columnNames("HistoryRoomEntity")
    assertTrue(
      "zimReaderSource column should exist in HistoryRoomEntity",
      historyCols.contains("zimReaderSource")
    )

    // Verify data was preserved
    db.query("SELECT * FROM HistoryRoomEntity WHERE zimId = 'test-zim'").use { cursor ->
      assertTrue("History data should be preserved after migration", cursor.moveToFirst())
      assertEquals(
        "Test Title",
        cursor.getString(cursor.getColumnIndex("historyTitle"))
      )
    }

    val notesCols = columnNames("NotesRoomEntity")
    assertTrue(
      "zimReaderSource column should exist in NotesRoomEntity",
      notesCols.contains("zimReaderSource")
    )
  }

  @Test
  fun migration5to6_addsPausedByUserColumn() {
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)
    KiwixRoomDatabase.MIGRATION_5_6.migrate(db)
    val cols = columnNames("DownloadRoomEntity")
    assertTrue("pausedByUser column should exist", cols.contains("pausedByUser"))
  }

  @Test
  fun migration6to7_recreatesDownloadRoomEntityWithoutPausedByUser() {
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)
    KiwixRoomDatabase.MIGRATION_5_6.migrate(db)

    val cv = ContentValues().apply {
      put("downloadId", 42)
      put("bookId", "book-1")
      put("title", "Test Book")
      put("language", "en")
      put("creator", "Test")
      put("publisher", "Test Pub")
      put("date", "2024-01-01")
      put("size", "100MB")
      put("favIcon", "icon.png")
    }
    db.insert("DownloadRoomEntity", SQLiteDatabase.CONFLICT_REPLACE, cv)
    KiwixRoomDatabase.MIGRATION_6_7.migrate(db)

    assertTrue("DownloadRoomEntity should still exist", tableExists("DownloadRoomEntity"))
    assertFalse(
      "Should not contain pausedByUser",
      columnNames("DownloadRoomEntity").contains("pausedByUser")
    )

    db.query("SELECT * FROM DownloadRoomEntity WHERE bookId = 'book-1'").use { cursor ->
      assertTrue("Download data should be preserved", cursor.moveToFirst())
      assertEquals("Test Book", cursor.getString(cursor.getColumnIndex("title")))
    }
  }

  @Test
  fun migration7to8_createsWebViewHistoryEntity() {
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)
    KiwixRoomDatabase.MIGRATION_5_6.migrate(db)
    KiwixRoomDatabase.MIGRATION_6_7.migrate(db)
    KiwixRoomDatabase.MIGRATION_7_8.migrate(db)
    assertTrue("WebViewHistoryEntity should exist", tableExists("WebViewHistoryEntity"))
    val cols = columnNames("WebViewHistoryEntity")
    assertTrue(
      cols.containsAll(
        listOf(
          "id",
          "zimId",
          "webViewIndex",
          "webViewCurrentPosition",
          "webViewBackForwardListBundle"
        )
      )
    )
  }

  @Test
  fun migration8to9_convertsStatusAndErrorToInteger() {
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)
    KiwixRoomDatabase.MIGRATION_5_6.migrate(db)
    KiwixRoomDatabase.MIGRATION_6_7.migrate(db)
    KiwixRoomDatabase.MIGRATION_7_8.migrate(db)
    KiwixRoomDatabase.MIGRATION_8_9.migrate(db)
    assertTrue(
      "DownloadRoomEntity should exist after migration 8 to 9",
      tableExists("DownloadRoomEntity")
    )
    db.query("PRAGMA table_info(DownloadRoomEntity)").use { cursor ->
      val nameIdx = cursor.getColumnIndex("name")
      val typeIdx = cursor.getColumnIndex("type")
      while (cursor.moveToNext()) {
        val colName = cursor.getString(nameIdx)
        val colType = cursor.getString(typeIdx)
        if (colName == "status" || colName == "error") {
          assertEquals("status/error should be INTEGER", "INTEGER", colType)
        }
      }
    }
  }

  @Test
  fun migration9to10_addsPauseReasonColumn() {
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)
    KiwixRoomDatabase.MIGRATION_5_6.migrate(db)
    KiwixRoomDatabase.MIGRATION_6_7.migrate(db)
    KiwixRoomDatabase.MIGRATION_7_8.migrate(db)
    KiwixRoomDatabase.MIGRATION_8_9.migrate(db)
    KiwixRoomDatabase.MIGRATION_9_10.migrate(db)
    val cols = columnNames("DownloadRoomEntity")
    assertTrue("pauseReason column should exist", cols.contains("pauseReason"))
  }

  @Test
  fun allMigrations_executeSuccessfully() {
    KiwixRoomDatabase.MIGRATION_1_2.migrate(db)
    KiwixRoomDatabase.MIGRATION_2_3.migrate(db)
    KiwixRoomDatabase.MIGRATION_3_4.migrate(db)
    KiwixRoomDatabase.MIGRATION_4_5.migrate(db)
    KiwixRoomDatabase.MIGRATION_5_6.migrate(db)
    KiwixRoomDatabase.MIGRATION_6_7.migrate(db)
    KiwixRoomDatabase.MIGRATION_7_8.migrate(db)
    KiwixRoomDatabase.MIGRATION_8_9.migrate(db)
    KiwixRoomDatabase.MIGRATION_9_10.migrate(db)
    assertTrue(tableExists("RecentSearchRoomEntity"))
    assertTrue(tableExists("HistoryRoomEntity"))
    assertTrue(tableExists("NotesRoomEntity"))
    assertTrue(tableExists("DownloadRoomEntity"))
    assertTrue(tableExists("WebViewHistoryEntity"))
  }
}
