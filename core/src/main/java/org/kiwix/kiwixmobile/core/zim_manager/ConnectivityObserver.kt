/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.zim_manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.isAirplaneModeOn
import org.kiwix.kiwixmobile.core.networkState
import javax.inject.Inject

class ConnectivityObserver @Inject constructor(
  private val connectivityManager: ConnectivityManager,
  @param:ApplicationContext private val context: Context
) {
  private val _networkStates = MutableStateFlow(connectivityManager.networkState(context))
  val networkStates: StateFlow<NetworkState> = _networkStates

  private val networkCallback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
      _networkStates.tryEmit(connectivityManager.networkState(context))
    }

    override fun onLost(network: Network) {
      _networkStates.tryEmit(connectivityManager.networkState(context))
    }

    override fun onCapabilitiesChanged(
      network: Network,
      networkCapabilities: NetworkCapabilities
    ) {
      _networkStates.tryEmit(connectivityManager.networkState(context))
    }

    override fun onUnavailable() {
      _networkStates.tryEmit(
        if (context.isAirplaneModeOn()) NetworkState.AIRPLANE_MODE else NetworkState.NOT_CONNECTED
      )
    }
  }

  fun register() {
    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()
    connectivityManager.registerNetworkCallback(request, networkCallback)
    _networkStates.tryEmit(connectivityManager.networkState(context))
  }

  fun unregister() {
    runCatching {
      connectivityManager.unregisterNetworkCallback(networkCallback)
    }.onFailure { it.printStackTrace() }
  }
}

/**
 * Builds the "we need to be online to <action>" message, picking the flight-mode
 * wording when the OS reports airplane mode, otherwise the generic offline wording.
 * Only call this once connectivity is already known to be absent.
 */
fun ConnectivityObserver.offlineMessage(context: Context, actionResId: Int): String {
  val action = context.getString(actionResId)
  return if (networkStates.value == NetworkState.AIRPLANE_MODE) {
    context.getString(R.string.airplane_mode_feature_blocked_message, action)
  } else {
    context.getString(R.string.offline_feature_blocked_message, action)
  }
}
