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

package org.kiwix.kiwixmobile.main

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import org.json.JSONArray
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.utils.Constants.TAG_CURRENT_ARTICLES
import org.kiwix.kiwixmobile.core.utils.Constants.TAG_CURRENT_FILE
import org.kiwix.kiwixmobile.core.utils.Constants.TAG_CURRENT_POSITIONS
import org.kiwix.kiwixmobile.core.utils.Constants.TAG_CURRENT_TAB
import org.kiwix.kiwixmobile.core.utils.Constants.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_KIWIX_MOBILE
import org.kiwix.kiwixmobile.core.utils.UpdateUtils.reformatProviderUrl
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.zim_manager.ZimReaderContainer
import org.kiwix.kiwixmobile.kiwixComponent
import java.io.File

class KiwixMainActivity : CoreMainActivity() {

  override fun injection() {
    kiwixComponent.inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    manageExternalLaunchAndRestoringViewState()
  }

  override fun createWebClient(
    webViewCallback: WebViewCallback,
    zimReaderContainer: ZimReaderContainer
  ) = KiwixWebViewClient(webViewCallback, zimReaderContainer)

  private fun manageExternalLaunchAndRestoringViewState() {

    val data = intent.data
    if (data != null) {
      val filePath = FileUtils.getLocalFilePathByUri(applicationContext, data)

      if (filePath == null || !File(filePath).exists()) {
        toast(R.string.error_file_not_found)
        return
      }

      Log.d(
        TAG_KIWIX, "Kiwix started from a file manager. Intent filePath: " +
          filePath +
          " -> open this zim file and load menu_main page"
      )
      openZimFile(File(filePath), false)
    } else {
      val settings = getSharedPreferences(PREF_KIWIX_MOBILE, 0)
      val zimFile = settings.getString(TAG_CURRENT_FILE, null)
      if (zimFile != null && File(zimFile).exists()) {
        Log.d(
          TAG_KIWIX,
          "Kiwix normal start, zimFile loaded last time -> Open last used zimFile $zimFile"
        )
        restoreTabStates()
        // Alternative would be to restore webView state. But more effort to implement, and actually
        // fits better normal android behavior if after closing app ("back" button) state is not maintained.
      } else {
        Log.d(TAG_KIWIX, "Kiwix normal start, no zimFile loaded last time  -> display home page")
        showHomePage()
      }
    }
  }

  private fun restoreTabStates() {
    val settings = getSharedPreferences(PREF_KIWIX_MOBILE, 0)
    val zimFile = settings.getString(TAG_CURRENT_FILE, null)
    val zimArticles = settings.getString(TAG_CURRENT_ARTICLES, null)
    val zimPositions = settings.getString(TAG_CURRENT_POSITIONS, null)

    val currentTab = settings.getInt(TAG_CURRENT_TAB, 0)

    if (zimFile != null) {
      openZimFile(File(zimFile), false)
    } else {
      Toast.makeText(this, "Unable to open zim file", Toast.LENGTH_SHORT).show()
    }
    try {
      val urls = JSONArray(zimArticles)
      val positions = JSONArray(zimPositions)
      var i = 0
      currentWebView.loadUrl(reformatProviderUrl(urls.getString(i)))
      currentWebView.scrollY = positions.getInt(i)
      i++
      while (i < urls.length()) {
        newTab(reformatProviderUrl(urls.getString(i)))
        currentWebView.scrollY = positions.getInt(i)
        i++
      }
      selectTab(currentTab)
    } catch (e: Exception) {
      Log.w(TAG_KIWIX, "Kiwix shared preferences corrupted", e)
      // TODO: Show to user
    }
  }
}
