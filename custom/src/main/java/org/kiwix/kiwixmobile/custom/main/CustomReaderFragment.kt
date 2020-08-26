/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions.Super
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions.Super.ShouldCall
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setupDrawerToggle
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.start
import org.kiwix.kiwixmobile.core.main.CoreReaderFragment
import org.kiwix.kiwixmobile.core.main.MainMenu
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.custom.BuildConfig
import org.kiwix.kiwixmobile.custom.R
import org.kiwix.kiwixmobile.custom.customActivityComponent
import org.kiwix.kiwixmobile.custom.download.CustomDownloadActivity
import java.util.Locale
import javax.inject.Inject

const val PAGE_URL_KEY = "pageUrl"

class CustomReaderFragment : CoreReaderFragment() {

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.customActivityComponent.inject(this)
  }

  @Inject lateinit var customFileValidator: CustomFileValidator
  @Inject lateinit var dialogShower: DialogShower

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (enforcedLanguage()) {
      return
    }

    setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    if (BuildConfig.DISABLE_SIDEBAR) {
      val toolbarToc = activity?.findViewById<ImageView>(R.id.bottom_toolbar_toc)
      toolbarToc?.isEnabled = false
    }
    with(activity as AppCompatActivity) {
      supportActionBar!!.setDisplayHomeAsUpEnabled(true)
      setupDrawerToggle(toolbar)
    }
    loadPageFromNavigationArguments()
  }

  private fun loadPageFromNavigationArguments() {
    val args = CustomReaderFragmentArgs.fromBundle(requireArguments())
    if (args.pageUrl.isNotEmpty()) {
      if (args.shouldOpenInNewTab) {
        manageExternalLaunchAndRestoringViewState()
        createNewTab()
      }
      loadUrlWithCurrentWebview(args.pageUrl)
    } else {
      openObbOrZim()
      manageExternalLaunchAndRestoringViewState()
    }
    if (args.findInPageSearchString.isNotEmpty()) {
      findInPage(args.findInPageSearchString)
    }
    requireArguments().clear()
  }

  override fun restoreViewStateOnInvalidJSON() {
    openHomeScreen()
  }

  override fun restoreViewStateOnValidJSON(
    zimArticles: String,
    zimPositions: String,
    currentTab: Int
  ) {
    restoreTabs(zimArticles, zimPositions, currentTab)
  }

  override fun setDrawerLockMode(lockMode: Int) {
    super.setDrawerLockMode(
      if (BuildConfig.DISABLE_SIDEBAR) DrawerLayout.LOCK_MODE_LOCKED_CLOSED
      else lockMode
    )
  }

  @TargetApi(Build.VERSION_CODES.M)
  private fun openObbOrZim() {
    customFileValidator.validate(
      onFilesFound = {
        when (it) {
          is ValidationState.HasFile -> openZimFile(it.file)
          is ValidationState.HasBothFiles -> {
            it.zimFile.delete()
            openZimFile(it.obbFile)
          }
        }
      },
      onNoFilesFound = {
        if (ContextCompat.checkSelfPermission(
            requireActivity(),
            READ_EXTERNAL_STORAGE
          ) == PERMISSION_DENIED
        ) {
          requestPermissions(arrayOf(READ_EXTERNAL_STORAGE), REQUEST_READ_FOR_OBB)
        } else {
          activity?.finish()
          activity?.start<CustomDownloadActivity>()
        }
      }
    )
  }

  override fun onBackPressed(activity: AppCompatActivity): Super {
    val result = super.onBackPressed(activity)
    if (zimReaderContainer.mainPage == getCurrentWebView().url.substringAfter(CONTENT_PREFIX)) {
      return ShouldCall
    }
    return result
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (permissions.isNotEmpty() && permissions[0] == Manifest.permission.READ_EXTERNAL_STORAGE) {
      if (readStorageHasBeenPermanentlyDenied(grantResults)) {
        dialogShower.show(KiwixDialog.ReadPermissionRequired, ::goToSettings)
      } else {
        openObbOrZim()
      }
    }
  }

  private fun goToSettings() {
    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", activity?.packageName, null)
    })
  }

  private fun readStorageHasBeenPermanentlyDenied(grantResults: IntArray) =
    grantResults[0] == PackageManager.PERMISSION_DENIED &&
      !ActivityCompat.shouldShowRequestPermissionRationale(
        requireActivity(),
        Manifest.permission.READ_EXTERNAL_STORAGE
      )

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    menu.findItem(R.id.menu_help)?.isVisible = false
    menu.findItem(R.id.menu_host_books)?.isVisible = false
  }

  private fun enforcedLanguage(): Boolean {
    val currentLocaleCode = Locale.getDefault().toString()
    if (BuildConfig.ENFORCED_LANG.isNotEmpty() && BuildConfig.ENFORCED_LANG != currentLocaleCode) {
      LanguageUtils.handleLocaleChange(requireActivity(), BuildConfig.ENFORCED_LANG)
      sharedPreferenceUtil.putPrefLanguage(BuildConfig.ENFORCED_LANG)
      activity?.recreate()
      return true
    }
    return false
  }

  override fun loadDrawerViews() {
    drawerLayout = requireActivity().findViewById(R.id.custom_drawer_container)
    tableDrawerRightContainer = requireActivity().findViewById(R.id.activity_main_nav_view)
  }

  override fun createMainMenu(menu: Menu?): MainMenu {
    return menuFactory.create(
      menu!!,
      webViewList,
      urlIsValid(),
      this,
      BuildConfig.DISABLE_READ_ALOUD,
      BuildConfig.DISABLE_TABS
    )
  }

  override fun showOpenInNewTabDialog(url: String?) {
    if (BuildConfig.DISABLE_TABS) return
    super.showOpenInNewTabDialog(url)
  }

  override fun configureWebViewSelectionHandler(menu: Menu?) {
    if (BuildConfig.DISABLE_READ_ALOUD) {
      menu?.findItem(org.kiwix.kiwixmobile.core.R.id.menu_speak_text)?.isVisible = false
    }
    super.configureWebViewSelectionHandler(menu)
  }

  override fun createNewTab() {
    newMainPageTab()
  }
}
