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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.reader.EncodedUrlTest
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.libzim.SuggestionSearcher
import java.io.File
import java.io.FileOutputStream

/**
 * These tests use the real `testzim.zim` resource file to construct actual
 * [ZimFileReader] instances with real [org.kiwix.libzim.Archive] objects.
 */
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
  fun testMainPageReturnsNonNull() = runBlocking {
    val reader = createZimFileReader()
    assertNotNull(reader.mainPage)
    reader.dispose()
  }

  @Test
  fun testMediaCountReturnsValidCount() = runBlocking {
    val reader = createZimFileReader()
    val mediaCount = reader.mediaCount
    assertNotNull(mediaCount)
    assertTrue(mediaCount!! >= 0)
    reader.dispose()
  }

  @Test
  fun testArticleCountReturnsValidCount() = runBlocking {
    val reader = createZimFileReader()
    val articleCount = reader.articleCount
    assertNotNull(articleCount)
    assertTrue(articleCount!! >= 0)
    reader.dispose()
  }

  @Test
  fun testTitleReturnsNonEmpty() = runBlocking {
    val reader = createZimFileReader()
    assertTrue(reader.title.isNotEmpty())
    reader.dispose()
  }

  @Test
  fun testIdReturnsNonEmpty() = runBlocking {
    val reader = createZimFileReader()
    assertTrue(reader.id.isNotEmpty())
    reader.dispose()
  }

  @Test
  fun testSearchSuggestionsReturnsNonNull() = runBlocking {
    val reader = createZimFileReader()
    val result = reader.searchSuggestions("android")
    assertNotNull(result)
    reader.dispose()
  }

  @Test
  fun testGetRandomArticleUrlReturnsNonNull() = runBlocking {
    val reader = createZimFileReader()
    val url = reader.getRandomArticleUrl()
    assertNotNull(url)
    reader.dispose()
  }

  @Test
  fun testToBookMapsFieldsCorrectly() = runBlocking {
    val reader = createZimFileReader()
    val book = reader.toBook()
    assertEquals(reader.title, book.title)
    assertEquals(reader.id, book.id)
    assertEquals(reader.creator, book.creator)
    assertEquals(reader.publisher, book.publisher)
    assertEquals(reader.date, book.date)
    assertEquals(reader.language, book.language)
    reader.dispose()
  }

  @Test
  fun testDisposeCompletesWithoutError() = runBlocking {
    val reader = createZimFileReader()
    reader.dispose()
  }

  @Test
  fun testGetSuggestedSpelledWordsReturnsEmptyWhenNotInitialized() = runBlocking {
    val reader = createZimFileReader()
    val suggestions = reader.getSuggestedSpelledWords("test", 5)
    assertTrue(suggestions.isEmpty())
    reader.dispose()
  }

  @Test
  fun testMainPageReturnsNullAfterDispose() = runBlocking {
    val reader = createZimFileReader()
    reader.dispose()
    assertNull(reader.mainPage)
  }

  @Test
  fun testMediaCountReturnsNullAfterDispose() = runBlocking {
    val reader = createZimFileReader()
    reader.dispose()
    assertNull(reader.mediaCount)
  }

  @Test
  fun testArticleCountReturnsNullAfterDispose() = runBlocking {
    val reader = createZimFileReader()
    reader.dispose()
    assertNull(reader.articleCount)
  }

  @Test
  fun testSearchSuggestionsReturnsNullAfterDispose() = runBlocking {
    val reader = createZimFileReader()
    reader.dispose()
    assertNull(reader.searchSuggestions("test"))
  }

  @Test
  fun testGetPageUrlFromReturnsNullForNonExistentTitle() = runBlocking {
    val reader = createZimFileReader()
    assertNull(reader.getPageUrlFrom("ThisTitleDefinitelyDoesNotExistInTestZim12345"))
    reader.dispose()
  }

  @Test
  fun testGetRandomArticleUrlReturnsNullAfterDispose() = runBlocking {
    val reader = createZimFileReader()
    reader.dispose()
    assertNull(reader.getRandomArticleUrl())
  }

  @Test
  fun testGetSafeMetaDataReturnsDefaultAfterDispose() = runBlocking {
    val reader = createZimFileReader()
    reader.dispose()
    // "No Title Found" is the hardcoded fallback value defined in ZimFileReader.title
    assertEquals("No Title Found", reader.title)
  }

  @Test
  fun testFactoryCreateReturnsNullForNonExistentFile() = runBlocking {
    val factory = ZimFileReader.Factory.Impl()
    val nonExistentSource = ZimReaderSource(File("/non/existent/path/fakefile.zim"))
    val result = factory.create(nonExistentSource, false)
    assertNull(result)
  }

  @Test
  fun testFactoryCreateReturnsNullForCorruptedFile() = runBlocking {
    val factory = ZimFileReader.Factory.Impl()
    val corruptedFile = File(context.getExternalFilesDirs(null)[0], "corrupted.zim")
    if (corruptedFile.exists()) corruptedFile.delete()
    corruptedFile.createNewFile()
    corruptedFile.writeBytes(ByteArray(100) { it.toByte() })
    val corruptedSource = ZimReaderSource(corruptedFile)
    val result = factory.create(corruptedSource, false)
    assertNull(result)
    corruptedFile.delete()
  }

  @Test
  fun testFactoryCreateReturnsValidReaderForValidFile() = runBlocking {
    val factory = ZimFileReader.Factory.Impl()
    val zimFile = copyZimFile("testzim_factory.zim")
    val zimReaderSource = ZimReaderSource(zimFile)
    val result = factory.create(zimReaderSource, false)
    assertNotNull(result)
    result?.dispose()
  }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
