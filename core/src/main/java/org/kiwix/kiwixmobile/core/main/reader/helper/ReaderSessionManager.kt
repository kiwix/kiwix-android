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

package org.kiwix.kiwixmobile.core.main.reader.helper

import android.os.Bundle
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.files.Log
import javax.inject.Inject
import kotlin.math.max

@Suppress("LongParameterList")
class ReaderSessionManager @Inject constructor(
  private val tabsManager: TabsManager,
  private val zimFileManager: ZimFileManager,
  private val kiwixDataStore: KiwixDataStore,
  private val mainRepositoryActions: MainRepositoryActions,
  val zimReaderContainer: ZimReaderContainer,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  sealed interface RestoreSessionResult {
    data class Valid(
      val currentTab: Int,
      val currentZimFile: String?,
      val webViewHistoryList: List<WebViewHistoryItem>
    ) : RestoreSessionResult

    data object Empty : RestoreSessionResult

    data object Invalid : RestoreSessionResult
  }

  private val savingTabsMutex = Mutex()

  suspend fun saveReaderSession(onComplete: () -> Unit = {}) {
    savingTabsMutex.withLock {
      clearAndSaveLatestReaderSession(getWebViewHistoryList())
      val source = zimFileManager.zimReaderSource?.toDatabase()
      kiwixDataStore.apply {
        setCurrentZimFile(source.orEmpty())
        setCurrentTab(tabsManager.currentState().selectedIndex)
      }
      Log.d(
        TAG_KIWIX,
        "Save current zim file to preferences: $source"
      )
      onComplete.invoke()
    }
  }

  private suspend fun clearAndSaveLatestReaderSession(webViewHistoryEntityList: List<WebViewHistoryEntity>) {
    withContext(ioDispatcher) {
      // clear the previous history saved in database
      mainRepositoryActions.clearWebViewPageHistory()
      // Store new history in database.
      mainRepositoryActions.saveWebViewPageHistory(webViewHistoryEntityList)
    }
  }

  suspend fun restoreReaderSession(): RestoreSessionResult =
    runCatching {
      val webViewHistoryList = withContext(ioDispatcher) {
        // perform database operation on IO thread.
        mainRepositoryActions.loadWebViewPagesHistory()
      }
      if (webViewHistoryList.isEmpty()) {
        RestoreSessionResult.Empty
      } else {
        RestoreSessionResult.Valid(
          currentTab = safelyGetCurrentTab(),
          currentZimFile = kiwixDataStore.currentZimFile.first(),
          webViewHistoryList = webViewHistoryList
        )
      }
    }.getOrElse {
      Log.e(
        TAG_KIWIX,
        "Could not restore tabs. Original exception = ${it.printStackTrace()}"
      )
      RestoreSessionResult.Invalid
    }

  private suspend fun safelyGetCurrentTab(): Int =
    max(kiwixDataStore.currentTab.first() ?: ZERO, ZERO)

  /**
   * Restores the state of the specified KiwixWebView based on the provided WebViewHistoryItem.
   *
   * This method retrieves the back-forward list from the WebViewHistoryItem and
   * uses it to restore the web view's state. It also sets the vertical scroll position
   * of the web view to the position stored in the WebViewHistoryItem.
   *
   * If the provided WebViewHistoryItem is null, the method instead loads the main page
   * of the currently opened ZIM file. This fallback behavior is triggered, for example,
   * when opening a note in the notes screen, where the webViewHistoryList is intentionally
   * set to null to indicate that the main page of the newly opened ZIM file should be loaded.
   *
   * @param webView The KiwixWebView instance whose state is to be restored.
   * @param webViewHistoryItem The WebViewHistoryItem containing the saved state and scroll position,
   * or null if the main page should be loaded.
   */
  fun restoreTabState(webView: KiwixWebView, webViewHistoryItem: WebViewHistoryItem?) {
    webViewHistoryItem?.webViewBackForwardListBundle?.let { bundle ->
      webView.restoreState(bundle)
      webView.scrollY = webViewHistoryItem.webViewCurrentPosition
    } ?: run {
      zimReaderContainer.zimFileReader?.let {
        webView.loadUrl(redirectOrOriginal(contentUrl("${it.mainPage}")))
      }
    }
  }

  private fun redirectOrOriginal(contentUrl: String): String =
    if (zimReaderContainer.isRedirect(contentUrl)) {
      zimReaderContainer.getRedirect(contentUrl)
    } else {
      contentUrl
    }

  private fun contentUrl(articleUrl: String?): String =
    "${CONTENT_PREFIX}$articleUrl".toUri().toString()

  private suspend fun getWebViewHistoryList(): List<WebViewHistoryEntity> {
    val webViewHistoryEntityList = arrayListOf<WebViewHistoryEntity>()
    tabsManager.currentState().webViews.forEachIndexed { index, view ->
      if (view.url == null) return@forEachIndexed
      getWebViewHistoryEntity(view, index)?.let(webViewHistoryEntityList::add)
    }
    return webViewHistoryEntityList.also {
      android.util.Log.e("HISTORY", "getWebViewHistoryList: $it")
    }
  }

  /**
   * Retrieves a `WebViewHistoryEntity` from the given `KiwixWebView` instance.
   *
   * This method captures the current state of the specified web view, including its
   * scroll position and back-forward list, and creates a `WebViewHistoryEntity`
   * if the necessary conditions are met. The steps involved are as follows:
   *
   * 1. Initializes a `Bundle` to store the state of the web view.
   * 2. Calls `saveState` on the provided `webView`, which populates the bundle
   *    with the current state of the web view's back-forward list.
   * 3. Retrieves the ID of the currently loaded ZIM file from the `zimReaderContainer`.
   * 4. Checks if the ZIM ID is not null and if the web back-forward list contains any entries:
   *    - If both conditions are satisfied, it creates and returns a `WebViewHistoryEntity`
   *      containing a `WebViewHistoryItem` with the following data:
   *      - `zimId`: The ID of the current ZIM file.
   *      - `webViewIndex`: The index of the web view in the list of opened views.
   *      - `webViewPosition`: The current vertical scroll position of the web view.
   *      - `webViewBackForwardList`: The bundle containing the saved state of the
   *        web view's back-forward list.
   * 5. If the ZIM ID is null or the web back-forward list is empty, the method returns null.
   *
   * @param webView The `KiwixWebView` instance from which to retrieve the history entity.
   * @param webViewIndex The index of the web view in the list of opened web views,
   *                     used to identify the position of this web view in the history.
   * @return A `WebViewHistoryEntity` containing the state information of the web view,
   *         or null if the necessary conditions for creating the entity are not met.
   */
  private suspend fun getWebViewHistoryEntity(
    webView: KiwixWebView,
    webViewIndex: Int
  ): WebViewHistoryEntity? {
    val bundle = Bundle()
    val webBackForwardList = webView.saveState(bundle)
    val zimId = zimReaderContainer.zimFileReader?.id

    if (zimId != null && webBackForwardList != null && webBackForwardList.size > ZERO) {
      return WebViewHistoryEntity(
        WebViewHistoryItem(
          zimId = zimId,
          webViewIndex = webViewIndex,
          webViewPosition = webView.scrollY,
          webViewBackForwardList = bundle
        )
      )
    }
    return null
  }

  suspend fun clearWebViewHistory() {
    tabsManager.getCurrentWebView()?.clearHistory()
    runCatching {
      withContext(ioDispatcher) {
        mainRepositoryActions.clearWebViewPageHistory()
      }
    }.onFailure {
      it.printStackTrace()
    }
  }
}
