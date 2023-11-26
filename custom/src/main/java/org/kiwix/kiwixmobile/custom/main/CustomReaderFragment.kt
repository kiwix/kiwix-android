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

import android.app.Dialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.findNavController
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setupDrawerToggle
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.main.CoreReaderFragment
import org.kiwix.kiwixmobile.core.main.MainMenu
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.getDemoFilePathForCustomApp
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.custom.BuildConfig
import org.kiwix.kiwixmobile.custom.R
import org.kiwix.kiwixmobile.custom.customActivityComponent
import java.io.File
import java.util.Locale
import javax.inject.Inject

class CustomReaderFragment : CoreReaderFragment() {

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.customActivityComponent.inject(this)
  }

  @Inject
  lateinit var customFileValidator: CustomFileValidator

  @JvmField
  @Inject
  var dialogShower: DialogShower? = null
  private var permissionRequiredDialog: Dialog? = null
  private var appSettingsLaunched = false
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    if (enforcedLanguage()) {
      return
    }

    if (isAdded) {
      setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
      if (BuildConfig.DISABLE_SIDEBAR) {
        val toolbarToc =
          activity?.findViewById<ImageView>(org.kiwix.kiwixmobile.core.R.id.bottom_toolbar_toc)
        toolbarToc?.isEnabled = false
      }
      with(activity as AppCompatActivity) {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar?.let { setupDrawerToggle(it) }
      }
      loadPageFromNavigationArguments()
    }
  }

  private fun loadPageFromNavigationArguments() {
    val args = CustomReaderFragmentArgs.fromBundle(requireArguments())
    if (args.pageUrl.isNotEmpty()) {
      loadUrlWithCurrentWebview(args.pageUrl)
    } else {
      openObbOrZim()
      manageExternalLaunchAndRestoringViewState()
    }
    requireArguments().clear()
  }

  /**
   * Restores the view state when the attempt to read JSON from shared preferences fails
   * due to invalid or corrupted data. In this case, it opens the homepage of the zim file,
   * as custom apps always have the zim file available.
   */
  override fun restoreViewStateOnInvalidJSON() {
    openHomeScreen()
  }

  /**
   * Restores the view state when the JSON data is valid. This method restores the tabs
   * and loads the last opened article in the specified tab.
   */
  override fun restoreViewStateOnValidJSON(
    zimArticles: String?,
    zimPositions: String?,
    currentTab: Int
  ) {
    restoreTabs(zimArticles, zimPositions, currentTab)
  }

  /**
   * Sets the locking mode for the sidebar in a custom app. If the app is configured not to show the sidebar,
   * this function disables the sidebar by locking it in the closed position through the parent class.
   * https://developer.android.com/reference/kotlin/androidx/drawerlayout/widget/DrawerLayout#LOCK_MODE_LOCKED_CLOSED()
   */
  override fun setDrawerLockMode(lockMode: Int) {
    super.setDrawerLockMode(
      if (BuildConfig.DISABLE_SIDEBAR) DrawerLayout.LOCK_MODE_LOCKED_CLOSED
      else lockMode
    )
  }

  private fun openObbOrZim() {
    customFileValidator.validate(
      onFilesFound = {
        when (it) {
          is ValidationState.HasFile -> {
            if (it.assetFileDescriptor != null) {
              openZimFile(null, true, it.assetFileDescriptor)
            } else {
              openZimFile(it.file, true)
            }
            // Save book in the database to display it in `ZimHostFragment`.
            zimReaderContainer?.zimFileReader?.let { zimFileReader ->
              // Check if the file is not null. If the file is null,
              // it means we have created zimFileReader with a fileDescriptor,
              // so we create a demo file to save it in the database for display on the `ZimHostFragment`.
              val file = it.file ?: createDemoFile()
              val bookOnDisk = BookOnDisk(file, zimFileReader)
              repositoryActions?.saveBook(bookOnDisk)
            }
          }
          is ValidationState.HasBothFiles -> {
            it.zimFile.delete()
            openZimFile(it.obbFile, true)
          }
          else -> {}
        }
      },
      onNoFilesFound = {
        findNavController().navigate(R.id.customDownloadFragment)
      }
    )
  }

  private fun createDemoFile() =
    File(getDemoFilePathForCustomApp(requireActivity())).also {
      if (!it.isFileExist()) it.createNewFile()
    }

  @Suppress("DEPRECATION")
  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    menu.findItem(org.kiwix.kiwixmobile.core.R.id.menu_help)?.isVisible = false
    menu.findItem(org.kiwix.kiwixmobile.core.R.id.menu_host_books)?.isVisible = false
  }

  private fun enforcedLanguage(): Boolean {
    val currentLocaleCode = Locale.getDefault().toString()
    if (BuildConfig.ENFORCED_LANG.isNotEmpty() && BuildConfig.ENFORCED_LANG != currentLocaleCode) {
      sharedPreferenceUtil?.let { sharedPreferenceUtil ->
        LanguageUtils.handleLocaleChange(
          requireActivity(),
          BuildConfig.ENFORCED_LANG,
          sharedPreferenceUtil
        )
        sharedPreferenceUtil.putPrefLanguage(BuildConfig.ENFORCED_LANG)
      }
      activity?.recreate()
      return true
    }
    return false
  }

  /**
   * This method is overridden to set the IDs of the `drawerLayout` and `tableDrawerRightContainer`
   * specific to the custom module in the `CoreReaderFragment`. Since we have an app and a custom module,
   * and `CoreReaderFragment` is a common class for both modules, we set the IDs of the custom module
   * in the parent class to ensure proper integration.
   */
  override fun loadDrawerViews() {
    drawerLayout = requireActivity().findViewById(R.id.custom_drawer_container)
    tableDrawerRightContainer = requireActivity().findViewById(R.id.activity_main_nav_view)
  }

  /**
   * Overrides the method to create the main menu for the app. The custom app can be configured to disable
   * features like "read aloud" and "tabs," and this method dynamically generates the menu based on the
   * provided configuration. It takes into account whether read aloud and tabs are enabled or disabled
   * and creates the menu accordingly.
   */
  override fun createMainMenu(menu: Menu?): MainMenu? {
    return menu?.let {
      menuFactory?.create(
        it,
        webViewList,
        urlIsValid(),
        this,
        BuildConfig.DISABLE_READ_ALOUD,
        BuildConfig.DISABLE_TABS
      )
    }
  }

  /**
   * Overrides the method to control the functionality of showing the "Open In New Tab" dialog.
   * When a user long-clicks on an article, the app typically prompts the "ShowOpenInNewTabDialog."
   * However, if a custom app is configured to disable the use of tabs, this function restricts
   * the dialog from appearing.
   */
  override fun showOpenInNewTabDialog(url: String) {
    if (BuildConfig.DISABLE_TABS) return
    super.showOpenInNewTabDialog(url)
  }

  /**
   * Overrides the method to configure the WebView selection handler. When the "read aloud" feature is disabled
   * in a custom app, this function hides the corresponding option from the menu that appears when the user selects
   * text in the WebView. This prevents the "read aloud" option from being displayed in the menu when it's disabled.
   */
  override fun configureWebViewSelectionHandler(menu: Menu?) {
    if (BuildConfig.DISABLE_READ_ALOUD) {
      menu?.findItem(org.kiwix.kiwixmobile.core.R.id.menu_speak_text)?.isVisible = false
    }
    super.configureWebViewSelectionHandler(menu)
  }

  override fun createNewTab() {
    newMainPageTab()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    permissionRequiredDialog = null
  }

  override fun onResume() {
    super.onResume()
    if (appSettingsLaunched) {
      appSettingsLaunched = false
      openObbOrZim()
    }
  }
}
