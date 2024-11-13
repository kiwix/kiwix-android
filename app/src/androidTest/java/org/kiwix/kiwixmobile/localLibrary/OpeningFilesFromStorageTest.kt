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
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
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
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.fail
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@RunWith(AndroidJUnit4::class)
class OpeningFilesFromStorageTest : BaseActivityTest() {
  @Rule
  @JvmField
  var retryRule = RetryRule()

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
      val uri = copyFileToDownloadsFolder(context, fileName)
      try {
        sharedPreferenceUtil.shouldShowStorageSelectionDialog = true
        // open file picker to select a file to test the real scenario.
        Espresso.onView(withId(R.id.select_file)).perform(ViewActions.click())
        uiDevice.findObject(By.textContains(fileName)).click()

        copyMoveFileHandler {
          assertCopyMoveDialogDisplayed()
          clickOnMove()
          assertStorageSelectionDialogDisplayed()
          clickOnInternalStorage()
          assertZimFileCopiedAndShowingIntoTheReader()
        }
      } catch (ignore: Exception) {
        fail("Could not open file from file manager. Original exception = $ignore")
      } finally {
        deleteAllFilesInDirectory(File(sharedPreferenceUtil.prefStorage))
        deleteZimFileFromDownloadsFolder(uri!!)
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
      val uri = copyFileToDownloadsFolder(context, fileName)
      try {
        sharedPreferenceUtil.shouldShowStorageSelectionDialog = true
        openFileManager()
        TestUtils.testFlakyView(uiDevice.findObject(By.textContains(fileName))::click, 10)
        copyMoveFileHandler {
          assertCopyMoveDialogDisplayed()
          clickOnMove()
          assertStorageSelectionDialogDisplayed()
          clickOnInternalStorage()
          assertZimFileCopiedAndShowingIntoTheReader()
        }
      } catch (ignore: Exception) {
        fail("Could not open file from file manager. Original exception = $ignore")
      } finally {
        deleteAllFilesInDirectory(File(sharedPreferenceUtil.prefStorage))
        deleteZimFileFromDownloadsFolder(uri!!)
      }
    }
  }

  @Test
  fun testOpeningFileFromBrowser() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      testCopyMoveDialogShowing(
        Uri.parse(
          "content://com.opera.browser.DownloadProvider/downloads/beer.stackexchange" +
            ".com_en_all_2023-05.zim?hash=w4A3vMuc7l1FPQwk23rmbgvfPGJfhcj5FujW37NApdA%3D&mt=" +
            "application%2Foctet-stream"
        )
      )
      testCopyMoveDialogShowing(
        Uri.parse(
          "content://org.mozilla.firefox.DownloadProvider/" +
            "downloads/beer.stackexchange.com_en_all_2023-05.zim"
        )
      )
    }
  }

  private fun testCopyMoveDialogShowing(uri: Uri) {
    sharedPreferenceUtil.shouldShowStorageSelectionDialog = true
    ActivityScenario.launch<KiwixMainActivity>(
      createDeepLinkIntent(uri)
    ).onActivity {}
    copyMoveFileHandler {
      assertCopyMoveDialogDisplayed()
      clickOnCancel()
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

  // delete the zim file from the downloads folder which we have put in the download folder.
  // Since after this test case we do not have the access to that file, and that file takes the
  // memory in device.
  private fun deleteZimFileFromDownloadsFolder(uri: Uri) {
    val resolver = context.contentResolver
    try {
      val rowsDeleted = resolver.delete(uri, null, null)
      if (rowsDeleted > 0) {
        Log.d("FileDeletion", "Deleted: $uri")
      } else {
        Log.e("FileDeletion", "Failed to delete: $uri")
      }
    } catch (e: Exception) {
      Log.e("FileDeletion", "Error deleting file: $uri, Error: ${e.message}")
    }
  }

  private fun openFileManager() {
    val intent =
      context.packageManager.getLaunchIntentForPackage("com.android.documentsui")
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    context.startActivity(intent)
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
      MediaScannerConnection.scanFile(
        context,
        arrayOf(uri.toString()),
        arrayOf("application/octet-stream"),
        null
      )
    }

    return uri
  }

  private fun deleteAllFilesInDirectory(directory: File) {
    if (directory.isDirectory) {
      directory.listFiles()?.forEach { file ->
        if (file.isDirectory) {
          // Recursively delete files in subdirectories
          deleteAllFilesInDirectory(file)
        }
        file.delete()
      }
    }
  }

  @After
  fun release() {
    Intents.release()
  }
}
