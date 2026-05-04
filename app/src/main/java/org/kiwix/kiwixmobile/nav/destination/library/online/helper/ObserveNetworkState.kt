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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isWifi
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import javax.inject.Inject

class ObserveNetworkState @Inject constructor(
  private val connectivityManager: ConnectivityManager,
  private val kiwixDataStore: KiwixDataStore
) {
  sealed class Result {
    object WifiAvailable : Result()
    object ShowWifiOnlyMessage : Result()
    object ShowNoInternetSnackBar : Result()

    object MobileInternet : Result()
  }

  operator fun invoke(networkState: Flow<NetworkState>): Flow<Result> =
    networkState.combine(kiwixDataStore.wifiOnly) { state, wifiOnly ->
      when (state) {
        NetworkState.CONNECTED -> {
          val isWifi = connectivityManager.isWifi()
          when {
            isWifi -> Result.WifiAvailable
            wifiOnly -> Result.ShowWifiOnlyMessage
            else -> Result.MobileInternet
          }
        }

        NetworkState.NOT_CONNECTED -> {
          Result.ShowNoInternetSnackBar
        }
      }
    }
}
