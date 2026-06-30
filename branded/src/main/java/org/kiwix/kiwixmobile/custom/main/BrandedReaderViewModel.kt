/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R.drawable
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.getObservableNavigationResult
import org.kiwix.kiwixmobile.core.extensions.browserIntent
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.extensions.runSafelyInLifecycleScope
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.PAGE_URL_KEY
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel
import org.kiwix.kiwixmobile.core.main.reader.FindInPageManager
import org.kiwix.kiwixmobile.core.main.reader.ReaderMenuState
import org.kiwix.kiwixmobile.core.main.reader.RestoreOrigin
import org.kiwix.kiwixmobile.core.main.reader.helper.BookmarkManager
import org.kiwix.kiwixmobile.core.main.reader.helper.PendingSearchItemManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderArticleManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderHistoryManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderSessionManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.ReaderIntentManager
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.DonationDialogHandler
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.getDemoFilePathForBrandedApp
import org.kiwix.kiwixmobile.custom.BuildConfig
import org.kiwix.kiwixmobile.custom.R
import org.kiwix.libkiwix.Book
import java.io.File
import java.util.Locale
import javax.inject.Inject

@Suppress("LongParameterList")
class BrandedReaderViewModel @Inject constructor(
  context: Application,
  kiwixDataStore: KiwixDataStore,
  externalLinkOpener: ExternalLinkOpener,
  unsupportedMimeTypeHandler: UnsupportedMimeTypeHandler,
  readerWebViewManager: ReaderWebViewManager,
  alertDialogShower: AlertDialogShower,
  zimReaderContainer: ZimReaderContainer,
  zimFileManager: ZimFileManager,
  kiwixPermissionChecker: KiwixPermissionChecker,
  repositoryActions: MainRepositoryActions,
  bookmarkManager: BookmarkManager,
  readerHistoryManager: ReaderHistoryManager,
  private val brandedFileValidator: BrandedFileValidator,
  readerSessionManager: ReaderSessionManager,
  readerIntentManager: ReaderIntentManager,
  pendingSearchItemManager: PendingSearchItemManager,
  readerArticleManager: ReaderArticleManager,
  readAloudManager: ReadAloudManager,
  donationDialogHandler: DonationDialogHandler,
  findInPageManager: FindInPageManager
) : CoreReaderViewModel(
    context,
    kiwixDataStore,
    externalLinkOpener,
    unsupportedMimeTypeHandler,
    readerWebViewManager,
    alertDialogShower,
    zimReaderContainer,
    zimFileManager,
    kiwixPermissionChecker,
    repositoryActions,
    bookmarkManager,
    readerHistoryManager,
    readerSessionManager,
    readerIntentManager,
    pendingSearchItemManager,
    readerArticleManager,
    readAloudManager,
    donationDialogHandler,
    findInPageManager
  ) {
  override suspend fun initialize(
    coreMainActivity: CoreMainActivity,
    alertDialogShower: AlertDialogShower
  ) {
    if (enforcedLanguage(coreMainActivity)) {
      return
    }
    externalLinkOpener.setAlertDialogShower(alertDialogShower)
    val appName = kiwixDataStore.appName.first()
    updateState { copy(isTocButtonEnable = !BuildConfig.DISABLE_SIDEBAR, appName = appName) }
    enableLeftDrawer()
    loadPageFromNavigationArguments(coreMainActivity)
    if (BuildConfig.DISABLE_EXTERNAL_LINK) {
      // If "external links" are disabled in a custom app,
      // this sets the shared preference to not show the external link popup
      // when opening external links.
      kiwixDataStore.setExternalLinkPopup(false)
    }
  }

  override fun openBookmarkScreen() {
    emitEffect(ReaderEffect.NavigateTo(CustomDestination.Bookmarks.route))
  }

  private suspend fun loadPageFromNavigationArguments(coreMainActivity: CoreMainActivity) {
    val pageUrl =
      coreMainActivity.getObservableNavigationResult<String>(PAGE_URL_KEY)?.value.orEmpty()
    if (pageUrl.isNotEmpty()) {
      loadUrlWithCurrentWebview(pageUrl)
      // Setup bookmark for current book
      // See https://github.com/kiwix/kiwix-android/issues/3541
      zimReaderContainer.zimFileReader?.let(::observeBookmarks)
    } else {
      isWebViewHistoryRestoring = true
      if (isZimFileAlreadyOpenedInReader()) {
        manageExternalLaunchAndRestoringViewState()
      } else {
        openObbOrZim(true)
      }
    }
    emitEffect(ReaderEffect.ConsumeSavedStateHandle(listOf(PAGE_URL_KEY)))
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
  private suspend fun openObbOrZim(shouldManageExternalLaunch: Boolean = false) {
    brandedFileValidator.validate(
      onFilesFound = {
        when (it) {
          is ValidationState.HasFile -> {
            viewModelScope.runSafelyInLifecycleScope(Dispatchers.Main.immediate) {
              openZimFile(
                ZimReaderSource(
                  file = it.file,
                  null,
                  it.assetFileDescriptorList
                )
              )
              if (shouldManageExternalLaunch) {
                // Open the previous loaded pages after ZIM file loads.
                manageExternalLaunchAndRestoringViewState()
              }
              saveBookToLibrary(it.file)
            }
          }

          is ValidationState.HasBothFiles -> {
            it.zimFile.delete()
            viewModelScope.runSafelyInLifecycleScope(Dispatchers.Main.immediate) {
              openZimFile(ZimReaderSource(it.obbFile))
              if (shouldManageExternalLaunch) {
                // Open the previous loaded pages after ZIM file loads.
                manageExternalLaunchAndRestoringViewState()
              }
              saveBookToLibrary(it.obbFile)
            }
          }

          else -> {}
        }
      },
      onNoFilesFound = {
        if (!kiwixDataStore.prefIsTest.first()) {
          openDownloadScreen()
        }
      }
    )
  }

  private suspend fun enforcedLanguage(coreMainActivity: CoreMainActivity): Boolean {
    // TODO : migrate this method with compose lifeCycle.
    // This runs in branded apps when there is an enforced language set in the build config.
    val currentLocaleCode = Locale.getDefault().toString()
    if (BuildConfig.ENFORCED_LANG.isNotEmpty() && BuildConfig.ENFORCED_LANG != currentLocaleCode) {
      kiwixDataStore.let { kiwixDataStore ->
        AppCompatDelegate.setApplicationLocales(
          LocaleListCompat.forLanguageTags(BuildConfig.ENFORCED_LANG)
        )
        kiwixDataStore.setPrefLanguage(BuildConfig.ENFORCED_LANG)
      }
      coreMainActivity.recreate()
      return true
    }
    return false
  }

  @Suppress("TooGenericExceptionCaught")
  private suspend fun saveBookToLibrary(zimFile: File?) {
    viewModelScope.runSafelyInLifecycleScope {
      zimReaderContainer.zimFileReader?.let { zimFileReader ->
        try {
          // Save book in the database to display it in `ZimHostFragment`.
          // Check if the file is not null. If the file is null,
          // it means we have created zimFileReader with a fileDescriptor,
          // so we create a demo file to save it in the database for display on the `ZimHostFragment`.
          val file = zimFile ?: createDemoFile()
          // Wrapped in try-catch because if the reader scope is cancelled (for example,
          // when the user navigates to another screen), the scope and related variables
          // may be cleared from the Fragment. Accessing them would then throw an error.
          // The `Book.update()` method is not a suspend function, and coroutine
          // cancellation is only checked at suspension points. As a result, this
          // block may still execute even after the lifecycle scope has been cancelled.
          val book = Book().apply { update(zimFileReader.jniKiwixReader) }
          repositoryActions.saveBook(book)
        } catch (e: Exception) {
          Log.e(TAG_KIWIX, "Could not save book in library. Original exception = $e")
        }
      }
    }
  }

  private suspend fun createDemoFile() {
    runCatching {
      File(getDemoFilePathForBrandedApp(context)).also {
        if (!it.isFileExist()) it.createNewFile()
      }
    }
  }

  override fun shouldShowSpellCheckedSuggestions(): Boolean =
    BuildConfig.SHOW_SEARCH_SUGGESTIONS_SPELLCHECKED

  override fun isBrandedApp(): Boolean = true

  override fun invalidZimFileFound(onInvalidZimFileFound: () -> Unit) {
    openDownloadScreen()
  }

  private fun openDownloadScreen() {
    viewModelScope.launch {
      delay(OPENING_DOWNLOAD_SCREEN_DELAY)
      val navOptions = NavOptions.Builder()
        .setPopUpTo(CustomDestination.Reader.route, true)
        .build()
      emitEffect(ReaderEffect.NavigateTo(CustomDestination.Downloads.route, navOptions))
    }
  }

  /**
   * Overrides the method to configure the title of toolbar. When the "setting title" is disabled
   * in a custom app, this function set the empty toolbar title.
   */
  override suspend fun updateTitle() {
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
    updateState {
      copy(searchPlaceHolderItemForBrandedApps = show)
    }
  }

  override fun openSearch(searchString: String, isOpenedFromTabView: Boolean, isVoice: Boolean) {
    emitEffect(
      ReaderEffect.NavigateTo(
        CustomDestination.Search.createRoute(
          searchString = searchString,
          isOpenedFromTabView = isOpenedFromTabView,
          isVoice = isVoice
        ),
        NavOptions.Builder().setPopUpTo(CustomDestination.Search.route, inclusive = true).build()
      )
    )
  }

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
    uiState.value.showTabSwitcher -> {
      IconItem.Drawable(drawable.ic_round_add_white_36dp)
    }

    BuildConfig.DISABLE_TITLE -> {
      // if the title is disable then set the app logo to hamburger icon,
      // see https://github.com/kiwix/kiwix-android/issues/3528#issuecomment-1814905330
      IconItem.MipmapImage(R.mipmap.ic_launcher)
    }

    else -> IconItem.Vector(Icons.Filled.Menu)
  }

  /**
   * Restores the view state when the webViewHistory data is valid.
   * This method restores the tabs with webView pages history.
   */
  override suspend fun restoreViewStateOnValidWebViewHistory(
    webViewHistoryItemList: List<WebViewHistoryItem>,
    currentTab: Int,
    currentZimFile: String?,
    // Unused in custom apps as there is only one ZIM file that is already set.
    restoreOrigin: RestoreOrigin,
    onComplete: () -> Unit
  ) {
    restoreTabs(webViewHistoryItemList, currentTab, onComplete)
  }

  /**
   * Restores the view state when the attempt to read web view history from the room database fails
   * due to the absence of any history records. In this case, it navigates to the homepage of the
   * ZIM file, as custom apps are expected to have the ZIM file readily available.
   */
  override suspend fun restoreViewStateOnInvalidWebViewHistory() {
    openHomeScreen()
  }

  override fun showNoBookOpenViews() {
    updateState { copy(showNoBookOpenInReader = false) }
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
   * Checks whether a ZIM file is currently opened and active in the reader.
   *
   * This method verifies these conditions:
   * 1. A ZIM file reader instance is available.
   * 2. The underlying ZIM file source still exists in storage.
   * 3. The currently opened ZIM file can open with libkiwix(Validates previous opened ZIM file).
   * 4. The currently opened archive is not null.
   *
   * @return `true` if a valid and accessible ZIM file is currently opened in the reader;
   *         otherwise `false`.
   */
  private suspend fun isZimFileAlreadyOpenedInReader(): Boolean =
    zimReaderContainer.zimFileReader != null &&
      zimReaderContainer.zimReaderSource?.exists() == true &&
      zimReaderContainer.zimReaderSource?.canOpenInLibkiwix() == true &&
      zimReaderContainer.zimFileReader?.jniKiwixReader != null

  /**
   * Overrides the method to create the main menu for the app. The branded app can be configured to disable
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
      disableSearch = BuildConfig.DISABLE_TITLE,
      // Custom apps usually don't need homescreen shortcuts
      isPinShortcutSupported = false
    )

  override fun openKiwixSupportUrl() {
    if (BuildConfig.SUPPORT_URL.isNotEmpty()) {
      externalLinkOpener.openExternalLinkWithDialog(
        BuildConfig.SUPPORT_URL.toUri().browserIntent(),
        context.getString(string.support_donation_platform)
      )
    }
  }
}
