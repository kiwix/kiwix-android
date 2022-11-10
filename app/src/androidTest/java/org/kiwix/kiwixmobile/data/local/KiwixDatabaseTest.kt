/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.data.local

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.yahoo.squidb.sql.Query
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.data.local.KiwixDatabase
import org.kiwix.kiwixmobile.core.data.local.entity.Bookmark
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer

@RunWith(AndroidJUnit4::class)
@SmallTest
class KiwixDatabaseTest {
  private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

  @Test @Throws(IOException::class) // Standard charset throws exception on < API19
  fun testMigrateDatabase() {
    val kiwixDatabase = KiwixDatabase(
      context,
      null,
      null,
      null,
      null
    )
    kiwixDatabase.recreate()
    val testId = "8ce5775a-10a9-bbf3-178a-9df69f23263c"
    val testBookmarks = arrayOf("Test1", "Test2", "Test3")
    val fileName = context.filesDir.absolutePath + File.separator + testId + ".txt"
    val f = File(fileName)
    if (!f.createNewFile()) {
      throw IOException("Unable to create file for testing migration")
    }
    val writer: Writer = BufferedWriter(
      OutputStreamWriter(
        FileOutputStream(fileName),
        "UTF-8"
      )
    )
    for (bookmark in testBookmarks) {
      writer.write(
        """
  $bookmark
  
        """.trimIndent()
      )
    }
    writer.close()
    kiwixDatabase.migrateBookmarksVersion6()
    val bookmarkTitles = ArrayList<String?>()
    try {
      val bookmarkCursor = kiwixDatabase.query(
        Bookmark::class.java,
        Query.selectDistinct(Bookmark.BOOKMARK_TITLE)
          .where(
            Bookmark.ZIM_ID.eq(testId)
              .or(Bookmark.ZIM_NAME.eq(""))
          )
          .orderBy(Bookmark.BOOKMARK_TITLE.asc())
      )
      while (bookmarkCursor.moveToNext()) {
        bookmarkTitles.add(bookmarkCursor.get(Bookmark.BOOKMARK_TITLE))
      }
    } catch (exception: Exception) {
      exception.printStackTrace()
    }
    Assert.assertArrayEquals(testBookmarks, bookmarkTitles.toTypedArray())

    // TODO Add new migration test for version 16
  }
}
