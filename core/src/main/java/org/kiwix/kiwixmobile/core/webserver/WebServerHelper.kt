/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.webserver

import android.util.Log
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.DEFAULT_PORT
import org.kiwix.kiwixmobile.core.utils.ServerUtils
import org.kiwix.kiwixmobile.core.utils.ServerUtils.INVALID_IP
import org.kiwix.kiwixmobile.core.utils.ServerUtils.getIp
import org.kiwix.kiwixmobile.core.utils.ServerUtils.getIpAddress
import org.kiwix.kiwixmobile.core.webserver.wifi_hotspot.IpAddressCallbacks
import org.kiwix.kiwixmobile.core.webserver.wifi_hotspot.ServerStatus
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * WebServerHelper class is used to set up the suitable environment i.e. getting the
 * ip address and port no. before starting the WebServer
 * Created by Adeel Zafar on 18/07/2019.
 */
class WebServerHelper @Inject constructor(
  private val kiwixServerFactory: KiwixServer.Factory,
  private val ipAddressCallbacks: IpAddressCallbacks
) {
  private var kiwixServer: KiwixServer? = null
  private var isServerStarted = false
  private var validIpAddressDisposable: Disposable? = null

  fun startServerHelper(selectedBooksPath: ArrayList<String>): ServerStatus {
    val ip = getIpAddress()
    return if (ip.isNullOrEmpty()) {
      ServerStatus(false, R.string.error_ip_address_not_found)
    } else {
      startAndroidWebServer(selectedBooksPath)
    }
  }

  fun stopAndroidWebServer() {
    if (isServerStarted) {
      kiwixServer?.stopServer()
      updateServerState(false)
    }
  }

  private fun startAndroidWebServer(selectedBooksPath: ArrayList<String>): ServerStatus {
    var errorMessage: Int? = null
    if (!isServerStarted) {
      ServerUtils.port = DEFAULT_PORT
      kiwixServer = kiwixServerFactory.createKiwixServer(selectedBooksPath).also {
        updateServerState(it.startServer(ServerUtils.port))
        Log.d(TAG, "Server status$isServerStarted").also {
          if (!isServerStarted) {
            errorMessage = R.string.error_server_already_running
          }
        }
      }
    }
    return ServerStatus(isServerStarted, errorMessage)
  }

  private fun updateServerState(isStarted: Boolean) {
    isServerStarted = isStarted
    ServerUtils.isServerStarted = isStarted
  }

  // Keeps checking if hotspot has been turned using the ip address with an interval of 1 sec
  // If no ip is found after 15 seconds, dismisses the progress dialog
  @Suppress("MagicNumber")
  fun pollForValidIpAddress() {
    validIpAddressDisposable = Flowable.interval(1, TimeUnit.SECONDS)
      .map { getIp() }
      .filter { s: String? -> s != INVALID_IP }
      .timeout(15, TimeUnit.SECONDS)
      .take(1)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(
        { s: String? ->
          ipAddressCallbacks.onIpAddressValid()
          Log.d(TAG, "onSuccess:  $s")
        }
      ) { e: Throwable? ->
        Log.d(TAG, "Unable to turn on server", e)
        ipAddressCallbacks.onIpAddressInvalid()
      }
  }

  fun dispose() {
    validIpAddressDisposable?.dispose()
  }

  companion object {
    private const val TAG = "WebServerHelper"
  }
}
