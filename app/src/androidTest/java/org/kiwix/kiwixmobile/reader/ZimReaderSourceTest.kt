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
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
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
import org.kiwix.kiwixmobile.main.KiwixMainActivity
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
  fun fileSourceTest() = runTest {
    val source = ZimReaderSource(testZimFile)
    // Normal operations
    assertTrue(source.exists())
    assertTrue(source.canOpenInLibkiwix())
    val archive = source.createArchive()
    assertNotNull(archive)
    archive?.dispose()
    assertEquals(testZimFile.canonicalPath, source.toDatabase())

    // Test hasCode of different source with same file
    val sameSource = ZimReaderSource(testZimFile)
    assertEquals(source, sameSource)
    assertEquals(source.hashCode(), sameSource.hashCode())

    assertEquals(source, source)

    // Test hasCode of different file
    val otherFile = File(targetContext.cacheDir, "other.zim").apply {
      writeText("dummy")
      tempFiles += this
    }
    assertNotEquals(source, ZimReaderSource(otherFile))
    assertFalse(source.equals("random"))

    // Corrupt file
    val corruptFile = File(targetContext.cacheDir, "corrupt.zim").apply {
      writeText("not a zim")
      tempFiles += this
    }
    val corruptSource = ZimReaderSource(corruptFile)
    assertTrue(corruptSource.exists())
    assertTrue(corruptSource.canOpenInLibkiwix())

    // Non-existent file
    val missingFile = File(targetContext.cacheDir, "ghost.zim")
    val missingSource = ZimReaderSource(missingFile)
    assertFalse(missingSource.exists())
    assertFalse(missingSource.canOpenInLibkiwix())
    assertNull(missingSource.createArchive())

    // fromDatabaseValue (file path)
    val dbSource = ZimReaderSource.fromDatabaseValue(testZimFile.absolutePath)
    assertNotNull(dbSource)
    assertEquals(testZimFile.absolutePath, dbSource?.file?.absolutePath)
  }

  @Test
  fun fileDescriptorSourceTest() = runTest {
    val fileLength = testZimFile.length()
    val half = fileLength / 2

    val pfd1 = ParcelFileDescriptor.open(testZimFile, ParcelFileDescriptor.MODE_READ_ONLY)
    val pfd2 = ParcelFileDescriptor.open(testZimFile, ParcelFileDescriptor.MODE_READ_ONLY)

    AssetFileDescriptor(pfd1, 0, fileLength).use { fdFull ->
      AssetFileDescriptor(pfd1, 0, half).use { fd1 ->
        AssetFileDescriptor(pfd2, half, fileLength - half).use { fd2 ->

          // Single descriptor
          val singleSource = ZimReaderSource(assetFileDescriptorList = listOf(fdFull))
          assertTrue(singleSource.exists())
          assertTrue(singleSource.canOpenInLibkiwix())
          val archive = singleSource.createArchive()
          assertNotNull(archive)
          archive?.dispose()

          // Chunked descriptors
          val multiSource = ZimReaderSource(assetFileDescriptorList = listOf(fd1, fd2))
          assertTrue(multiSource.exists())
          assertTrue(multiSource.canOpenInLibkiwix())
          val multiArchive = multiSource.createArchive()
          assertNotNull(multiArchive)
          multiArchive?.dispose()

          // Equality
          val sameDescriptorSource = ZimReaderSource(assetFileDescriptorList = listOf(fdFull))
          assertEquals(singleSource, sameDescriptorSource)
          assertEquals(singleSource.hashCode(), sameDescriptorSource.hashCode())

          // Descriptor size mismatch
          assertNotEquals(singleSource, multiSource)
        }
      }
    }

    // Empty descriptor list
    val emptyDescriptorSource = ZimReaderSource(assetFileDescriptorList = emptyList())

    assertFalse(emptyDescriptorSource.exists())
    assertFalse(emptyDescriptorSource.canOpenInLibkiwix())
    assertNull(emptyDescriptorSource.createArchive())

    // Default ZimReaderSource
    val defaultSource = ZimReaderSource()
    assertEquals(0, defaultSource.hashCode())
  }

  @Test
  fun uriSourceTest() = runTest {
    val uri = Uri.parse("content://test/uri")
    val source = ZimReaderSource(uri)
    // Constructor
    assertEquals(uri, source.uri)

    // Database conversion
    assertEquals(uri.toString(), source.toDatabase())

    // Equality
    val sameSource = ZimReaderSource(uri)
    assertEquals(source, sameSource)
    assertEquals(source.hashCode(), sameSource.hashCode())

    // Different URI
    val differentUriSource = ZimReaderSource(Uri.parse("content://test/other"))
    assertNotEquals(source, differentUriSource)

    // Cross type equality
    val fileSource = ZimReaderSource(testZimFile)
    assertNotEquals(source, fileSource)

    // Invalid URI
    val invalidUriSource = ZimReaderSource(Uri.parse("content://invalid/nonexistent"))
    assertFalse(invalidUriSource.exists())
    assertFalse(invalidUriSource.canOpenInLibkiwix())
    assertNull(invalidUriSource.createArchive())

    // fromDatabaseValue with URI
    val dbUriSource = ZimReaderSource.fromDatabaseValue(uri.toString())
    assertNotNull(dbUriSource)
    assertEquals(uri, dbUriSource!!.uri)

    // fromDatabaseValue null
    assertNull(ZimReaderSource.fromDatabaseValue(null))
  }

  @Test
  fun getUriTest() {
    val activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java)

    activityScenario.onActivity { activity ->
      val file = File(activity.cacheDir, "test.zim").apply {
        writeText("dummy")
      }
      val fileSource = ZimReaderSource(file)
      val resultUri = fileSource.getUri(activity)
      val expectedUri = FileProvider.getUriForFile(
        activity,
        "${activity.packageName}.fileprovider",
        file
      )
      assertEquals(expectedUri, resultUri)

      val testUri = Uri.parse("content://test/someuri")
      val uriSource = ZimReaderSource(testUri)
      val returnedUri = uriSource.getUri(activity)
      assertEquals(testUri, returnedUri)
      file.delete()
    }
  }
}
