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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.libzim.Archive
import org.kiwix.libzim.SuggestionSearcher
import java.io.File

@RunWith(AndroidJUnit4::class)
class ZimReaderContainerTest {
  private lateinit var container: ZimReaderContainer
  private lateinit var testZimFile: File

  private val zimFileName = "testzim.zim"
  private val targetContext =
    InstrumentationRegistry.getInstrumentation().targetContext
  private val tempFiles = mutableListOf<File>()

  private fun getMainEntryPath(): String =
    container.mainPage ?: "A/index.html"

  @Before
  fun setup() {
    testZimFile = File(targetContext.cacheDir, zimFileName)
    this::class.java.classLoader!!.getResourceAsStream(zimFileName)!!.use { input ->
      testZimFile.outputStream().use { output ->
        input.copyTo(output)
      }
    }

    val realFactory = object : ZimFileReader.Factory {
      override suspend fun create(
        zimReaderSource: ZimReaderSource,
        showSearchSuggestionsSpellChecked: Boolean
      ): ZimFileReader? = try {
        val archive: Archive =
          zimReaderSource.createArchive() ?: return null
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
    if (::container.isInitialized) {
      container.zimFileReader?.dispose()
    }
    if (::testZimFile.isInitialized) {
      testZimFile.delete()
    }
    tempFiles.forEach { it.delete() }
    tempFiles.clear()
  }

  @Test
  fun setZimReaderSourceScenarios() = runTest {
    val source = ZimReaderSource(testZimFile)

    // Valid source
    container.setZimReaderSource(source)
    assertNotNull(container.zimFileReader)
    assertEquals(source, container.zimReaderSource)

    // Same source should not recreate
    val originalReader = container.zimFileReader
    container.setZimReaderSource(source)
    assertSame(originalReader, container.zimFileReader)

    // Different source should recreate
    val secondFile = File(targetContext.cacheDir, "second.zim").also {
      testZimFile.copyTo(it, overwrite = true)
      tempFiles += it
    }
    container.setZimReaderSource(ZimReaderSource(secondFile))
    assertNotSame(originalReader, container.zimFileReader)

    // Invalid file
    val invalidFile = File(targetContext.cacheDir, "fake.zim").also {
      it.writeText("corrupt")
      tempFiles += it
    }
    container.setZimReaderSource(ZimReaderSource(invalidFile))
    assertNull(container.zimFileReader)

    // Null source clears reader
    container.setZimReaderSource(null)
    assertNull(container.zimFileReader)
    assertNull(container.zimReaderSource)
  }

  @Test
  fun loadRequestScenarios() = runTest {
    container.setZimReaderSource(ZimReaderSource(testZimFile))
    val mainEntry = getMainEntryPath()

    // Normal load
    val normal = container.load(mainEntry, emptyMap())
    assertEquals(200, normal.statusCode)
    assertEquals("OK", normal.reasonPhrase)
    assertEquals("bytes", normal.responseHeaders["Accept-Ranges"])
    assertNotNull(normal.data)

    // Range request
    val partial = container.load(mainEntry, mapOf("Range" to "bytes=0-10"))
    assertEquals(206, partial.statusCode)
    assertEquals("Partial Content", partial.reasonPhrase)
    assertNotNull(partial.responseHeaders["Content-Range"])

    // Range without dash
    val singleByte = container.load(mainEntry, mapOf("Range" to "bytes=0"))
    assertEquals(206, singleByte.statusCode)
    assertEquals("close", singleByte.responseHeaders["Connection"])

    // Open ended range
    val openRange = container.load(mainEntry, mapOf("Range" to "bytes=0-"))
    assertEquals(206, openRange.statusCode)
    assertNull(openRange.responseHeaders["Connection"])

    // Non-existent entry
    val missing = container.load("nonexistent/path.html", emptyMap())
    assertNull(missing.mimeType)
  }

  @Test
  fun readerApiAndMetadataScenarios() = runTest {
    // Behavior without reader
    assertNull(container.zimFileReader)
    assertNull(container.zimReaderSource)
    assertNull(container.zimFileTitle)
    assertNull(container.mainPage)
    assertNull(container.id)
    assertNull(container.creator)
    assertNull(container.publisher)
    assertNull(container.name)
    assertNull(container.date)
    assertNull(container.description)
    assertNull(container.favicon)
    assertNull(container.language)
    assertEquals(0L, container.fileSize)
    assertEquals("", container.getRedirect("anything"))
    assertFalse(container.isRedirect("anything"))
    assertNull(container.getPageUrlFromTitle("anything"))
    assertNull(container.getRandomArticleUrl())

    // Load reader
    container.setZimReaderSource(ZimReaderSource(testZimFile))

    val mainEntry = getMainEntryPath()

    // Redirect behavior
    assertFalse(container.isRedirect(CONTENT_PREFIX + mainEntry))
    assertTrue(container.getRedirect(CONTENT_PREFIX + mainEntry).contains(mainEntry))

    // Title lookup
    val title = container.zimFileReader!!
      .jniKiwixReader.mainEntry.getItem(true).title

    val url = container.getPageUrlFromTitle(title)
    assertNotNull(url)
    assertTrue(url!!.isNotBlank())

    val mainEntryTitle = container.zimFileReader!!
      .jniKiwixReader.mainEntry.getItem(true).title
    val result = container.getPageUrlFromTitle(mainEntryTitle)
    assertNotNull(result)
    assertTrue(result!!.isNotBlank())

    assertNull(container.getPageUrlFromTitle("Nonexistent_Title_12345"))

    // Random article
    assertNotNull(container.getRandomArticleUrl())

    // Metadata
    container.setZimReaderSource(ZimReaderSource(testZimFile))
    assertEquals("Test_Zim", container.zimFileTitle)
    assertEquals("Wikipedia Contributors", container.creator)
    assertEquals("Zimbalaka 1.0", container.publisher)
    assertEquals("60094d1e-1c9a-a60b-2011-4fb02f8db6c3", container.name)
    assertEquals("2017-04-21", container.date)
    assertEquals("Wikipedia article on Test Zim", container.description)
    assertEquals("en", container.language)
    assertEquals("A/index.html", container.mainPage)
    assertEquals(348L, container.fileSize)
    assertNotNull(container.id)
    assertNotNull(container.favicon)
  }
}
