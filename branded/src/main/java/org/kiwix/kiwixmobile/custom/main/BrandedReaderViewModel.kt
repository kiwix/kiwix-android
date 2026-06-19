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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion
import androidx.compose.ui.graphics.Color.Companion.Unspecified
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.getObservableNavigationResult
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.PAGE_URL_KEY
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel
import org.kiwix.kiwixmobile.core.main.reader.RestoreOrigin
import org.kiwix.kiwixmobile.core.main.reader.helper.BookmarkManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler
import org.kiwix.kiwixmobile.custom.BuildConfig
import javax.inject.Inject

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
  bookmarkManager: BookmarkManager
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
  bookmarkManager
) {
  override suspend fun initialize(coreMainActivity: CoreMainActivity) {
    enableLeftDrawer()
    loadPageFromNavigationArguments(coreMainActivity)
    if (BuildConfig.DISABLE_EXTERNAL_LINK) {
      // If "external links" are disabled in a custom app,
      // this sets the shared preference to not show the external link popup
      // when opening external links.
      kiwixDataStore.setExternalLinkPopup(false)
    }
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
    emitEffect(ReaderEffect.ConsumeObservable<String>(PAGE_URL_KEY))
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

  /**
   * Restores the view state when the webViewHistory data is valid.
   * This method restores the tabs with webView pages history.
   */
  override suspend fun restoreViewStateOnValidWebViewHistory(
    webViewHistoryItemList: List<WebViewHistoryItem>,
    currentTab: Int,
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
}
