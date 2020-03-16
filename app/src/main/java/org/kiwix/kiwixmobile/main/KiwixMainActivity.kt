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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.start
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_ZIM_FILE
import org.kiwix.kiwixmobile.core.utils.Constants.TAG_CURRENT_ARTICLES
import org.kiwix.kiwixmobile.core.utils.Constants.TAG_CURRENT_FILE
import org.kiwix.kiwixmobile.core.utils.Constants.TAG_CURRENT_POSITIONS
import org.kiwix.kiwixmobile.core.utils.Constants.TAG_CURRENT_TAB
import org.kiwix.kiwixmobile.core.utils.Constants.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_KIWIX_MOBILE
import org.kiwix.kiwixmobile.core.utils.UpdateUtils.reformatProviderUrl
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.webserver.ZimHostActivity
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity
import java.io.File

class KiwixMainActivity : CoreMainActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    manageExternalLaunchAndRestoringViewState()
  }

  override fun onResume() {
    super.onResume()
    if (zimReaderContainer.zimFile == null && HOME_URL != currentWebView.url) {
      showHomePage()
    }
  }

  override fun createWebClient(
    webViewCallback: WebViewCallback,
    zimReaderContainer: ZimReaderContainer
  ) = KiwixWebViewClient(webViewCallback, zimReaderContainer)

  private fun manageExternalLaunchAndRestoringViewState() {

    val data = uriFromIntent()
    if (data != null) {
      val filePath = FileUtils.getLocalFilePathByUri(applicationContext, data)

      if (filePath == null || !File(filePath).exists()) {
        currentWebView.snack(R.string.error_file_not_found)
        return
      }

      Log.d(
        TAG_KIWIX, "Kiwix started from a file manager. Intent filePath: " +
          filePath +
          " -> open this zim file and load menu_main page"
      )
      openZimFile(File(filePath))
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

  override fun injection(coreComponent: CoreComponent) {
    kiwixActivityComponent.inject(this)
  }

  override fun hasValidFileAndUrl(url: String?, zimFileReader: ZimFileReader?) =
    super.hasValidFileAndUrl(url, zimFileReader) && url != HOME_URL

  override fun getIconResId() = R.mipmap.ic_launcher

  override fun urlIsInvalid() =
    super.urlIsInvalid() || currentWebView.url == HOME_URL

  override fun showHomePage() {
    currentWebView.removeAllViews()
    currentWebView.loadUrl(HOME_URL)
  }

  override fun createNewTab() {
    newTab(HOME_URL)
  }

  override fun isInvalidTitle(zimFileTitle: String?) =
    super.isInvalidTitle(zimFileTitle) || HOME_URL == currentWebView.url

  private fun uriFromIntent() =
    intent.data ?: intent.getStringExtra(EXTRA_ZIM_FILE)?.let {
      File(FileUtils.getFileName(it)).toUri()
    }

  private fun restoreTabStates() {
    val settings = getSharedPreferences(PREF_KIWIX_MOBILE, 0)
    val zimFile = settings.getString(TAG_CURRENT_FILE, null)
    val zimArticles = settings.getString(TAG_CURRENT_ARTICLES, null)
    val zimPositions = settings.getString(TAG_CURRENT_POSITIONS, null)

    val currentTab = settings.getInt(TAG_CURRENT_TAB, 0)

    if (zimFile != null) {
      openZimFile(File(zimFile))
    } else {
      currentWebView.snack(R.string.error_file_not_opened)
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

  override fun manageZimFiles(tab: Int) {
    start<ZimManageActivity> { putExtra(ZimManageActivity.TAB_EXTRA, tab) }
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    intent?.data?.let {
      if ("file" == it.scheme) openZimFile(it.toFile())
      else toast(R.string.cannot_open_file)
    }
  }

  override fun onHostBooksMenuClicked() {
    start<ZimHostActivity>()
  }
}
