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

package org.kiwix.kiwixmobile.core.reader

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.reader.EncodedUrlTest
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.libzim.SuggestionSearcher
import java.io.File
import java.io.FileOutputStream

class ZimFileReaderInstrumentedTest : BaseActivityTest() {
  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    KiwixDataStore(context).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
        setPrefLanguage("en")
        setLastDonationPopupShownInMilliSeconds(System.currentTimeMillis())
        setIsScanFileSystemDialogShown(true)
        setIsFirstRun(false)
        setPrefIsTest(true)
      }
    }
    activityScenario =
      ActivityScenario.launch(KiwixMainActivity::class.java).apply {
        moveToState(Lifecycle.State.RESUMED)
        onActivity {
          AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
      }
  }

  private suspend fun copyZimFile(name: String): File {
    val loadFileStream =
      EncodedUrlTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    val zimFile = File(context.getExternalFilesDirs(null)[0], name)
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    loadFileStream.use { inputStream ->
      FileOutputStream(zimFile).use { outputStream ->
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
          outputStream.write(buffer, 0, length)
        }
      }
    }
    return zimFile
  }

  private suspend fun createZimFileReader(): ZimFileReader {
    val zimFile = copyZimFile("testzim.zim")
    val zimReaderSource = ZimReaderSource(zimFile)
    val archive = zimReaderSource.createArchive()!!
    return ZimFileReader(zimReaderSource, archive, SuggestionSearcher(archive))
  }

  @Test
  fun testZimFileReader() {
    runBlocking {
      val reader = createZimFileReader()

      // ── Verify all 14 properties with actual expected values ──
      assertEquals("Test Zim", reader.title)
      assertNotNull(reader.mainPage)
      assertTrue(reader.id.isNotEmpty())
      assertEquals(16, reader.mediaCount)
      assertEquals(4, reader.articleCount)
      assertEquals("Test_Zim", reader.creator)
      assertEquals("Kiwix", reader.publisher)
      assertTrue(reader.name.isNotEmpty())
      assertEquals("2024-02-01", reader.date)
      assertEquals("This is a test zim file", reader.description)
      assertEquals("eng", reader.language)
      assertEquals("", reader.tags)
      assertTrue(reader.fileSize > 0)
      assertNotNull(reader.favicon)

      // ── Verify searchSuggestions and related methods ──
      assertNotNull(reader.searchSuggestions("android"))
      assertNotNull(reader.getRandomArticleUrl())
      assertTrue(reader.getSuggestedSpelledWords("test", 5).isEmpty())

      // ── Verify getPageUrlFrom for correct and incorrect title ──
      assertNull(reader.getPageUrlFrom("ThisTitleDefinitelyDoesNotExistInTestZim12345"))
      reader.mainPage?.let { mainPagePath ->
        assertNotNull(reader.getPageUrlFrom(mainPagePath))
      }

      // ── Verify getMimeTypeFromUrl ──
      assertEquals("text/html", reader.getMimeTypeFromUrl("${CONTENT_PREFIX}A/index.html"))
      assertEquals("text/css", reader.getMimeTypeFromUrl("${CONTENT_PREFIX}-/assets/style1.css"))
      assertNull(reader.getMimeTypeFromUrl("${CONTENT_PREFIX}A/non_existent_page.html"))

      // ── Verify isRedirect ──
      assertFalse(reader.isRedirect("http://example.com/test"))
      assertFalse(reader.isRedirect(""))

      // ── Verify getRedirect ──
      val redirect = reader.getRedirect("${CONTENT_PREFIX}A/index.html")
      assertNotNull(redirect)
      assertTrue(redirect.isNotEmpty())

      // ── Verify load returns content for valid URL ──
      val inputStream = reader.load("${CONTENT_PREFIX}A/index.html")
      assertNotNull(inputStream)
      val content = inputStream?.bufferedReader()?.readText()
      assertNotNull(content)
      assertTrue(content!!.isNotEmpty())

      // ── Verify load returns empty content for invalid URL ──
      val invalidInputStream = reader.load("${CONTENT_PREFIX}A/non_existent_page_12345.html")
      assertNotNull(invalidInputStream)
      val invalidContent = invalidInputStream?.bufferedReader()?.readText()
      assertTrue(invalidContent.isNullOrEmpty())

      // ── Verify getItem ──
      val item = reader.getItem("${CONTENT_PREFIX}A/index.html")
      assertNotNull(item)
      val nullItem = reader.getItem("${CONTENT_PREFIX}A/non_existent_12345.html")
      assertNull(nullItem)

      // ── Verify prepareSpellingsDB and getSuggestedSpelledWords ──
      reader.prepareSpellingsDB(reader.jniKiwixReader)
      val suggestions = reader.getSuggestedSpelledWords("android", 5)
      assertNotNull(suggestions)

      // ── Verify toBook maps all fields correctly ──
      val book = reader.toBook()
      assertEquals(reader.title, book.title)
      assertEquals(reader.id, book.id)
      assertEquals("${reader.fileSize}", book.size)
      assertEquals(reader.favicon.toString(), book.favicon)
      assertEquals(reader.creator, book.creator)
      assertEquals(reader.publisher, book.publisher)
      assertEquals(reader.date, book.date)
      assertEquals(reader.description, book.description)
      assertEquals(reader.language, book.language)
      assertEquals(reader.articleCount.toString(), book.articleCount)
      assertEquals(reader.mediaCount.toString(), book.mediaCount)
      assertEquals(reader.name, book.bookName)
      assertEquals(reader.tags, book.tags)

      // ── Verify all 14 properties return defaults after dispose ──
      reader.dispose()

      assertNull(reader.mainPage)
      assertEquals("No Title Found", reader.title)
      assertNull(reader.mediaCount)
      assertNull(reader.articleCount)
      assertEquals("", reader.creator)
      assertEquals("", reader.publisher)
      assertEquals("", reader.date)
      assertEquals("", reader.description)
      assertEquals("", reader.language)
      assertEquals("", reader.tags)
      assertNull(reader.favicon)
      assertNull(reader.searchSuggestions("test"))
      assertNull(reader.getRandomArticleUrl())
      assertNull(reader.getPageUrlFrom("index"))

      // ── Verify Factory.create with non-existent file ──
      val factory = ZimFileReader.Factory.Impl()
      val nonExistentSource = ZimReaderSource(File("/non/existent/path/fakefile.zim"))
      assertNull(factory.create(nonExistentSource, false))

      // ── Verify Factory.create with corrupted file ──
      val corruptedFile = File(context.getExternalFilesDirs(null)[0], "corrupted.zim")
      if (corruptedFile.exists()) corruptedFile.delete()
      corruptedFile.createNewFile()
      corruptedFile.writeBytes(ByteArray(100) { it.toByte() })
      val corruptedSource = ZimReaderSource(corruptedFile)
      assertNull(factory.create(corruptedSource, false))
      corruptedFile.delete()

      // ── Verify Factory.create with valid file ──
      val validZimFile = copyZimFile("testzim_factory.zim")
      val validSource = ZimReaderSource(validZimFile)
      val validResult = factory.create(validSource, false)
      assertNotNull(validResult)
      validResult?.dispose()
    }
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
