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

package org.kiwix.kiwixmobile.reader

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.libzim.Archive
import org.kiwix.libzim.SuggestionSearcher
import java.io.File

@RunWith(AndroidJUnit4::class)
class ZimReaderContainerTest {
  private lateinit var container: ZimReaderContainer
  private lateinit var testZimFile: File

  private val zimFileName = "bash_docs.zim"
  private val mainEntryPath = "mainPage"

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

    val realFactory = object : ZimFileReader.Factory {
      override suspend fun create(
        zimReaderSource: ZimReaderSource,
        showSearchSuggestionsSpellChecked: Boolean
      ): ZimFileReader? = try {
        val archive: Archive = zimReaderSource.createArchive() ?: return null
        val searcher = SuggestionSearcher(archive)
        ZimFileReader(
          zimReaderSource = zimReaderSource,
          jniKiwixReader = archive,
          searcher = searcher
        )
      } catch (_: Exception) {
        null
      }
    }

    container = ZimReaderContainer(realFactory)
  }

  @After
  fun tearDown() {
    testZimFile.delete()
    tempFiles.forEach { it.delete() }
    tempFiles.clear()
  }

  @Test
  fun setZimReaderSourceWithValidFileCreatesReader() = runTest {
    val source = ZimReaderSource(testZimFile)
    container.setZimReaderSource(source)

    assertNotNull(container.zimFileReader)
    assertEquals(source, container.zimReaderSource)
  }

  @Test
  fun setZimReaderSourceWithSameSourceDoesNotRecreateReader() = runTest {
    val source = ZimReaderSource(testZimFile)
    container.setZimReaderSource(source)
    val originalReader = container.zimFileReader

    container.setZimReaderSource(source)
    assertSame(originalReader, container.zimFileReader)
  }

  @Test
  fun setZimReaderSourceWithInvalidFileReaderIsNull() = runTest {
    val invalidFile = File(targetContext.cacheDir, "fake.zim").also {
      it.writeText("corrupt data")
      tempFiles += it
    }

    val source = ZimReaderSource(invalidFile)
    container.setZimReaderSource(source)

    assertNull(container.zimFileReader)
  }

  @Test
  fun setZimReaderSourceWithNullReaderIsNull() = runTest {
    container.setZimReaderSource(ZimReaderSource(testZimFile))
    assertNotNull(container.zimFileReader)

    container.setZimReaderSource(null)
    assertNull(container.zimFileReader)
    assertNull(container.zimReaderSource)
  }

  @Test
  fun loadWithSinglePartRangeAddsConnectionClose() = runTest {
    container.setZimReaderSource(ZimReaderSource(testZimFile))

    val response = container.load(mainEntryPath, mapOf("Range" to "bytes=0"))

    assertEquals(206, response.statusCode)
    assertEquals("close", response.responseHeaders["Connection"])
  }

  @Test
  fun loadWithOpenEndedRangeHasNoConnectionClose() = runTest {
    container.setZimReaderSource(ZimReaderSource(testZimFile))

    val response = container.load(mainEntryPath, mapOf("Range" to "bytes=0-"))

    assertEquals(206, response.statusCode)
    assertNull(response.responseHeaders["Connection"])
  }

  @Test
  fun loadWithFullRangeHasNoConnectionClose() = runTest {
    container.setZimReaderSource(ZimReaderSource(testZimFile))

    val response = container.load(mainEntryPath, mapOf("Range" to "bytes=0-10"))

    assertEquals(206, response.statusCode)
    assertNull(response.responseHeaders["Connection"])
  }

  @Test
  fun isRedirectWithNonRedirectUrlIsFalse() = runTest {
    container.setZimReaderSource(ZimReaderSource(testZimFile))

    assertFalse(container.isRedirect(mainEntryPath))
  }

  @Test
  fun getRedirectWithNonRedirectUrlContainsOriginalPath() = runTest {
    container.setZimReaderSource(ZimReaderSource(testZimFile))

    assertTrue(container.getRedirect(mainEntryPath).contains(mainEntryPath))
  }

  @Test
  fun getPageUrlFromTitleWithUnknownTitleIsNull() = runTest {
    container.setZimReaderSource(ZimReaderSource(testZimFile))

    assertNull(container.getPageUrlFromTitle("This_Title_Does_Not_Exist_12345"))
  }

  @Test
  fun getRandomArticleUrlIsNonNull() = runTest {
    container.setZimReaderSource(ZimReaderSource(testZimFile))
    assertNotNull(container.getRandomArticleUrl())
  }

  @Test
  fun propertyDelegationWithReaderUsesRealMetadata() = runTest {
    container.setZimReaderSource(ZimReaderSource(testZimFile))

    assertTrue(container.fileSize > 0)
    assertNotNull(container.zimFileTitle)
    assertNotNull(container.creator)
    assertNotNull(container.language)
  }

  @Test
  fun propertyDelegationWithoutReaderUsesDefaults() {
    assertNull(container.zimFileReader)
    assertEquals(0L, container.fileSize)
    assertNull(container.zimFileTitle)
  }
}
