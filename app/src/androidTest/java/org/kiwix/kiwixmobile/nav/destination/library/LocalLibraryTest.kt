/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library

import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaSwipeRefreshInteractions.refresh
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.search.SearchFragmentTest
import org.kiwix.kiwixmobile.testutils.RetryRule
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class LocalLibraryTest : BaseActivityTest() {

  @Rule
  @JvmField
  var retryRule = RetryRule()

  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()
    PreferenceManager.getDefaultSharedPreferences(
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    ).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      // set PREF_IS_TEST false for testing the real scenario
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, false)
      // set PREF_MANAGE_EXTERNAL_FILES false for hiding
      // manage external storage permission dialog on android 11 and above
      putBoolean(SharedPreferenceUtil.PREF_MANAGE_EXTERNAL_FILES, false)
    }
  }

  @Test
  fun testLocalLibrary() {
    ActivityScenario.launch(KiwixMainActivity::class.java).onActivity {
      it.navigate(R.id.libraryFragment)
    }
    library {
      deleteZimIfExists()
      assertNoFilesTextDisplayed()
    }
    // load a zim file to test, After downloading zim file library list is visible or not
    val loadFileStream =
      SearchFragmentTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    val zimFile =
      File(
        ContextCompat.getExternalFilesDirs(context, null)[0],
        "testzim.zim"
      )
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    loadFileStream.use { inputStream ->
      val outputStream: OutputStream = FileOutputStream(zimFile)
      outputStream.use { it ->
        val buffer = ByteArray(inputStream.available())
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
          it.write(buffer, 0, length)
        }
      }
    }
    refresh(R.id.zim_swiperefresh)
    library(LibraryRobot::assertLibraryListDisplayed)
  }
}
