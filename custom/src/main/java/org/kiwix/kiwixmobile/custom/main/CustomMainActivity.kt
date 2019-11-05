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
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.start
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.custom.BuildConfig
import org.kiwix.kiwixmobile.custom.customActivityComponent
import org.kiwix.kiwixmobile.custom.download.CustomDownloadActivity
import org.kiwix.kiwixmobile.custom.main.ValidationState.HasBothFiles
import org.kiwix.kiwixmobile.custom.main.ValidationState.HasFile
import java.io.File
import java.util.Locale
import javax.inject.Inject

class CustomMainActivity : CoreMainActivity() {
  @Inject lateinit var customFileValidator: CustomFileValidator

  override fun showHomePage() {
    Log.e("CustomMain", "tried to show home page")
  }

  override fun createNewTab() {
    newMainPageTab()
  }

  override fun injection() {
    customActivityComponent.inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requireEnforcedLanguage()
    customFileValidator.validate(
      {
        when (it) {
          is HasFile -> openZimFile(it.file)
          is HasBothFiles -> {
            it.zimFile.delete()
            openZimFile(it.obbFile)
          }
        }
      },
      {
        finish()
        start<CustomDownloadActivity>()
      }
    )
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

  private fun requireEnforcedLanguage(): Boolean {
    val currentLocaleCode = Locale.getDefault().toString()
    if (BuildConfig.ENFORCED_LANG.isNotEmpty() && BuildConfig.ENFORCED_LANG != currentLocaleCode) {
      LanguageUtils.handleLocaleChange(this, BuildConfig.ENFORCED_LANG)
      sharedPreferenceUtil.putPrefLanguage(BuildConfig.ENFORCED_LANG)
      startActivity(Intent(this, this.javaClass))
      return true
    }
    return false
  }

  override fun manageZimFiles(tab: Int) {
    TODO("not implemented")
  }

  companion object {
    private fun getExpansionAPKFileName() =
      "main.${BuildConfig.CONTENT_VERSION_CODE}.${CoreApp.getInstance().packageName}.obb"

    fun generateExpansionFilePath(fileName: String = getExpansionAPKFileName()) =
      "${CoreApp.getInstance().obbDir}${File.separator}$fileName"
  }
}
