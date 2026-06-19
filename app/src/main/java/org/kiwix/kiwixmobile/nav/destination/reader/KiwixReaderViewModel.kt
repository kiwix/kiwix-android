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

package org.kiwix.kiwixmobile.nav.destination.reader

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel
import org.kiwix.kiwixmobile.core.main.reader.OPEN_HOME_SCREEN_DELAY
import org.kiwix.kiwixmobile.core.main.reader.RestoreOrigin
import org.kiwix.kiwixmobile.core.main.reader.helper.BookmarkManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler
import org.kiwix.kiwixmobile.ui.KiwixDestination
import javax.inject.Inject

class KiwixReaderViewModel @Inject constructor(
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
  override fun shouldShowSpellCheckedSuggestions(): Boolean = false
  override fun isBrandedApp(): Boolean = false
  override suspend fun initialize(coreMainActivity: CoreMainActivity) {
  }

  override suspend fun restoreViewStateOnValidWebViewHistory(
    webViewHistoryItemList: List<WebViewHistoryItem>,
    currentTab: Int,
    restoreOrigin: RestoreOrigin,
    onComplete: () -> Unit
  ) {
  }

  override suspend fun restoreViewStateOnInvalidWebViewHistory() {
  }

  override fun openSearch(
    searchString: String,
    isOpenedFromTabView: Boolean,
    isVoice: Boolean
  ) {
    emitEffect(
      ReaderEffect.NavigateTo(
        KiwixDestination.Search.createRoute(
          searchString = searchString,
          isOpenedFromTabView = isOpenedFromTabView,
          isVoice = isVoice
        ),
        NavOptions.Builder().setPopUpTo(KiwixDestination.Search.route, inclusive = true).build()
      )
    )
  }

  override fun invalidZimFileFound(onInvalidZimFileFound: () -> Unit) {
    // Invoke the function so that it can show toast message to user.
    runCatching { onInvalidZimFileFound.invoke() }
  }

  override fun openHomeScreen() {
    viewModelScope.launch {
      // Run safely because it is runs after 300 MS.
      delay(OPEN_HOME_SCREEN_DELAY)
      runCatching {
        if (readerWebViewManager.webViewList.isEmpty()) {
          hideTabSwitcher(false)
        }
      }
    }
  }

  /**
   * Hides the tab switcher and optionally closes the ZIM book based on the `shouldCloseZimBook` parameter.
   *
   * @param shouldCloseZimBook If `true`, the ZIM book will be closed, and the `ZimFileReader` will be set to `null`.
   * If `false`, it skips setting the `ZimFileReader` to `null`. This is particularly useful when restoring tabs,
   * as setting the `ZimFileReader` to `null` would require re-creating it, which is a resource-intensive operation,
   * especially for large ZIM files.
   *
   * Refer to the following methods for more details:
   * @See exitBook
   * @see closeTab
   * @see closeAllTabs
   */
  override suspend fun hideTabSwitcher(shouldCloseZimBook: Boolean) {
    enableLeftDrawer()
    emitEffect(ReaderEffect.ShowActivityBottomAppBar)
    if (readerWebViewManager.webViewList.isEmpty()) {
      // TODO: Remove this comment when readerMenuState is implemented in the KiwixReaderViewModel.
      // readerMenuState?.hideTabSwitcher()
      exitBook(shouldCloseZimBook)
    } else {
      // Reset the top margin of web views to 0 to remove any previously set margin
      // This ensures that the web views are displayed without any additional
      // top margin for kiwix main app.
      // setTopMarginToWebViews(0)
      updateState {
        copy(
          showBottomBar = true,
          loading = false,
          progress = ZERO
        )
      }
      // TODO: Remove this comment when readerMenuState is implemented in the KiwixReaderViewModel.
      // readerMenuState?.showWebViewOptions(urlIsValid())
      readerWebViewManager.selectTab(readerWebViewManager.currentWebViewIndex)
    }
  }
}
