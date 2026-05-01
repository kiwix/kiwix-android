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

package org.kiwix.kiwixmobile.nav.destination.library.online.helper

import android.net.ConnectivityManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isWifi
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.OnlineLibraryRequest
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.OnlineLibraryState
import org.kiwix.kiwixmobile.nav.destination.library.online.OnlineLibraryViewModel.OnlineLibraryState.WifiOnlyException
import org.kiwix.kiwixmobile.nav.destination.library.online.repository.OnlineLibraryRepository
import org.kiwix.kiwixmobile.zimManager.AppProgressListenerProvider
import javax.inject.Inject

class ObserveOnlineLibrary @Inject constructor(
  private val repository: OnlineLibraryRepository,
  private val kiwixDataStore: KiwixDataStore,
  private val connectivityManager: ConnectivityManager
) {
  @OptIn(ExperimentalCoroutinesApi::class)
  operator fun invoke(
    requests: Flow<OnlineLibraryRequest>,
    appProgressListener: AppProgressListenerProvider?,
    connectivityBroadcastReceiver: ConnectivityBroadcastReceiver
  ): Flow<OnlineLibraryState> {
    return requests
      .flatMapLatest { request ->
        flow {
          connectivityBroadcastReceiver.networkStates
            .filter { it == NetworkState.CONNECTED }
            .first()

          if (!shouldProceedWithDownload()) {
            emit(WifiOnlyException)
            return@flow
          }

          emitAll(repository.fetchOnlineLibrary(request, appProgressListener))
        }
      }
  }

  private suspend fun shouldProceedWithDownload(): Boolean {
    if (!connectivityManager.isWifi()) {
      return !kiwixDataStore.wifiOnly.first()
    }
    return true
  }
}
