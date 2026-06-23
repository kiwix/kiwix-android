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
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.getObservableNavigationResult
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.PAGE_URL_KEY
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel
import org.kiwix.kiwixmobile.core.main.reader.OPEN_HOME_SCREEN_DELAY
import org.kiwix.kiwixmobile.core.main.reader.RestoreOrigin
import org.kiwix.kiwixmobile.core.main.reader.RestoreOrigin.FromExternalLaunch
import org.kiwix.kiwixmobile.core.main.reader.RestoreOrigin.FromSearchScreen
import org.kiwix.kiwixmobile.core.main.reader.SEARCH_ITEM_TITLE_KEY
import org.kiwix.kiwixmobile.core.main.reader.helper.BookmarkManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderHistoryManager
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.ReaderIntentManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderSessionManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource.Companion.fromDatabaseValue
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.ui.KiwixDestination
import java.io.File
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
  bookmarkManager: BookmarkManager,
  readerHistoryManager: ReaderHistoryManager,
  readerSessionManager: ReaderSessionManager,
  readerIntentManager: ReaderIntentManager
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
  readerIntentManager
) {
  override fun shouldShowSpellCheckedSuggestions(): Boolean = false
  override fun isBrandedApp(): Boolean = false
  override suspend fun initialize(coreMainActivity: CoreMainActivity) {
    enableLeftDrawer()
    openPageInBookFromNavigationArguments(coreMainActivity)
  }

  @Suppress("MagicNumber")
  private suspend fun openPageInBookFromNavigationArguments(coreMainActivity: CoreMainActivity) {
    showProgressBarWithProgress(30)
    val zimFileUri = getNavigationResult(ZIM_FILE_URI_KEY, coreMainActivity)
    val pageUrl = getNavigationResult(PAGE_URL_KEY, coreMainActivity)
    val searchItemTitle = getNavigationResult(SEARCH_ITEM_TITLE_KEY, coreMainActivity)
    if (pageUrl.isNotEmpty()) {
      if (zimFileUri.isNotEmpty()) {
        tryOpeningZimFile(zimFileUri)
      } else {
        // Set up bookmarks for the current book when opening bookmarks from the Bookmark screen.
        // This is necessary because we are not opening the ZIM file again; the bookmark is
        // inside the currently opened book. Bookmarks are set up when opening the ZIM file.
        // See https://github.com/kiwix/kiwix-android/issues/3541
        zimReaderContainer.zimFileReader?.let(::observeBookmarks)
      }
      hideProgressBar()
      loadUrlWithCurrentWebview(pageUrl)
    } else {
      if (zimFileUri.isNotEmpty()) {
        tryOpeningZimFile(zimFileUri)
      } else {
        isWebViewHistoryRestoring = true
        val restoreOrigin =
          if (searchItemTitle.isNotEmpty()) FromSearchScreen else FromExternalLaunch
        manageExternalLaunchAndRestoringViewState(restoreOrigin)
      }
    }
    // Consume the argument.
    emitEffect(
      ReaderEffect.ConsumeSavedStateHandle(
        listOf(
          ZIM_FILE_URI_KEY to String::class.java,
          PAGE_URL_KEY to String::class.java,
          SEARCH_ITEM_TITLE_KEY to String::class.java
        )
      )
    )
  }

  private fun getNavigationResult(key: String, coreMainActivity: CoreMainActivity) =
    coreMainActivity.getObservableNavigationResult<String>(key)?.value.orEmpty()

  private suspend fun tryOpeningZimFile(zimFileUri: String) {
    // Stop any ongoing WebView loading and clear the WebView list
    // before setting a new ZIM file to the reader. This helps prevent native crashes.
    // The WebView's `shouldInterceptRequest` method continues to be invoked until the WebView is
    // fully destroyed, which can cause a native crash. This happens because a new ZIM file is set
    // in the reader while the WebView is still trying to access content from the old archive.
    stopOngoingLoadingAndClearWebViewList()
    // Close the previously opened book in the reader before opening a new ZIM file
    // to avoid native crashes due to "null pointer dereference." These crashes can occur
    // when setting a new ZIM file in the archive while the previous one is being disposed of.
    // Since the WebView may still asynchronously request data from the disposed archive,
    // we close the previous book before opening a new ZIM file in the archive.
    closeZimBook()
    // Update the reader screen title to prevent showing the previously set title
    // when creating the new archive object.
    updateTitle()
    val filePath = FileUtils.getLocalFilePathByUri(context.applicationContext, zimFileUri.toUri())
    if (filePath == null || !File(filePath).isFileExist()) {
      // Close the previously opened book in the reader. Since this file is not found,
      // it will not be set in the zimFileReader. The previously opened ZIM file
      // will be saved when we move between fragments. If we return to the reader again,
      // it will attempt to open the last opened ZIM file with the last loaded URL,
      // which is inside the non-existing ZIM file. This leads to unexpected behavior.
      exitBook()
      emitEffect(ReaderEffect.ShowToast(context.getString(string.error_file_not_found, zimFileUri)))
      return
    }
    val zimReaderSource = ZimReaderSource(File(filePath))
    openZimFile(zimReaderSource)
  }

  override suspend fun restoreViewStateOnValidWebViewHistory(
    webViewHistoryItemList: List<WebViewHistoryItem>,
    currentTab: Int,
    currentZimFile: String?,
    restoreOrigin: RestoreOrigin,
    onComplete: () -> Unit
  ) {
    when (restoreOrigin) {
      FromExternalLaunch -> {
        val zimReaderSource = fromDatabaseValue(currentZimFile)
        if (zimReaderSource?.canOpenInLibkiwix() == true) {
          if (zimReaderContainer.zimReaderSource == null) {
            openZimFile(zimReaderSource)
            Log.d(
              TAG_KIWIX,
              "Kiwix normal start, Opened last used zimFile: -> ${zimReaderSource.toDatabase()}"
            )
          } else {
            zimReaderContainer.zimFileReader?.let(::observeBookmarks)
          }
          restoreTabs(webViewHistoryItemList, currentTab, onComplete)
        } else {
          emitEffect(
            ReaderEffect.ShowSnackbar(context.getString(string.zim_not_opened))
          )
          exitBook() // hide the options for zim file to avoid unexpected UI behavior
        }
      }

      FromSearchScreen -> {
        restoreTabs(webViewHistoryItemList, currentTab, onComplete)
      }
    }
  }

  override suspend fun restoreViewStateOnInvalidWebViewHistory() {
    Log.d(TAG_KIWIX, "Kiwix normal start, no zimFile loaded last time  -> display home page")
    exitBook()
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
      readerMenuState?.hideTabSwitcher()
      exitBook(shouldCloseZimBook)
    } else {
      updateState {
        copy(
          showBottomBar = true,
          loading = false,
          progress = ZERO
        )
      }
      readerMenuState?.showWebViewOptions(urlIsValid())
      readerWebViewManager.selectTab(readerWebViewManager.currentWebViewIndex)
    }
  }
}
