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

package org.kiwix.kiwixmobile.reader

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import java.io.File

@RunWith(AndroidJUnit4::class)
class ZimReaderSourceTest {
  private lateinit var testZimFile: File

  private val zimFileName = "bash_docs.zim"

  private val instrumentationContext = InstrumentationRegistry.getInstrumentation().context

  private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

  private val tempFiles = mutableListOf<File>()

  @Before
  fun setup() {
    testZimFile = File(targetContext.cacheDir, zimFileName)
    instrumentationContext.assets.open(zimFileName).use { input ->
      testZimFile.outputStream().use { output ->
        input.copyTo(output)
      }
    }
  }

  @After
  fun tearDown() {
    testZimFile.delete()
    tempFiles.forEach { it.delete() }
    tempFiles.clear()
  }

  @Test
  fun fileConstructorWithExistingFileIsValid() = runTest {
    val source = ZimReaderSource(testZimFile)
    assertTrue(source.exists())
  }

  @Test
  fun existsWithNonExistentFileIsFalse() = runTest {
    val missing = File(targetContext.cacheDir, "no_such_file.zim")
    val source = ZimReaderSource(missing)
    assertFalse(source.exists())
  }

  @Test
  fun canOpenInLibkiwixWithValidZimIsTrue() = runTest {
    val source = ZimReaderSource(testZimFile)
    assertTrue(
      "A real ZIM must be openable by libkiwix",
      source.canOpenInLibkiwix()
    )
  }

  @Test
  fun canOpenInLibkiwixWithCorruptFileIsTrue() = runTest {
    val corrupt = File(targetContext.cacheDir, "corrupt.zim").also {
      it.writeText("this is not a ZIM file")
      tempFiles += it
    }
    val source = ZimReaderSource(corrupt)
    assertTrue(source.canOpenInLibkiwix())
  }

  @Test
  fun canOpenInLibkiwixWithNonExistentFileIsFalse() = runTest {
    val missing = File(targetContext.cacheDir, "ghost.zim")
    val source = ZimReaderSource(missing)
    assertFalse(source.canOpenInLibkiwix())
  }

  @Test
  fun createArchiveWithValidZimIsNonNull() = runTest {
    val source = ZimReaderSource(testZimFile)
    val archive = source.createArchive()
    assertNotNull("Archive must be created from a valid ZIM", archive)
  }

  @Test
  fun uriConstructorStoresUri() {
    val uri = Uri.parse("content://dummy/path")
    val source = ZimReaderSource(uri)
    assertEquals(uri, source.uri)
  }

  @Test
  fun fromDatabaseValueWithContentStringCreatesUriSource() {
    val contentString = "content://some/document/path"
    val source = ZimReaderSource.fromDatabaseValue(contentString)

    assertNotNull("Source should be created", source)
    assertEquals(Uri.parse(contentString), source!!.uri)
    assertNull("File should be null for content URIs", source.file)
  }

  @Test
  fun fromDatabaseValueWithFileStringCreatesFileSource() {
    val path = testZimFile.absolutePath
    val source = ZimReaderSource.fromDatabaseValue(path)

    assertNotNull("Source should be created", source)
    assertEquals(path, source!!.file?.absolutePath)
    assertNull("Uri should be null for file paths", source.uri)
  }

  @Test
  fun toDatabaseWithFileSourceUsesCanonicalPath() {
    val source = ZimReaderSource(testZimFile)
    assertEquals(testZimFile.canonicalPath, source.toDatabase())
  }

  @Test
  fun toDatabaseWithUriSourceUsesUriString() {
    val uriString = "content://test/uri"
    val source = ZimReaderSource(Uri.parse(uriString))
    assertEquals(uriString, source.toDatabase())
  }

  @Test
  fun equalsWithSameFilePathAreEqual() {
    val a = ZimReaderSource(testZimFile)
    val b = ZimReaderSource(testZimFile)
    assertEquals(a, b)
  }

  @Test
  fun equalsWithDifferentFilesAreNotEqual() {
    val other = File(targetContext.cacheDir, "other.zim").also {
      it.createNewFile()
      tempFiles += it
    }
    assertNotEquals(ZimReaderSource(testZimFile), ZimReaderSource(other))
  }

  @Test
  fun hashCodeWithEqualSourcesMatch() {
    val a = ZimReaderSource(testZimFile)
    val b = ZimReaderSource(testZimFile)
    assertEquals("Equal sources must have equal hashCodes", a.hashCode(), b.hashCode())
  }
}
