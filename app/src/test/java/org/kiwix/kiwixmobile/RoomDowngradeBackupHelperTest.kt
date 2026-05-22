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

package org.kiwix.kiwixmobile

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import org.kiwix.kiwixmobile.core.data.RoomDowngradeBackupHelper
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class RoomDowngradeBackupHelperTest {
  private lateinit var context: Context
  private lateinit var databaseFile: File

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()

    databaseFile = context.getDatabasePath(RoomDowngradeBackupHelper.DB_NAME)
    cleanupDatabase()

    KiwixRoomDatabase.destroyInstance()
  }

  @After
  fun teardown() {
    KiwixRoomDatabase.destroyInstance()
    cleanupDatabase()
  }

  private fun cleanupDatabase() {
    if (databaseFile.exists()) {
      databaseFile.deleteRecursively()
    }
  }

  private fun createDatabase(): KiwixRoomDatabase {
    return Room.databaseBuilder(
      context,
      KiwixRoomDatabase::class.java,
      RoomDowngradeBackupHelper.DB_NAME
    )
      .allowMainThreadQueries()
      .build()
  }

  private fun createDatabaseWithVersion(version: Int) {
    SQLiteDatabase.openOrCreateDatabase(
      databaseFile,
      null
    ).apply {
      this.version = version
      close()
    }
  }

  @Test
  fun `isDowngrade returns true when existing version is higher`() {
    createDatabaseWithVersion(version = 15)

    val result = RoomDowngradeBackupHelper.isDowngrade(context, targetVersion = 10)
    assertTrue(result)
  }

  @Test
  fun `isDowngrade returns false when existing version is lower`() {
    createDatabaseWithVersion(version = 5)

    val result = RoomDowngradeBackupHelper.isDowngrade(context, targetVersion = 10)
    assertFalse(result)
  }

  @Test
  fun `isDowngrade returns false when database does not exist`() {
    val result = RoomDowngradeBackupHelper.isDowngrade(context, targetVersion = 10)
    assertFalse(result)
  }

  @Test
  fun `createSnapshot backs up all supported tables`() = runBlocking {
    val db = createDatabase()

    db.notesRoomDao().saveNote(
      KiwixRoomDatabaseTest.getNoteListItem(
        title = "Note",
        zimUrl = "https://kiwix.app/note"
      )
    )

    db.historyRoomDao().saveHistory(
      KiwixRoomDatabaseTest.getHistoryItem(
        title = "History"
      )
    )

    db.recentSearchRoomDao().saveSearch(
      title = "kiwix",
      zimId = "zim-id",
      url = ""
    )

    val snapshot =
      RoomDowngradeBackupHelper.createSnapshot(context)

    assertEquals(1, snapshot.notes.size)
    assertEquals(1, snapshot.history.size)
    assertEquals(1, snapshot.recentSearches.size)

    db.close()
  }

  @Test
  fun `createSnapshot stores correct note values`() = runBlocking {
    val db = createDatabase()

    val note =
      KiwixRoomDatabaseTest.getNoteListItem(
        title = "Kiwix Note",
        zimId = "zim-123",
        zimUrl = "https://kiwix.app/note",
        noteFilePath = "/storage/notes/file.txt"
      )

    db.notesRoomDao().saveNote(note)

    val snapshot =
      RoomDowngradeBackupHelper.createSnapshot(context)

    val row = snapshot.notes.first().values

    assertEquals(note.title, row["noteTitle"])
    assertEquals(note.zimId, row["zimId"])
    assertEquals(note.zimUrl, row["zimUrl"])
    assertEquals(note.noteFilePath, row["noteFilePath"])

    db.close()
  }

  @Test
  fun `createSnapshot preserves null values`() = runBlocking {
    val db = createDatabase()

    val note =
      KiwixRoomDatabaseTest.getNoteListItem(
        zimUrl = "https://kiwix.app/note"
      )

    db.notesRoomDao().saveNote(note)

    val snapshot =
      RoomDowngradeBackupHelper.createSnapshot(context)

    val row = snapshot.notes.first().values

    assertNull(row["favicon"])

    db.close()
  }

  @Test
  fun `createSnapshot safely handles missing tables`() {
    createDatabaseWithVersion(version = 10)

    val snapshot =
      RoomDowngradeBackupHelper.createSnapshot(context)

    assertTrue(snapshot.notes.isEmpty())
    assertTrue(snapshot.history.isEmpty())
    assertTrue(snapshot.recentSearches.isEmpty())
  }

  @Test
  fun `restoreSnapshot restores complete note data`() = runBlocking {
    val originalDb = createDatabase()

    val originalNote =
      KiwixRoomDatabaseTest.getNoteListItem(
        title = "Alpine Wiki",
        zimId = "zim-123",
        zimUrl = "https://kiwix.app/note",
        noteFilePath = "/storage/notes/alpine.txt"
      )

    originalDb.notesRoomDao().saveNote(originalNote)

    val snapshot =
      RoomDowngradeBackupHelper.createSnapshot(context)

    originalDb.close()

    cleanupDatabase()

    val restoredDb = createDatabase()

    RoomDowngradeBackupHelper.restoreSnapshot(
      restoredDb.openHelper.writableDatabase,
      snapshot
    )

    val restoredNotes =
      restoredDb.notesRoomDao().notes().first()

    assertEquals(1, restoredNotes.size)

    val restored = restoredNotes.first()

    with(restored) {
      assertEquals(originalNote.title, title)
      assertEquals(originalNote.zimId, zimId)
      assertEquals(originalNote.zimUrl, url)
      assertEquals(originalNote.zimReaderSource, zimReaderSource)
      assertEquals(originalNote.favicon, favicon)
    }

    restoredDb.close()
  }

  @Test
  fun `restoreSnapshot restores multiple rows`() = runBlocking {
    val db = createDatabase()

    val snapshot =
      RoomDowngradeBackupHelper.DatabaseSnapshot(
        notes = listOf(
          RoomDowngradeBackupHelper.RowSnapshot(
            mapOf(
              "zimId" to "1",
              "zimUrl" to "url1",
              "noteTitle" to "title1",
              "noteFilePath" to "path1"
            )
          ),
          RoomDowngradeBackupHelper.RowSnapshot(
            mapOf(
              "zimId" to "2",
              "zimUrl" to "url2",
              "noteTitle" to "title2",
              "noteFilePath" to "path2"
            )
          )
        ),
        history = emptyList(),
        recentSearches = emptyList()
      )

    RoomDowngradeBackupHelper.restoreSnapshot(
      db.openHelper.writableDatabase,
      snapshot
    )

    val notes =
      db.notesRoomDao().notes().first()

    assertEquals(2, notes.size)

    db.close()
  }

  @Test
  fun `restoreSnapshot does nothing for empty snapshot`() = runBlocking {
    val db = createDatabase()

    RoomDowngradeBackupHelper.restoreSnapshot(
      db.openHelper.writableDatabase,
      RoomDowngradeBackupHelper.DatabaseSnapshot(
        notes = emptyList(),
        history = emptyList(),
        recentSearches = emptyList()
      )
    )

    val notes =
      db.notesRoomDao().notes().first()

    assertTrue(notes.isEmpty())

    db.close()
  }

  @Test
  fun `restoreSnapshot ignores unsupported columns from newer schema`() =
    runBlocking {
      val db = createDatabase()

      val snapshot =
        RoomDowngradeBackupHelper.DatabaseSnapshot(
          notes = listOf(
            RoomDowngradeBackupHelper.RowSnapshot(
              values = mapOf(
                // Valid columns
                "zimId" to "zim-123",
                "zimUrl" to "https://kiwix.app/note",
                "noteTitle" to "Kiwix Note",
                "noteFilePath" to "/storage/notes/kiwix.txt",
                // Future schema columns
                "futureColumn" to "ignored",
                "anotherField" to 12345
              )
            )
          ),
          history = emptyList(),
          recentSearches = emptyList()
        )

      RoomDowngradeBackupHelper.restoreSnapshot(
        db.openHelper.writableDatabase,
        snapshot
      )

      val notes =
        db.notesRoomDao().notes().first()

      assertEquals(1, notes.size)

      val restored = notes.first()

      assertEquals("zim-123", restored.zimId)
      assertEquals("Kiwix Note", restored.title)

      db.close()
    }

  @Test
  fun `restoreSnapshot skips rows with only unsupported columns`() =
    runBlocking {
      val db = createDatabase()

      val snapshot =
        RoomDowngradeBackupHelper.DatabaseSnapshot(
          notes = listOf(
            RoomDowngradeBackupHelper.RowSnapshot(
              mapOf(
                "futureOnlyColumn" to "value"
              )
            )
          ),
          history = emptyList(),
          recentSearches = emptyList()
        )

      RoomDowngradeBackupHelper.restoreSnapshot(
        db.openHelper.writableDatabase,
        snapshot
      )

      val notes =
        db.notesRoomDao().notes().first()

      assertTrue(notes.isEmpty())

      db.close()
    }

  @Test
  fun `restoreSnapshot ignores id column and generates new primary key`() =
    runBlocking {
      val db = createDatabase()

      val snapshot =
        RoomDowngradeBackupHelper.DatabaseSnapshot(
          notes = listOf(
            RoomDowngradeBackupHelper.RowSnapshot(
              mapOf(
                "id" to 999L,
                "zimId" to "zim-id",
                "zimUrl" to "url",
                "noteTitle" to "title",
                "noteFilePath" to "path"
              )
            )
          ),
          history = emptyList(),
          recentSearches = emptyList()
        )

      RoomDowngradeBackupHelper.restoreSnapshot(
        db.openHelper.writableDatabase,
        snapshot
      )

      val restored =
        db.notesRoomDao().notes().first().first()

      assertNotEquals(999L, restored.id)

      db.close()
    }

  @Test
  fun `restoreSnapshot restores blob values correctly`() {
    val db = createDatabase()

    val blob = byteArrayOf(1, 2, 3, 4)

    val sqliteDb: SupportSQLiteDatabase =
      db.openHelper.writableDatabase

    sqliteDb.execSQL(
      """
      INSERT INTO WebViewHistoryEntity(
        zimId,
        webViewIndex,
        webViewCurrentPosition,
        webViewBackForwardListBundle
      ) VALUES (?, ?, ?, ?)
      """.trimIndent(),
      arrayOf(
        "zim-id",
        1,
        2,
        blob
      )
    )

    val cursor =
      sqliteDb.query(
        "SELECT webViewBackForwardListBundle FROM WebViewHistoryEntity"
      )

    cursor.moveToFirst()

    val restoredBlob =
      cursor.getBlob(0)

    assertArrayEquals(blob, restoredBlob)

    cursor.close()
    db.close()
  }

  @Test
  fun `restoreSnapshot restores complete history data`() = runBlocking {
    val originalDb = createDatabase()

    val originalHistory =
      KiwixRoomDatabaseTest.getHistoryItem(
        title = "Main Page",
        historyUrl = "https://kiwix.app/A/MainPage",
        zimId = "zim-history-id"
      )

    originalDb.historyRoomDao().saveHistory(originalHistory)

    val snapshot =
      RoomDowngradeBackupHelper.createSnapshot(context)

    originalDb.close()

    cleanupDatabase()

    val restoredDb = createDatabase()

    RoomDowngradeBackupHelper.restoreSnapshot(
      restoredDb.openHelper.writableDatabase,
      snapshot
    )

    val restoredHistory =
      restoredDb.historyRoomDao()
        .historyRoomEntity()
        .first()

    assertEquals(1, restoredHistory.size)

    val restored = restoredHistory.first()

    with(restored) {
      assertEquals(originalHistory.title, historyTitle)
      assertEquals(originalHistory.zimId, zimId)
      assertEquals(originalHistory.historyUrl, historyUrl)
      assertEquals(originalHistory.zimName, zimName)
      assertEquals(originalHistory.favicon, favicon)
      assertEquals(originalHistory.dateString, dateString)
      assertEquals(originalHistory.timeStamp, timeStamp)
      assertEquals(
        originalHistory.zimReaderSource,
        zimReaderSource
      )
    }

    restoredDb.close()
  }

  @Test
  fun `restoreSnapshot restores recent searches`() = runBlocking {
    val originalDb = createDatabase()

    originalDb.recentSearchRoomDao().saveSearch(
      title = "kiwix",
      zimId = "zim-search-id",
      url = "https://kiwix.app/search"
    )

    val snapshot =
      RoomDowngradeBackupHelper.createSnapshot(context)

    originalDb.close()

    cleanupDatabase()

    val restoredDb = createDatabase()

    RoomDowngradeBackupHelper.restoreSnapshot(
      restoredDb.openHelper.writableDatabase,
      snapshot
    )

    val restoredSearches =
      restoredDb.recentSearchRoomDao()
        .search("zim-search-id")
        .first()

    assertEquals(1, restoredSearches.size)

    val restored = restoredSearches.first()

    assertEquals("kiwix", restored.searchTerm)
    assertEquals("zim-search-id", restored.zimId)
    assertEquals(
      "https://kiwix.app/search",
      restored.url
    )

    restoredDb.close()
  }

  @Test
  fun `restoreSnapshot restores all tables together`() = runBlocking {
    val originalDb = createDatabase()

    originalDb.notesRoomDao().saveNote(
      KiwixRoomDatabaseTest.getNoteListItem(
        zimUrl = "note-url"
      )
    )

    originalDb.historyRoomDao().saveHistory(
      KiwixRoomDatabaseTest.getHistoryItem()
    )

    originalDb.recentSearchRoomDao().saveSearch(
      title = "search",
      zimId = "zim-id",
      url = "url"
    )

    val snapshot =
      RoomDowngradeBackupHelper.createSnapshot(context)

    originalDb.close()

    cleanupDatabase()

    val restoredDb = createDatabase()

    RoomDowngradeBackupHelper.restoreSnapshot(
      restoredDb.openHelper.writableDatabase,
      snapshot
    )

    assertEquals(
      1,
      restoredDb.notesRoomDao().notes().first().size
    )

    assertEquals(
      1,
      restoredDb.historyRoomDao()
        .historyRoomEntity()
        .first()
        .size
    )

    assertEquals(
      1,
      restoredDb.recentSearchRoomDao()
        .search("zim-id")
        .first()
        .size
    )

    restoredDb.close()
  }
}
