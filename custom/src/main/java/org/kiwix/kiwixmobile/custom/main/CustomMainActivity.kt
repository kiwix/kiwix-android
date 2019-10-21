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

package org.kiwix.kiwixmobile.custom.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.Constants.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.StyleUtils.dialogStyle
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.custom.BuildConfig
import org.kiwix.kiwixmobile.custom.customActivityComponent
import java.io.File
import java.util.Locale

class CustomMainActivity : CoreMainActivity() {
  override fun injection() {
    customActivityComponent.inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d(TAG_KIWIX, "This is a custom app:$packageName")
    if (loadCustomAppContent()) {
      Log.d(TAG_KIWIX, "Found custom content, continuing...")
      // Continue
    } else {
      Log.e(TAG_KIWIX, "Problem finding the content, no more OnCreate() code")
      // What should we do here? exit? I'll investigate empirically.
      // It seems unpredictable behaviour if the code returns at this point as yesterday
      // it didn't crash yet today the app crashes because it tries to load books
      // in onResume();
    }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    val onCreateOptionsMenu = super.onCreateOptionsMenu(menu)
    menu?.findItem(R.id.menu_help)?.isVisible = false
    menu?.findItem(R.id.menu_openfile)?.isVisible = false
    return onCreateOptionsMenu
  }

  override fun createWebClient(
    webViewCallback: WebViewCallback,
    zimReaderContainer: ZimReaderContainer
  ) = CustomWebViewClient(webViewCallback, zimReaderContainer)

  /**
   * loadCustomAppContent  Return true if all's well, else false.
   */
  private fun loadCustomAppContent(): Boolean {
    Log.d(
      TAG_KIWIX,
      "Kiwix Custom App starting for the first time. Checking Companion ZIM: " +
        BuildConfig.ZIM_FILE_NAME
    )

    val currentLocaleCode = Locale.getDefault().toString()
    // Custom App recommends to start off a specific language
    if (BuildConfig.ENFORCED_LANG.length > 0 && BuildConfig.ENFORCED_LANG != currentLocaleCode) {

      // change the locale machinery
      LanguageUtils.handleLocaleChange(this, BuildConfig.ENFORCED_LANG)

      // save new locale into preferences for next startup
      sharedPreferenceUtil.putPrefLanguage(BuildConfig.ENFORCED_LANG)

      // restart activity for new locale to take effect
      this.setResult(1236)
      this.finish()
      this.startActivity(Intent(this, this.javaClass))
      return false
    }

    var filePath = ""
    if (BuildConfig.HAS_EMBEDDED_ZIM) {
      val appPath = packageResourcePath
      val libDir = File(appPath.substring(0, appPath.lastIndexOf("/")) + "/lib/")
      if (libDir.exists() && libDir.listFiles().size > 0) {
        filePath = libDir.listFiles()[0].path + "/" + BuildConfig.ZIM_FILE_NAME
      }
      if (filePath.isEmpty() || !File(filePath).exists()) {
        filePath = String.format(
          "/data/data/%s/lib/%s", packageName,
          BuildConfig.ZIM_FILE_NAME
        )
      }
    } else {
      val fileName = getExpansionAPKFileName()
      filePath = FileUtils.generateSaveFileName(fileName)
    }

    Log.d(TAG_KIWIX, "BuildConfig.ZIM_FILE_SIZE = " + BuildConfig.ZIM_FILE_SIZE)
    if (!FileUtils.doesFileExist(filePath, BuildConfig.ZIM_FILE_SIZE, false)) {

      val zimFileMissingBuilder = AlertDialog.Builder(this, dialogStyle())
      zimFileMissingBuilder.setTitle(R.string.app_name)
      zimFileMissingBuilder.setMessage(R.string.custom_app_missing_content)
      zimFileMissingBuilder.setIcon(R.mipmap.kiwix_icon)
      val activity = this
      zimFileMissingBuilder.setPositiveButton(
        getString(R.string.go_to_play_store)
      ) { dialog, which ->
        startActivity(Intent(Intent.ACTION_VIEW).apply {
          data = "market://details?id=$packageName".toUri()
        })
        activity.finish()
      }
      zimFileMissingBuilder.setCancelable(false)
      val zimFileMissingDialog = zimFileMissingBuilder.create()
      zimFileMissingDialog.show()
      return false
    } else {
      openZimFile(File(filePath))
      return true
    }
  }

  override fun manageZimFiles(tab: Int) {
    TODO("not implemented")
  }

  companion object {
    /**
     * Returns the file name (without full path) for an Expansion APK file from the given context.
     *
     * @return String the file name of the expansion file
     */
    @JvmStatic fun getExpansionAPKFileName() =
      "patch.${BuildConfig.CONTENT_VERSION_CODE}.${CoreApp.getInstance().packageName}.obb"
  }
}
