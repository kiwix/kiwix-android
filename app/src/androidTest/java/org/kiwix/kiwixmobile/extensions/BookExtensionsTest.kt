/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.extensions

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.extensions.getFavicon
import org.kiwix.libkiwix.Book
import org.kiwix.libzim.Archive
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
@SmallTest
class BookExtensionsTest {
  @Test
  fun testBookFavicon() {
    // Test null Book
    assertNull(
      "getFavicon on a null Book should return null",
      null.getFavicon()
    )

    // Test favicon is empty when getFavicon throw exception.
    val emptyBook = Book()
    val exceptionResult = emptyBook.getFavicon()
    assertEquals(
      "getFavicon should return empty string when illustration extraction fails",
      "",
      exceptionResult
    )

    // Load a real ZIM file and create a Book backed by an Archive
    val zimFile = getZimFile("testzim.zim")
    val archive = Archive(zimFile.path)
    val book = Book().apply { update(archive) }
    val result = book.getFavicon()
    // A real ZIM file should produce a non-null, non-empty favicon (Base64 or URL)
    assertNotNull("getFavicon should not return null for a real ZIM file", result)
    assertTrue(
      "getFavicon should return a non-empty string for a real ZIM file",
      result!!.isNotEmpty()
    )
    zimFile.delete()
  }

  private fun getZimFile(zimFileName: String): File {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val loadFileStream =
      BookExtensionsTest::class.java.classLoader!!.getResourceAsStream(zimFileName)
    val zimFile = File(context.getExternalFilesDir(null), zimFileName)
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    loadFileStream.use { inputStream ->
      FileOutputStream(zimFile).use { outputStream ->
        val buffer = ByteArray(inputStream.available())
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
          outputStream.write(buffer, 0, length)
        }
      }
    }
    return zimFile
  }
}
