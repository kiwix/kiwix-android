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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.di.MainDispatcher
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.addContentPrefix
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
  @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  @param:MainDispatcher private val mainDispatcher: MainCoroutineDispatcher
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

  suspend fun <T> withTabsMutationLock(block: suspend () -> T): T =
    savingTabsMutex.withLock { block() }

  suspend fun saveReaderSession(onComplete: () -> Unit = {}) {
    withTabsMutationLock {
      val tabsState = withContext(mainDispatcher) { tabsManager.currentState() }
      val savedTabs = getWebViewHistoryList(tabsState)
      clearAndSaveLatestReaderSession(savedTabs.map { it.second })
      lastSavedTabs = savedTabs.map { it.first }
      val source = zimFileManager.zimReaderSource?.toDatabase()
      kiwixDataStore.apply {
        setCurrentZimFile(source.orEmpty())
        // Read the selected tab again: the user may have switched tabs while the
        // history was being written.
        val selectedTab = withContext(mainDispatcher) { tabsManager.currentState().currentWebView }
        setCurrentTab(
          savedTabIndexOf(selectedTab)
            ?: fallbackTabIndex(tabsState, savedTabs.map { it.first })
        )
      }
      Log.d(
        TAG_KIWIX,
        "Save current zim file to preferences: $source"
      )
      onComplete.invoke()
    }
  }

  /**
   * The tabs stored by the last [saveReaderSession], in the order they are saved.
   * The saved tab index must point into this list, because it is the list that will
   * be restored.
   */
  @Volatile
  private var lastSavedTabs: List<KiwixWebView> = emptyList()

  /**
   * Saves only the selected tab, for example when the user switches tabs.
   *
   * Switching tabs does not load a page, so [saveReaderSession] is not called for it.
   * Without this, the app reopens the tab that last loaded a page instead of the tab
   * the user was reading.
   *
   * If the selected tab is not in the saved history yet (for example a new tab that is
   * still loading), nothing is saved here. The next [saveReaderSession] saves it.
   *
   * This does not take [savingTabsMutex]: it only writes one value, so it does not need
   * the lock.
   */
  suspend fun saveCurrentTab() {
    val selectedTab = withContext(mainDispatcher) { tabsManager.currentState().currentWebView }
    savedTabIndexOf(selectedTab)?.let { kiwixDataStore.setCurrentTab(it) }
  }

  private fun savedTabIndexOf(webView: KiwixWebView?): Int? =
    webView?.let { tab -> lastSavedTabs.indexOfFirst { it === tab }.takeIf { it >= ZERO } }

  /**
   * Used when the selected tab itself is not saved (for example it has no page yet).
   * Picks the nearest saved tab before it.
   */
  private fun fallbackTabIndex(
    tabsState: TabsManager.TabsState,
    savedTabs: List<KiwixWebView>
  ): Int {
    val tabsBefore = tabsState.webViews.take(tabsState.selectedIndex)
    val savedTabsBefore = savedTabs.count { saved -> tabsBefore.any { it === saved } }
    return (savedTabsBefore - 1).coerceIn(ZERO, max(savedTabs.size - 1, ZERO))
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
    withTabsMutationLock {
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

  private fun contentUrl(entryPath: String?): String =
    "$entryPath".addContentPrefix

  /**
   * Returns the tabs that can be saved, each paired with its history entity.
   */
  private suspend fun getWebViewHistoryList(
    tabsState: TabsManager.TabsState
  ): List<Pair<KiwixWebView, WebViewHistoryEntity>> {
    return withContext(mainDispatcher) {
      val webViewHistoryEntityList = arrayListOf<Pair<KiwixWebView, WebViewHistoryEntity>>()
      tabsState.webViews.forEachIndexed { index, view ->
        if (view.url == null) return@forEachIndexed
        getWebViewHistoryEntity(view, index)?.let { webViewHistoryEntityList.add(view to it) }
      }
      return@withContext webViewHistoryEntityList
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
    lastSavedTabs = emptyList()
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
