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

import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import java.io.File

@RunWith(AndroidJUnit4::class)
class ZimReaderSourceTest {
  private lateinit var testZimFile: File

  private val zimFileName = "testzim.zim"
  private val targetContext =
    InstrumentationRegistry.getInstrumentation().targetContext
  private val tempFiles = mutableListOf<File>()

  @Before
  fun setup() {
    testZimFile = File(targetContext.cacheDir, zimFileName)
    this::class.java.classLoader!!.getResourceAsStream(zimFileName)!!.use { input ->
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
    assertTrue(source.canOpenInLibkiwix())
  }

  @Test
  fun singleFileDescriptorConstructorCreatesValidSource() = runTest {
    val parcelFd = ParcelFileDescriptor.open(testZimFile, ParcelFileDescriptor.MODE_READ_ONLY)
    AssetFileDescriptor(parcelFd, 0, testZimFile.length()).use { assetFd ->
      val source = ZimReaderSource(
        file = null,
        uri = null,
        assetFileDescriptorList = listOf(assetFd)
      )
      assertTrue(source.exists())
      assertTrue(source.canOpenInLibkiwix())
      val archive = source.createArchive()
      assertNotNull(archive)
      archive!!.dispose()
    }
  }

  @Test
  fun multipleFileDescriptorsSourceIsValid() = runTest {
    val parcelFd1 = ParcelFileDescriptor.open(testZimFile, ParcelFileDescriptor.MODE_READ_ONLY)
    val parcelFd2 = ParcelFileDescriptor.open(testZimFile, ParcelFileDescriptor.MODE_READ_ONLY)
    AssetFileDescriptor(parcelFd1, 0, testZimFile.length()).use { assetFd1 ->
      AssetFileDescriptor(parcelFd2, 0, testZimFile.length()).use { assetFd2 ->
        val source = ZimReaderSource(
          file = null,
          uri = null,
          assetFileDescriptorList = listOf(assetFd1, assetFd2)
        )
        assertTrue(source.exists())
        assertTrue(source.canOpenInLibkiwix())
      }
    }
  }

  @Test
  fun existsWithNonExistentFileIsFalse() = runTest {
    assertFalse(ZimReaderSource(File(targetContext.cacheDir, "no_such_file.zim")).exists())
  }

  @Test
  fun existsWithInvalidUriIsFalse() = runTest {
    assertFalse(ZimReaderSource(Uri.parse("content://invalid/nonexistent")).exists())
  }

  @Test
  fun canOpenInLibkiwixWithValidZimIsTrue() = runTest {
    assertTrue(ZimReaderSource(testZimFile).canOpenInLibkiwix())
  }

  @Test
  fun canOpenInLibkiwixReturnsTrueForReadableCorruptFile() = runTest {
    val corrupt = File(targetContext.cacheDir, "corrupt.zim").also {
      it.writeText("this is not a ZIM file")
      tempFiles += it
    }
    assertTrue(ZimReaderSource(corrupt).canOpenInLibkiwix())
  }

  @Test
  fun canOpenInLibkiwixWithNonExistentFileIsFalse() = runTest {
    assertFalse(ZimReaderSource(File(targetContext.cacheDir, "ghost.zim")).canOpenInLibkiwix())
  }

  @Test
  fun canOpenInLibkiwixWithInvalidUriIsFalse() = runTest {
    assertFalse(ZimReaderSource(Uri.parse("content://invalid/nonexistent")).canOpenInLibkiwix())
  }

  @Test
  fun createArchiveWithValidZimIsNonNull() = runTest {
    val archive = ZimReaderSource(testZimFile).createArchive()
    assertNotNull(archive)
    archive!!.dispose()
  }

  @Test
  fun createArchiveWithInvalidUriReturnsNull() = runTest {
    assertNull(ZimReaderSource(Uri.parse("content://invalid/nonexistent")).createArchive())
  }

  @Test
  fun uriConstructorStoresUri() {
    val uri = Uri.parse("content://dummy/path")
    assertEquals(uri, ZimReaderSource(uri).uri)
  }

  @Test
  fun fromDatabaseValueWithContentStringCreatesUriSource() {
    val contentString = "content://some/document/path"
    val source = ZimReaderSource.fromDatabaseValue(contentString)
    assertNotNull(source)
    assertEquals(Uri.parse(contentString), source!!.uri)
    assertNull(source.file)
  }

  @Test
  fun fromDatabaseValueWithFileStringCreatesFileSource() {
    val path = testZimFile.absolutePath
    val source = ZimReaderSource.fromDatabaseValue(path)
    assertNotNull(source)
    assertEquals(path, source!!.file?.absolutePath)
    assertNull(source.uri)
  }

  @Test
  fun toDatabaseWithFileSourceUsesCanonicalPath() {
    assertEquals(testZimFile.canonicalPath, ZimReaderSource(testZimFile).toDatabase())
  }

  @Test
  fun toDatabaseWithUriSourceUsesUriString() {
    val uriString = "content://test/uri"
    assertEquals(uriString, ZimReaderSource(Uri.parse(uriString)).toDatabase())
  }

  @Test
  fun equalsWithSameFilePathAreEqual() {
    assertEquals(ZimReaderSource(testZimFile), ZimReaderSource(testZimFile))
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
    assertEquals(ZimReaderSource(testZimFile).hashCode(), ZimReaderSource(testZimFile).hashCode())
  }
}
