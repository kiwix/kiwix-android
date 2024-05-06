/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.fail
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.NightModeConfig
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.LocalLibraryFragmentDirections
import org.kiwix.kiwixmobile.page.history.navigationHistory
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.libzim.Archive
import org.kiwix.libzim.SuggestionSearcher
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ZimFileReaderWithSplittedZimFileTest : BaseActivityTest() {

  @Rule
  @JvmField
  var retryRule = RetryRule()

  private lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_PLAY_STORE_RESTRICTION, false)
    }
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
    }
  }

  @Test
  fun testZimFileReaderWithSplittedZimFile() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(R.id.libraryFragment)
    }
    createAndGetSplitedZimFile()?.let {
      UiThreadStatement.runOnUiThread {
        kiwixMainActivity.navigate(
          LocalLibraryFragmentDirections.actionNavigationLibraryToNavigationReader()
            .apply { zimFileUri = it.toUri().toString() }
        )
      }

      navigationHistory {
        checkZimFileLoadedSuccessful(R.id.readerFragment)
        assertZimFileLoaded() // check if the zim file successfully loaded
        clickOnAndroidArticle()
      }
    } ?: kotlin.run {
      // error in creating the zim file chunk
      fail("Couldn't create the zim file chunk")
    }
  }

  @Test
  fun testWithExtraZeroSizeFile() {
    createAndGetSplitedZimFile(true)?.let { zimFile ->
      // test the articleCount and mediaCount of this zim file.
      val archive = Archive(zimFile.canonicalPath)
      val zimFileReader = ZimFileReader(
        zimFile,
        null,
        null,
        archive,
        NightModeConfig(SharedPreferenceUtil(context), context),
        SuggestionSearcher(archive)
      )
      Assert.assertEquals(zimFileReader.mediaCount, 16)
      Assert.assertEquals(zimFileReader.articleCount, 4)
    } ?: kotlin.run {
      // error in creating the zim file chunk
      fail("Couldn't create the zim file chunk")
    }
  }

  private fun createAndGetSplitedZimFile(shouldCreateExtraZeroSizeFile: Boolean = false): File? {
    val loadFileStream =
      EncodedUrlTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    val storageDir = ContextCompat.getExternalFilesDirs(context, null)[0]

    // Delete existing parts if they exist
    (1..3)
      .asSequence()
      .map { File(storageDir, "testzim.zima${('a' + it - 1).toChar()}") }
      .filter(File::exists)
      .forEach(File::delete)

    // Calculate the size of each part
    val totalFileSize = loadFileStream.available().toLong()
    val partSize = totalFileSize / 3 // convert into 3 parts
    var partNumber = 1

    // Read the input stream in chunks and write to smaller files
    loadFileStream.use { inputStream ->
      var bytesRead: Long = 0
      var currentPartSize: Long = 0
      val buffer = ByteArray(1024)
      var length: Int
      var outputStream: OutputStream? = null

      while (inputStream.read(buffer).also { length = it } > 0) {
        if (currentPartSize >= partSize || bytesRead == 0L) {
          outputStream?.close() // Close the previous outputStream if any open.
          val partFile = File(storageDir, "testzim.zima${('a' + partNumber - 1).toChar()}")
          outputStream = FileOutputStream(partFile)
          partNumber++
          currentPartSize = 0
        }

        outputStream?.write(buffer, 0, length)

        bytesRead += length
        currentPartSize += length
      }
      outputStream?.close()
    }
    if (shouldCreateExtraZeroSizeFile) {
      File(storageDir, "testzim.zimad").apply {
        if (exists()) delete() // delete if already exist.
        createNewFile() // create new zero size file.
      }
    }
    val splittedZimFile = File(storageDir, "testzim.zimaa")
    return if (splittedZimFile.exists()) splittedZimFile else null
  }
}
