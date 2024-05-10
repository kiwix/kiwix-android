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

package org.kiwix.kiwixmobile.deeplinks

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.fail
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.page.history.navigationHistory
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class DeepLinksTest : BaseActivityTest() {
  @Rule
  @JvmField
  var retryRule = RetryRule()
  private lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    context.let {
      sharedPreferenceUtil = SharedPreferenceUtil(it).apply {
        setIntroShown()
        putPrefWifiOnly(false)
        setIsPlayStoreBuildType(true)
        prefIsTest = true
        playStoreRestrictionPermissionDialog = false
      }
    }
  }

  @Test
  fun fileTypeDeepLinkTest() {
    loadZimFileInApplicationAndReturnSchemeTypeUri("file")?.let {
      // Launch the activity to test the deep link
      ActivityScenario.launch<KiwixMainActivity>(createDeepLinkIntent(it)).onActivity {}
      navigationHistory {
        checkZimFileLoadedSuccessful(R.id.readerFragment)
        assertZimFileLoaded() // check if the zim file successfully loaded
        clickOnAndroidArticle()
      }
    } ?: kotlin.run {
      // error in getting the zim file Uri
      fail("Couldn't get file type Uri for zim file")
    }
  }

  @Test
  fun contentTypeDeepLinkTest() {
    loadZimFileInApplicationAndReturnSchemeTypeUri("content")?.let {
      // Launch the activity to test the deep link
      ActivityScenario.launch<KiwixMainActivity>(createDeepLinkIntent(it)).onActivity {}
      navigationHistory {
        checkZimFileLoadedSuccessful(R.id.readerFragment)
        assertZimFileLoaded() // check if the zim file successfully loaded
        clickOnAndroidArticle()
      }
    } ?: kotlin.run {
      // error in getting the zim file Uri
      fail("Couldn't get file type Uri for zim file")
    }
  }

  private fun loadZimFileInApplicationAndReturnSchemeTypeUri(schemeType: String): Uri? {
    val loadFileStream =
      DeepLinksTest::class.java.classLoader.getResourceAsStream("testzim.zim")
    val zimFile = File(sharedPreferenceUtil.prefStorage, "testzim.zim")
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
    return when (schemeType) {
      "file" -> Uri.fromFile(zimFile)
      "content" -> FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        zimFile
      )

      else -> null
    }
  }

  private fun createDeepLinkIntent(uri: Uri): Intent {
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/octet-stream")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      setPackage(context.packageName)
    }
    return intent
  }
}
