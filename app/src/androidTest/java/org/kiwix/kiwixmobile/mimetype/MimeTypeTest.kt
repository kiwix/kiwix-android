/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.mimetype

import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.kiwix.kiwixlib.JNIKiwixReader
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.NightModeConfig
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MimeTypeTest : BaseActivityTest() {

  override var activityRule: ActivityTestRule<KiwixMainActivity> = activityTestRule {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
    }
  }

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()
  }

  @Test
  fun testMimeType() {
    val loadFileStream = MimeTypeTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    val zimFile = File(context.cacheDir, "testzim.zim")
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
    val zimFileReader = ZimFileReader(
      zimFile,
      JNIKiwixReader(zimFile.canonicalPath),
      NightModeConfig(SharedPreferenceUtil(context), context)
    )
    zimFileReader.getRandomArticleUrl()?.let {
      val mimeType = zimFileReader.readContentAndMimeType(it)
      if (mimeType.contains("^([^ ]+).*$") || mimeType.contains(";")) {
        Assert.fail(
          "Unable to get mime type from zim file. File = " +
            " $zimFile and url of article = $it"
        )
      }
    }.also {
      zimFileReader.dispose()
    } ?: kotlin.run {
      Assert.fail("Unable to get article from zim file $zimFile")
    }.also {
      zimFileReader.dispose()
    }
  }
}
