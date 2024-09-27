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

package org.kiwix.kiwixmobile.localLibrary

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.TestUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@RunWith(AndroidJUnit4::class)
class OpeningFilesFromStorageTest : BaseActivityTest() {

  private lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  private lateinit var kiwixMainActivity: KiwixMainActivity
  private lateinit var uiDevice: UiDevice
  private val fileName = "testzim.zim"

  @Before
  override fun waitForIdle() {
    Intents.init()
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      uiDevice = this
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
      putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.IS_PLAY_STORE_BUILD, true)
      putBoolean(SharedPreferenceUtil.PREF_PLAY_STORE_RESTRICTION, false)
      putString(SharedPreferenceUtil.PREF_LANG, "en")
      putLong(
        SharedPreferenceUtil.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS,
        System.currentTimeMillis()
      )
    }
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      sharedPreferenceUtil = SharedPreferenceUtil(context)
      onActivity {
        LanguageUtils.handleLocaleChange(
          it,
          "en",
          sharedPreferenceUtil
        )
      }
    }
  }

  @Test
  fun testOpeningFileWithFilePicker() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      activityScenario.onActivity {
        kiwixMainActivity = it
        it.navigate(R.id.libraryFragment)
      }
      copyFileToDownloadsFolder(context, fileName)
      sharedPreferenceUtil.copyMoveZimFilePermissionDialog = false
      // open file picker to select a file to test the real scenario.
      Espresso.onView(withId(R.id.select_file)).perform(ViewActions.click())
      uiDevice.findObject(By.textContains(fileName)).click()

      copyMoveFileHandler {
        assertCopyMovePermissionDialogDisplayed()
        clickOnMove()
        assertZimFileCopiedAndShowingIntoTheReader()
      }
    }
  }

  @Test
  fun testOpeningFileFromFileManager() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      activityScenario.onActivity {
        kiwixMainActivity = it
        it.navigate(R.id.libraryFragment)
      }
      copyFileToDownloadsFolder(context, fileName)
      val intent = Intent(Intent.ACTION_VIEW).apply {
        putExtra(
          "android.provider.extra.INITIAL_URI",
          Uri.parse("content://com.android.externalstorage.documents/document/primary:Download")
        )
        setPackage("com.android.documentsui")
      }

      kiwixMainActivity.startActivity(intent)
      sharedPreferenceUtil.copyMoveZimFilePermissionDialog = false
      uiDevice.findObject(By.textContains(fileName)).click()
    }
  }

  private fun getFileManagerPackageName(context: Context): String? {
    val packageManager = context.packageManager
    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
      type = "*/*"
      addCategory(Intent.CATEGORY_OPENABLE)
    }

    val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
    return resolveInfoList
      .asSequence()
      .map { it.activityInfo.packageName }
      .firstOrNull(::isFileManagerPackage).also {
        Log.e("PACKAGE_NAME", "getFileManagerPackageName: $it")
      }
  }

  private fun isFileManagerPackage(packageName: String): Boolean {
    val knownFileManagers = listOf(
      "com.android.fileexplorer",
      "com.android.documentsui",
      "com.google.android.documentsui",
      "com.samsung.android.documentsui",
      "com.microsoft.filemanager",
      "com.rim.browser.fileexplorer",
    )
    return knownFileManagers.contains(packageName)
  }

  private fun getSelectedFile(): File {
    val loadFileStream =
      CopyMoveFileHandlerTest::class.java.classLoader.getResourceAsStream(fileName)
    val zimFile = File(
      ContextCompat.getExternalFilesDirs(context, null)[0],
      fileName
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
    return zimFile
  }

  private fun copyFileToDownloadsFolder(
    context: Context,
    fileName: String,
    content: ByteArray = getSelectedFile().readBytes()
  ): Uri? {
    val downloadsFolder =
      Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(downloadsFolder, fileName)

    if (file.exists()) {
      // File exists, don't save a new one
      return null
    }
    // File does not exist, save the new file
    file.createNewFile()
    file.writeText("This is the new content.")
    val contentValues = ContentValues().apply {
      put(MediaStore.Downloads.DISPLAY_NAME, fileName)
      put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
      put(MediaStore.Downloads.IS_PENDING, true)
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
      resolver.openOutputStream(uri).use { outputStream ->
        outputStream?.write(content)
      }

      contentValues.clear()
      contentValues.put(MediaStore.Downloads.IS_PENDING, false)
      resolver.update(uri, contentValues, null, null)
    }

    return uri
  }

  @After
  fun release() {
    Intents.release()
  }
}
