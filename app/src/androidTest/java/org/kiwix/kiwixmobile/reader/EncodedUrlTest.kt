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
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.NightModeConfig
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.libzim.Archive
import org.kiwix.libzim.SuggestionSearcher
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class EncodedUrlTest : BaseActivityTest() {

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
      putString(SharedPreferenceUtil.PREF_LANG, "en")
    }
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        handleLocaleChange(
          it,
          "en",
          SharedPreferenceUtil(context)
        )
      }
    }
  }

  @Test
  fun testEncodedUrls() {
    val loadFileStream =
      EncodedUrlTest::class.java.classLoader.getResourceAsStream("characters_encoding.zim")
    val zimFile = File(
      ContextCompat.getExternalFilesDirs(context, null)[0],
      "characters_encoding.zim"
    )
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    loadFileStream.use { inputStream ->
      val outputStream: OutputStream = FileOutputStream(zimFile)
      outputStream.use { it ->
        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
          it.write(buffer, 0, length)
        }
      }
    }
    val archive = Archive(zimFile.canonicalPath)
    val zimFileReader = ZimFileReader(
      zimFile,
      null,
      null,
      archive,
      NightModeConfig(SharedPreferenceUtil(context), context),
      SuggestionSearcher(archive)
    )
    val encodedUrls = arrayOf(
      EncodedUrl(
        "https://kiwix.app/foo/part%2520with%2520space/bar%253Fkey%253Dvalue",
        "https://kiwix.app/foo/part%2520with%2520space/bar%253Fkey%253Dvalue"
      ),
      EncodedUrl(
        "https://kiwix.app/foo/part%20with%20space/bar%3Fkey%3Dvalue",
        "https://kiwix.app/foo/part%20with%20space/bar%3Fkey%3Dvalue"
      ),
      EncodedUrl(
        "https://kiwix.app/foo%2Fpart%20with%20space%2Fbar%3Fkey%3Dvalue",
        "https://kiwix.app/foo%2Fpart%20with%20space%2Fbar%3Fkey%3Dvalue"
      ),
      EncodedUrl(
        "https://kiwix.app/foo/part%20with%20space/bar?key=value",
        "https://kiwix.app/foo/part%20with%20space/bar?key=value"
      ),
      EncodedUrl(
        "https://kiwix.app/foo/part/file%20with%20%3F%20and%20+?who=Chip%26Dale&quer=Is%20" +
          "there%20any%20%2B%20here%3F",
        "https://kiwix.app/foo/part/file%20with%20%3F%20and%20+?who=Chip%26Dale&quer" +
          "=Is%20there%20any%20%2B%20here%3F"
      ),
      EncodedUrl(
        "https://kiwix.app/foo/part/file%20with%20%253F%20and%20%2B%3Fwho%3DChip%2526Dale%26" +
          "quer%3DIs%2520there%2520any%2520%252B%2520here%253F",
        "https://kiwix.app/foo/part/file%20with%20%253F%20and%20%2B%3Fwho%3DChip" +
          "%2526Dale%26quer%3DIs%2520there%2520any%2520%252B%2520here%253F"
      ),
      EncodedUrl(
        "https://kiwix.app/foo/part/file%20with%20%3F%20and%20%2B%3Fwho%3DChip%26Dale%26" +
          "question%3DIt%20there%20any%20%2B%20here%3F",
        "https://kiwix.app/foo/part/file%20with%20%3F%20and%20%2B%3Fwho%3DChip%26" +
          "Dale%26question%3DIt%20there%20any%20%2B%20here%3F"
      ),
      EncodedUrl(
        "https://kiwix.app/foo/part/file%3Fquestion%3DIs%2520there%2520any" +
          "%2520%252B%2520here%253F",
        "https://kiwix.app/foo/part/file%3Fquestion%3DIs%2520there%2520" +
          "any%2520%252B%2520here%253F"
      ),
      EncodedUrl(
        "https://kiwix.app/foo/part/file%3Fquestion%3DIs%2Bthere%2Bany%2B%252B%2Bhere%253F",
        "https://kiwix.app/foo/part/file%3Fquestion%3DIs%2Bthere%2Bany%2B%252B%2B" +
          "here%253F"
      ),
      EncodedUrl(
        "https://kiwix.app/%F0%9F%A5%B3%F0%9F%A5%B0%F0%9F%98%98%F0%9F%A4%A9%F0%9F%98%8D%F0%9F" +
          "%A4%8D%F0%9F%8E%80%F0%9F%A7%B8%F0%9F%8C%B7%F0%9F%8D%AD",
        "https://kiwix.app/%F0%9F%A5%B3%F0%9F%A5%B0%F0%9F%98%98%F0%9F%A4%A9%F0%9F%98%8D" +
          "%F0%9F%A4%8D%F0%9F%8E%80%F0%9F%A7%B8%F0%9F%8C%B7%F0%9F%8D%AD"
      )
    )
    encodedUrls.forEach {
      Assert.assertEquals(
        it.expectedUrl,
        zimFileReader.getRedirect(it.url)
      )
    }
    // dispose the ZimFileReader
    zimFileReader.dispose()
  }

  data class EncodedUrl(val url: String, val expectedUrl: String)
}
