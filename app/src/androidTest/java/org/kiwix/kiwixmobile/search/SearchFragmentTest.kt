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
package org.kiwix.kiwixmobile.search

import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import leakcanary.LeakAssertions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.LocalLibraryFragmentDirections.actionNavigationLibraryToNavigationReader
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class SearchFragmentTest : BaseActivityTest() {

  @Rule
  @JvmField
  var retryRule = RetryRule()

  private lateinit var kiwixMainActivity: KiwixMainActivity

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(context)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.PREF_PLAY_STORE_RESTRICTION, false)
    }
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
    }
  }

  @Test
  fun searchFragmentSimple() {
    activityScenario.onActivity {
      kiwixMainActivity = it
      kiwixMainActivity.navigate(R.id.libraryFragment)
    }
    val loadFileStream =
      SearchFragmentTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    val zimFile = File(context.cacheDir, "testzim.zim")
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
    UiThreadStatement.runOnUiThread {
      kiwixMainActivity.navigate(
        actionNavigationLibraryToNavigationReader()
          .apply { zimFileUri = zimFile.toUri().toString() }
      )
    }
    search { checkZimFileSearchSuccessful(R.id.readerFragment) }
    UiThreadStatement.runOnUiThread {
      if (zimFile.canRead()) {
        kiwixMainActivity.openSearch(searchString = "Android")
      } else {
        throw RuntimeException(
          "File $zimFile is not readable." +
            " Original File $zimFile is readable = ${zimFile.canRead()}" +
            " Size ${zimFile.length()}"
        )
      }
    }
    search {
      clickOnSearchItemInSearchList()
      checkZimFileSearchSuccessful(R.id.readerFragment)
    }
    LeakAssertions.assertNoLeaks()
  }

  @After
  fun setIsTestPreference() {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, false)
    }
  }
}
