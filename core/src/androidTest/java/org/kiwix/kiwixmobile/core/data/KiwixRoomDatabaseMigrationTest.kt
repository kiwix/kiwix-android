package org.kiwix.kiwixmobile.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KiwixRoomDatabaseMigrationTest {
  private val testDbName = "kiwix-migration-test"

  @get:Rule
  val helper: MigrationTestHelper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    KiwixRoomDatabase::class.java,
    emptyList(),
    FrameworkSQLiteOpenHelperFactory()
  )

  @Test
  fun migrate8To9() {
    var db = helper.createDatabase(testDbName, 8)

    db.execSQL(
      """
        INSERT INTO DownloadRoomEntity (
            downloadId, file, etaInMilliSeconds, bytesDownloaded, totalSizeOfDownload,
            status, error, progress, bookId, title, description, language, creator,
            publisher, date, url, articleCount, mediaCount, size, name, favIcon, tags
        ) VALUES (
            1, 'test.zim', 1000, 500, 1000,
            'NONE', 'NONE', 50, 'book_id', 'Kotlin Docs', 'Kotlin documentation, by DevDocs', 'en', 'DevDocs',
            'Kiwix', '2023-01-01', 'http://kiwix.org.com', '10', '5', '10MB', 'Kotlin Docs', 'icon.png', 'tag1'
        )
      """.trimIndent()
    )

    db.close()

    db = helper.runMigrationsAndValidate(
      testDbName,
      9,
      true,
      KiwixRoomDatabase.MIGRATION_8_9
    )

    val cursor = db.query("SELECT * FROM DownloadRoomEntity WHERE downloadId = 1")
    cursor.moveToFirst()

    assertEquals(1L, cursor.getLong(cursor.getColumnIndex("downloadId")))
    assertEquals("test.zim", cursor.getString(cursor.getColumnIndex("file")))
    assertEquals(1000L, cursor.getLong(cursor.getColumnIndex("etaInMilliSeconds")))
    assertEquals(500L, cursor.getLong(cursor.getColumnIndex("bytesDownloaded")))
    assertEquals(1000L, cursor.getLong(cursor.getColumnIndex("totalSizeOfDownload")))
    assertEquals(50, cursor.getInt(cursor.getColumnIndex("progress")))
    assertEquals("book_id", cursor.getString(cursor.getColumnIndex("bookId")))
    assertEquals("Kotlin Docs", cursor.getString(cursor.getColumnIndex("title")))
    assertEquals(
      "Kotlin documentation, by DevDocs",
      cursor.getString(cursor.getColumnIndex("description"))
    )
    assertEquals("en", cursor.getString(cursor.getColumnIndex("language")))
    assertEquals("DevDocs", cursor.getString(cursor.getColumnIndex("creator")))
    assertEquals("Kiwix", cursor.getString(cursor.getColumnIndex("publisher")))
    assertEquals("2023-01-01", cursor.getString(cursor.getColumnIndex("date")))
    assertEquals("http://kiwix.org.com", cursor.getString(cursor.getColumnIndex("url")))
    assertEquals("10", cursor.getString(cursor.getColumnIndex("articleCount")))
    assertEquals("5", cursor.getString(cursor.getColumnIndex("mediaCount")))
    assertEquals("10MB", cursor.getString(cursor.getColumnIndex("size")))
    assertEquals("Kotlin Docs", cursor.getString(cursor.getColumnIndex("name")))
    assertEquals("icon.png", cursor.getString(cursor.getColumnIndex("favIcon")))
    assertEquals("tag1", cursor.getString(cursor.getColumnIndex("tags")))
    assertEquals(0, cursor.getInt(cursor.getColumnIndex("status")))
    assertEquals(0, cursor.getInt(cursor.getColumnIndex("error")))

    cursor.close()
  }

  @Test
  fun migrate9To10() {
    var db = helper.createDatabase(testDbName, 9)

    db.execSQL(
      """
      INSERT INTO DownloadRoomEntity (
          downloadId, file, etaInMilliSeconds, bytesDownloaded, totalSizeOfDownload,
          status, error, progress, bookId, title, description, language, creator,
          publisher, date, url, articleCount, mediaCount, size, name, favIcon, tags
      ) VALUES (
          1, 'test.zim', 1000, 500, 1000, 
          0, 0, 50, 'book_id', 'Kotlin Docs', 'Kotlin documentation, by DevDocs', 'en', 'DevDocs', 
          'Kiwix', '2023-01-01', 'http://kiwix.org.com', '10', '5', '10MB', 'Kotlin Docs', 'icon.png', 'tag2'
      )
      """.trimIndent()
    )

    db.close()

    db = helper.runMigrationsAndValidate(
      testDbName,
      10,
      true,
      KiwixRoomDatabase.MIGRATION_9_10
    )

    val cursor = db.query("SELECT * FROM DownloadRoomEntity WHERE downloadId = 1")
    cursor.moveToFirst()

    val pauseReasonColumnIndex = cursor.getColumnIndex("pauseReason")
    val pauseReason = cursor.getInt(pauseReasonColumnIndex)

    assertEquals(0, pauseReason)

    cursor.close()
  }
}
