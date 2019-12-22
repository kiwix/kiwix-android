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

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.annotation.TargetApi
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.core.content.ContextCompat
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.start
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.custom.BuildConfig
import org.kiwix.kiwixmobile.custom.R
import org.kiwix.kiwixmobile.custom.customActivityComponent
import org.kiwix.kiwixmobile.custom.download.CustomDownloadActivity
import org.kiwix.kiwixmobile.custom.main.ValidationState.HasBothFiles
import org.kiwix.kiwixmobile.custom.main.ValidationState.HasFile
import java.util.Locale
import javax.inject.Inject

const val REQUEST_READ_FOR_OBB = 5002

class CustomMainActivity : CoreMainActivity() {
  @Inject lateinit var customFileValidator: CustomFileValidator

  override fun showHomePage() {
    Log.e("CustomMain", "tried to show home page")
  }

  override fun createNewTab() {
    newMainPageTab()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    customActivityComponent.inject(this)
    super.onCreate(savedInstanceState)
    if (enforcedLanguage()) {
      return
    }
    openObbOrZim()
  }

  @TargetApi(VERSION_CODES.M)
  private fun openObbOrZim() {
    customFileValidator.validate(
      onFilesFound = {
        when (it) {
          is HasFile -> openZimFile(it.file)
          is HasBothFiles -> {
            it.zimFile.delete()
            openZimFile(it.obbFile)
          }
        }
      },
      onNoFilesFound = {
        if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PERMISSION_DENIED) {
          requestPermissions(arrayOf(READ_EXTERNAL_STORAGE), REQUEST_READ_FOR_OBB)
        } else {
          finish()
          start<CustomDownloadActivity>()
        }
      }
    )
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_READ_FOR_OBB) {
      openObbOrZim()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    val onCreateOptionsMenu = super.onCreateOptionsMenu(menu)
    menu?.findItem(R.id.menu_help)?.isVisible = false
    menu?.findItem(R.id.menu_openfile)?.isVisible = false
    menu?.findItem(R.id.menu_host_books)?.isVisible = false
    return onCreateOptionsMenu
  }

  override fun createWebClient(
    webViewCallback: WebViewCallback,
    zimReaderContainer: ZimReaderContainer
  ) = CustomWebViewClient(webViewCallback, zimReaderContainer)

  override fun getIconResId() = R.mipmap.ic_launcher

  private fun enforcedLanguage(): Boolean {
    val currentLocaleCode = Locale.getDefault().toString()
    if (BuildConfig.ENFORCED_LANG.isNotEmpty() && BuildConfig.ENFORCED_LANG != currentLocaleCode) {
      LanguageUtils.handleLocaleChange(this, BuildConfig.ENFORCED_LANG)
      sharedPreferenceUtil.putPrefLanguage(BuildConfig.ENFORCED_LANG)
      recreate()
      return true
    }
    return false
  }

  override fun manageZimFiles(tab: Int) {
    // Do nothing
  }
}
