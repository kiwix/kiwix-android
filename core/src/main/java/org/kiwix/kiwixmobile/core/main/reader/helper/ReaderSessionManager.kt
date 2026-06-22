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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.files.Log
import javax.inject.Inject
import kotlin.math.max

class ReaderSessionManager @Inject constructor(
  private val tabsManager: TabsManager,
  private val zimFileManager: ZimFileManager,
  private val kiwixDataStore: KiwixDataStore,
  private val mainRepositoryActions: MainRepositoryActions,
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

  suspend fun saveReaderSession() {
    savingTabsMutex.withLock {
      clearAndSaveLatestReaderSession(tabsManager.getWebViewHistoryList())
      val source = zimFileManager.zimReaderSource?.toDatabase()
      kiwixDataStore.apply {
        setCurrentZimFile(source.orEmpty())
        setCurrentTab(tabsManager.currentWebViewIndex)
      }
      Log.d(
        TAG_KIWIX,
        "Save current zim file to preferences: $source"
      )
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
}
