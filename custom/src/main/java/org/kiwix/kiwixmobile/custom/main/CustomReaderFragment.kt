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
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R.dimen
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.extensions.browserIntent
import org.kiwix.kiwixmobile.core.extensions.getResizedDrawable
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.main.CoreReaderFragment
import org.kiwix.kiwixmobile.core.main.MainMenu
import org.kiwix.kiwixmobile.core.main.RestoreOrigin
import org.kiwix.kiwixmobile.core.page.history.adapter.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
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

  @Suppress("NestedBlockDepth")
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
        toolbar?.let(::setUpDrawerToggle)
      }
      loadPageFromNavigationArguments()
      if (BuildConfig.DISABLE_EXTERNAL_LINK) {
        // If "external links" are disabled in a custom app,
        // this sets the shared preference to not show the external link popup
        // when opening external links.
        sharedPreferenceUtil?.putPrefExternalLinkPopup(false)
      }
    }
  }

  /**
   * Overrides the method to configure the hamburger icon. When the "setting title" is disabled
   * in a custom app, this function set the app logo on hamburger.
   */
  override fun setUpDrawerToggle(toolbar: Toolbar) {
    super.setUpDrawerToggle(toolbar)
    if (BuildConfig.DISABLE_TITLE) {
      // if the title is disable then set the app logo to hamburger icon,
      // see https://github.com/kiwix/kiwix-android/issues/3528#issuecomment-1814905330
      val iconSize =
        resources.getDimensionPixelSize(dimen.hamburger_icon_size)
      requireActivity().getResizedDrawable(R.mipmap.ic_launcher, iconSize, iconSize)
        ?.let { drawable ->
          super.toolbar?.apply {
            navigationIcon = drawable
            // remove the default margin between hamburger and placeholder
            contentInsetStartWithNavigation = 0
          }
        }
    }
  }

  /**
   * Overrides the method to hide/show the placeholder from toolbar.
   * When the "setting title" is disabled/enabled in a custom app,
   * this function set the visibility of placeholder in toolbar when showing the tabs.
   */
  override fun setTabSwitcherVisibility(visibility: Int) {
    if (BuildConfig.DISABLE_TITLE) {
      // If custom apps are configured to show the placeholder,
      // and if tabs are visible, hide the placeholder.
      // If tabs are hidden, show the placeholder.
      updateToolbarSearchPlaceholderVisibility(if (visibility == VISIBLE) GONE else VISIBLE)
    } else {
      // Permanently hide the placeholder if the custom app is not configured to show it.
      updateToolbarSearchPlaceholderVisibility(GONE)
    }
    super.setTabSwitcherVisibility(visibility)
  }

  private fun loadPageFromNavigationArguments() {
    val args = CustomReaderFragmentArgs.fromBundle(requireArguments())
    if (args.pageUrl.isNotEmpty()) {
      loadUrlWithCurrentWebview(args.pageUrl)
      // Setup bookmark for current book
      // See https://github.com/kiwix/kiwix-android/issues/3541
      zimReaderContainer?.zimFileReader?.let(::setUpBookmarks)
    } else {
      openObbOrZim(true)
    }
    requireArguments().clear()
  }

  /**
   * Restores the view state when the attempt to read web view history from the room database fails
   * due to the absence of any history records. In this case, it navigates to the homepage of the
   * ZIM file, as custom apps are expected to have the ZIM file readily available.
   */
  override fun restoreViewStateOnInvalidWebViewHistory() {
    openHomeScreen()
  }

  /**
   * Restores the view state when the webViewHistory data is valid.
   * This method restores the tabs with webView pages history.
   */
  override fun restoreViewStateOnValidWebViewHistory(
    webViewHistoryItemList: List<WebViewHistoryItem>,
    currentTab: Int,
    // Unused in custom apps as there is only one ZIM file that is already set.
    restoreOrigin: RestoreOrigin,
    onComplete: () -> Unit
  ) {
    restoreTabs(webViewHistoryItemList, currentTab, onComplete)
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

  /**
   * Opens a ZIM file or an OBB file based on the validation of available files.
   *
   * This method uses the `customFileValidator` to check for the presence of required files.
   * Depending on the validation results, it performs the following actions:
   *
   * - If a valid ZIM file is found:
   *   - It opens the ZIM file and creates a `ZimReaderSource` for it.
   *   - Saves the book information in the database to be displayed in the `ZimHostFragment`.
   *   - Manages the external launch and restores the view state if specified.
   *
   * - If both ZIM and OBB files are found:
   *   - The ZIM file is deleted, and the OBB file is opened instead.
   *   - Manages the external launch and restores the view state if specified.
   *
   * If no valid files are found and the app is not in test mode, the user is navigated to
   * the `customDownloadFragment` to facilitate downloading the required files.
   *
   * @param shouldManageExternalLaunch Indicates whether to manage external launch and
   *                                   restore the view state after opening the file. Default is false.
   */
  private fun openObbOrZim(shouldManageExternalLaunch: Boolean = false) {
    customFileValidator.validate(
      onFilesFound = {
        coreReaderLifeCycleScope?.launch {
          when (it) {
            is ValidationState.HasFile -> {
              openZimFile(
                ZimReaderSource(
                  file = it.file,
                  null,
                  it.assetFileDescriptorList
                ),
                true,
                shouldManageExternalLaunch
              )
              // Save book in the database to display it in `ZimHostFragment`.
              zimReaderContainer?.zimFileReader?.let { zimFileReader ->
                // Check if the file is not null. If the file is null,
                // it means we have created zimFileReader with a fileDescriptor,
                // so we create a demo file to save it in the database for display on the `ZimHostFragment`.
                val file = it.file ?: createDemoFile()
                val bookOnDisk = BookOnDisk(zimFileReader)
                repositoryActions?.saveBook(bookOnDisk)
              }
              if (shouldManageExternalLaunch) {
                // Open the previous loaded pages after ZIM file loads.
                manageExternalLaunchAndRestoringViewState()
              }
            }

            is ValidationState.HasBothFiles -> {
              it.zimFile.delete()
              openZimFile(ZimReaderSource(it.obbFile), true, shouldManageExternalLaunch)
              if (shouldManageExternalLaunch) {
                // Open the previous loaded pages after ZIM file loads.
                manageExternalLaunchAndRestoringViewState()
              }
            }

            else -> {}
          }
        }
      },
      onNoFilesFound = {
        if (sharedPreferenceUtil?.prefIsTest == false) {
          findNavController().navigate(R.id.customDownloadFragment)
        }
      }
    )
  }

  private suspend fun createDemoFile() =
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
        AppCompatDelegate.setApplicationLocales(
          LocaleListCompat.forLanguageTags(BuildConfig.ENFORCED_LANG)
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
        BuildConfig.DISABLE_TABS,
        BuildConfig.DISABLE_TITLE
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

  /**
   * Overrides the method to configure the title of toolbar. When the "setting title" is disabled
   * in a custom app, this function set the empty toolbar title.
   */
  override fun updateTitle() {
    if (BuildConfig.DISABLE_TITLE) {
      // Set an empty title for the toolbar because we are handling the toolbar click on behalf of this title.
      // Since we have increased the zone for triggering search suggestions (see https://github.com/kiwix/kiwix-android/pull/3566),
      // we need to set this title for handling the toolbar click,
      // even if it is empty. If we do not set up this title,
      // the search screen will open if the user clicks on the toolbar from the tabs screen.
      actionBar?.title = " "
      updateToolbarSearchPlaceholderVisibility(VISIBLE)
    } else {
      updateToolbarSearchPlaceholderVisibility(GONE)
      super.updateTitle()
    }
  }

  private fun updateToolbarSearchPlaceholderVisibility(visibility: Int) {
    toolbarWithSearchPlaceholder?.visibility = visibility
  }

  override fun createNewTab() {
    newMainPageTab()
  }

  /**
   * Overrides the method to show the donation popup. When the "Support url" is disabled
   * in a custom app, this function stop to show the donationPopup.
   */
  override fun showDonationLayout() {
    if (BuildConfig.SUPPORT_URL.isNotEmpty()) {
      super.showDonationLayout()
    }
  }

  override fun openKiwixSupportUrl() {
    if (BuildConfig.SUPPORT_URL.isNotEmpty()) {
      externalLinkOpener?.openExternalUrl(BuildConfig.SUPPORT_URL.toUri().browserIntent(), false)
    }
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
