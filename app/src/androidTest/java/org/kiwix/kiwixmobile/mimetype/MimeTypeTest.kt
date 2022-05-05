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

import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.rule.ActivityTestRule
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

  @Test
  fun testMimeType() {
    val loadFileStream = MimeTypeTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    val zimFile = File(context.cacheDir, "testzim.zim")
    if (zimFile.exists()) zimFile.delete()
    zimFile.createNewFile()
    loadFileStream.use { inputStream ->
      val out: OutputStream = FileOutputStream(zimFile)
      out.use { out ->
        // Transfer bytes from in to out
        val buf = ByteArray(1024)
        var len: Int
        while (inputStream.read(buf).also { len = it } > 0) {
          out.write(buf, 0, len)
        }
      }
    }
    val zimFileReader = ZimFileReader(
      zimFile,
      JNIKiwixReader(zimFile.canonicalPath),
      NightModeConfig(SharedPreferenceUtil(context), context)
    )
    zimFileReader.readMimeType(zimFileReader.getRandomArticleUrl()!!).also {
      Log.e("ZIMFILEREADER", "testMimeType: $it")
    }
  }
}
