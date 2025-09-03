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
import android.view.View
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.navigation.NavOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.consumeObservable
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.getObservableNavigationResult
import org.kiwix.kiwixmobile.core.extensions.browserIntent
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.PAGE_URL_KEY
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderFragment
import org.kiwix.kiwixmobile.core.main.reader.ReaderMenuState
import org.kiwix.kiwixmobile.core.main.reader.RestoreOrigin
import org.kiwix.kiwixmobile.core.page.history.adapter.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.getDemoFilePathForCustomApp
import org.kiwix.kiwixmobile.custom.BuildConfig
import org.kiwix.kiwixmobile.custom.R
import org.kiwix.kiwixmobile.custom.customActivityComponent
import org.kiwix.libkiwix.Book
import java.io.File
import java.util.Locale
import javax.inject.Inject

const val OPENING_DOWNLOAD_SCREEN_DELAY = 300L

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
      enableLeftDrawer()
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
   * Returns the TOC (Table of Contents) button's enabled state and click action.
   *
   * In this custom app variant, the TOC button is disabled if [BuildConfig.DISABLE_SIDEBAR] is `true`.
   * This is typically used when the sidebar functionality is intentionally turned off.
   *
   * @return A [Pair] containing:
   *  - [Boolean]: `true` if the TOC button should be enabled (i.e., sidebar is allowed),
   *               `false` if it should be disabled (i.e., [DISABLE_SIDEBAR] is `true`).
   *  - [() -> Unit]: Action to execute when the button is clicked. This will only be invoked if enabled.
   */
  override fun getTocButtonStateAndAction(): Pair<Boolean, () -> Unit> =
    !BuildConfig.DISABLE_SIDEBAR to { openToc() }

  /**
   * Returns the tint color for the navigation icon.
   *
   * If the custom app is configured to show the app icon in place of the hamburger icon
   * (i.e., [BuildConfig.DISABLE_TITLE] is true), the tint is set to [Color.Unspecified] to preserve
   * the original colors of the image.
   *
   * Otherwise, [White] is used as the default tint, which is suitable for vector icons.
   */
  override fun navigationIconTint(): Color =
    if (BuildConfig.DISABLE_TITLE) {
      Color.Unspecified
    } else {
      White
    }

  override fun navigationIcon(): IconItem = when {
    readerMenuState?.isInTabSwitcher == true -> {
      IconItem.Drawable(org.kiwix.kiwixmobile.core.R.drawable.ic_round_add_white_36dp)
    }

    BuildConfig.DISABLE_TITLE -> {
      // if the title is disable then set the app logo to hamburger icon,
      // see https://github.com/kiwix/kiwix-android/issues/3528#issuecomment-1814905330
      IconItem.MipmapImage(R.mipmap.ic_launcher)
    }

    else -> IconItem.Vector(Icons.Filled.Menu)
  }

  /**
   * Overrides the method to hide/show the placeholder from toolbar.
   * When the "setting title" is disabled/enabled in a custom app,
   * this function set the visibility of placeholder in toolbar when showing the tabs.
   */
  override fun showSearchPlaceHolderInToolbar(isTabSwitcherShowing: Boolean) {
    if (BuildConfig.DISABLE_TITLE) {
      // If custom apps are configured to show the placeholder,
      // and if tabs are visible, hide the placeholder.
      // If tabs are hidden, show the placeholder.
      updateToolbarSearchPlaceholderVisibility(!isTabSwitcherShowing)
    } else {
      // Permanently hide the placeholder if the custom app is not configured to show it.
      updateToolbarSearchPlaceholderVisibility(false)
    }
  }

  /**
   * Handles clicks on the navigation icon in custom apps.
   * - If the tab switcher is active, triggers the home menu action.
   * - Otherwise, toggles the navigation drawer: closes it if open; opens it only if the sidebar is enabled.
   *
   * This override customizes the default behavior by preventing the drawer from opening
   * when the sidebar is disabled in the app configuration.
   */
  override fun navigationIconClick() {
    if (readerMenuState?.isInTabSwitcher == true) {
      onHomeMenuClicked()
      return
    }

    val activity = activity as CoreMainActivity
    if (activity.navigationDrawerIsOpen()) {
      activity.closeNavigationDrawer()
    } else if (!BuildConfig.DISABLE_SIDEBAR) {
      activity.openNavigationDrawer()
    }
  }

  private fun loadPageFromNavigationArguments() {
    val customMainActivity = activity as? CustomMainActivity
    val pageUrl =
      customMainActivity?.getObservableNavigationResult<String>(PAGE_URL_KEY)?.value.orEmpty()
    if (pageUrl.isNotEmpty()) {
      loadUrlWithCurrentWebview(pageUrl)
      // Setup bookmark for current book
      // See https://github.com/kiwix/kiwix-android/issues/3541
      zimReaderContainer?.zimFileReader?.let(::setUpBookmarks)
    } else {
      coreReaderLifeCycleScope?.launch {
        openObbOrZim(true)
      }
    }
    customMainActivity?.consumeObservable<String>(PAGE_URL_KEY)
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
   * Enables or disables the sidebar in a custom app based on the configuration.
   * If the app is configured to disable the sidebar, this method disables it;
   * otherwise, it enables the sidebar.
   */
  override fun enableLeftDrawer() {
    if (BuildConfig.DISABLE_SIDEBAR) {
      (requireActivity() as CoreMainActivity).disableLeftDrawer()
    } else {
      super.enableLeftDrawer()
    }
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
  @Suppress("InjectDispatcher")
  private suspend fun openObbOrZim(shouldManageExternalLaunch: Boolean = false) {
    customFileValidator.validate(
      onFilesFound = {
        when (it) {
          is ValidationState.HasFile -> {
            withContext(Dispatchers.Main) {
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
                val book = Book().apply { update(zimFileReader.jniKiwixReader) }
                repositoryActions?.saveBook(book)
              }
              if (shouldManageExternalLaunch) {
                // Open the previous loaded pages after ZIM file loads.
                manageExternalLaunchAndRestoringViewState()
              }
            }
          }

          is ValidationState.HasBothFiles -> {
            it.zimFile.delete()
            withContext(Dispatchers.Main) {
              openZimFile(ZimReaderSource(it.obbFile), true, shouldManageExternalLaunch)
              if (shouldManageExternalLaunch) {
                // Open the previous loaded pages after ZIM file loads.
                manageExternalLaunchAndRestoringViewState()
              }
            }
          }

          else -> {}
        }
      },
      onNoFilesFound = {
        if (sharedPreferenceUtil?.prefIsTest == false) {
          delay(OPENING_DOWNLOAD_SCREEN_DELAY)
          withContext(Dispatchers.Main) {
            val navOptions = NavOptions.Builder()
              .setPopUpTo(CustomDestination.Reader.route, true)
              .build()
            (activity as? CoreMainActivity)?.navigate(
              CustomDestination.Downloads.route,
              navOptions
            )
          }
        }
      }
    )
  }

  private suspend fun createDemoFile() =
    File(getDemoFilePathForCustomApp(requireActivity())).also {
      if (!it.isFileExist()) it.createNewFile()
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
   * Overrides the method to create the main menu for the app. The custom app can be configured to disable
   * features like "read aloud" and "tabs," and this method dynamically generates the menu based on the
   * provided configuration. It takes into account whether read aloud and tabs are enabled or disabled
   * and creates the menu accordingly.
   */
  override fun createMainMenu(): ReaderMenuState =
    ReaderMenuState(
      this,
      isUrlValidInitially = urlIsValid(),
      disableReadAloud = BuildConfig.DISABLE_READ_ALOUD,
      disableTabs = BuildConfig.DISABLE_TABS,
      disableSearch = BuildConfig.DISABLE_TITLE
    )

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
      // Since we have increased the zone for triggering search suggestions (see https://github.com/kiwix/kiwix-android/pull/3566),
      // we need to set this title for handling the toolbar click,
      // even if it is empty. If we do not set up this title,
      // the search screen will open if the user clicks on the toolbar from the tabs screen.
      updateToolbarSearchPlaceholderVisibility(true)
    } else {
      updateToolbarSearchPlaceholderVisibility(false)
      super.updateTitle()
    }
  }

  private fun updateToolbarSearchPlaceholderVisibility(show: Boolean) {
    readerScreenState.update {
      copy(
        searchPlaceHolderItemForCustomApps = searchPlaceHolderItemForCustomApps.copy(first = show)
      )
    }
  }

  override fun createNewTab() {
    newMainPageTab()
  }

  override fun showNoBookOpenViews() {
    readerScreenState.update { copy(isNoBookOpenInReader = false) }
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
      coreReaderLifeCycleScope?.launch {
        openObbOrZim(true)
      }
    }
  }
}
