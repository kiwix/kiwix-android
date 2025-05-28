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
import android.content.Intent
import android.net.ConnectivityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.kiwix.kiwixmobile.core.base.BaseBroadcastReceiver
import org.kiwix.kiwixmobile.core.networkState
import javax.inject.Inject

class ConnectivityBroadcastReceiver @Inject constructor(
  private val connectivityManager: ConnectivityManager
) : BaseBroadcastReceiver() {
  @Suppress("DEPRECATION")
  override val action: String = ConnectivityManager.CONNECTIVITY_ACTION

  private val _networkStates = MutableStateFlow(NetworkState.NOT_CONNECTED).apply {
    tryEmit(connectivityManager.networkState)
  }
  val networkStates: StateFlow<NetworkState> = _networkStates

  override fun onIntentWithActionReceived(
    context: Context,
    intent: Intent
  ) {
    _networkStates.tryEmit(connectivityManager.networkState)
  }
}
