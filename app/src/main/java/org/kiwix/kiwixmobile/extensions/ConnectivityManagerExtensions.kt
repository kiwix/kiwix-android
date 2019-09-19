package org.kiwix.kiwixmobile.extensions

import android.net.ConnectivityManager
import org.kiwix.kiwixmobile.zim_manager.NetworkState
import org.kiwix.kiwixmobile.zim_manager.NetworkState.CONNECTED
import org.kiwix.kiwixmobile.zim_manager.NetworkState.NOT_CONNECTED

val ConnectivityManager.networkState: NetworkState
  get() = if (activeNetworkInfo?.isConnected == true)
    CONNECTED
  else
    NOT_CONNECTED
