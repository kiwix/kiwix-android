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

@file:Suppress("PackageNaming")

package org.kiwix.kiwixmobile.localFileTransfer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager.EXTRA_NETWORK_INFO
import android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_P2P_DEVICE
import android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_STATE
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_ENABLED
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION

/**
 * Helper class for the local file sharing module.
 *
 * Handles the broadcasts pertaining to the wifi p2p group formed in WiFi Direct. Works along with
 * the wifi p2p manager in [WifiDirectManager].
 */

class KiwixWifiP2pBroadcastReceiver(private val p2pEventListener: P2pEventListener) :
  BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      WIFI_P2P_STATE_CHANGED_ACTION -> {
        val wifiP2pState = intent.getIntExtra(EXTRA_WIFI_STATE, -1)
        p2pEventListener.onWifiP2pStateChanged(wifiP2pState == WIFI_P2P_STATE_ENABLED)
      }
      WIFI_P2P_PEERS_CHANGED_ACTION -> p2pEventListener.onPeersChanged()
      WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
        val networkInfo =
          intent.getParcelableExtra<NetworkInfo>(EXTRA_NETWORK_INFO)
        networkInfo?.let {
          p2pEventListener.onConnectionChanged(it.isConnected)
        }
      }
      WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
        val userDevice =
          intent.getParcelableExtra<WifiP2pDevice>(EXTRA_WIFI_P2P_DEVICE)
        p2pEventListener.onDeviceChanged(userDevice)
      }
    }
  }

  interface P2pEventListener {
    fun onWifiP2pStateChanged(isEnabled: Boolean)
    fun onPeersChanged()
    fun onConnectionChanged(isConnected: Boolean)
    fun onDeviceChanged(userDevice: WifiP2pDevice?)
  }
}
