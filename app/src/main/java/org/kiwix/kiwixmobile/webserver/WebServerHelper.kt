/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.webserver

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.utils.DEFAULT_PORT
import org.kiwix.kiwixmobile.core.utils.ServerUtils
import org.kiwix.kiwixmobile.core.utils.ServerUtils.INVALID_IP
import org.kiwix.kiwixmobile.core.utils.ServerUtils.getIp
import org.kiwix.kiwixmobile.core.utils.ServerUtils.getIpAddress
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.IpAddressCallbacks
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.ServerStatus
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * WebServerHelper class is used to set up the suitable environment i.e. getting the
 * ip address and port no. before starting the WebServer
 * Created by Adeel Zafar on 18/07/2019.
 */

const val FINDING_IP_ADDRESS_TIMEOUT = 15_000L
const val FINDING_IP_ADDRESS_RETRY_TIME = 1_000L

class WebServerHelper @Inject constructor(
  private val kiwixServerFactory: KiwixServer.Factory,
  private val ipAddressCallbacks: IpAddressCallbacks
) {
  private var kiwixServer: KiwixServer? = null
  private var isServerStarted = false

  suspend fun startServerHelper(
    selectedBooksPath: ArrayList<String>,
    restartServer: Boolean
  ): ServerStatus? {
    val ip = getIpAddress()
    return if (ip.isNullOrEmpty()) {
      ServerStatus(false, R.string.error_ip_address_not_found)
    } else {
      startAndroidWebServer(selectedBooksPath, restartServer)
    }
  }

  fun stopAndroidWebServer() {
    if (isServerStarted) {
      kiwixServer?.stopServer()
      updateServerState(false)
    }
  }

  private suspend fun startAndroidWebServer(
    selectedBooksPath: ArrayList<String>,
    restartServer: Boolean
  ): ServerStatus? {
    var serverStatus: ServerStatus? = null
    if (!isServerStarted) {
      serverStatus = startKiwixServer(selectedBooksPath)
    } else if (restartServer) {
      kiwixServer?.stopServer()
      serverStatus = startKiwixServer(selectedBooksPath)
    }
    return serverStatus
  }

  private suspend fun startKiwixServer(selectedBooksPath: ArrayList<String>): ServerStatus {
    var errorMessage: Int? = null
    ServerUtils.port = DEFAULT_PORT
    kiwixServer =
      kiwixServerFactory.createKiwixServer(selectedBooksPath).also {
        updateServerState(it.startServer(ServerUtils.port))
        Log.d(TAG, "Server status$isServerStarted").also {
          if (!isServerStarted) {
            errorMessage = R.string.error_server_already_running
          }
        }
      }
    return ServerStatus(isServerStarted, errorMessage)
  }

  private fun updateServerState(isStarted: Boolean) {
    isServerStarted = isStarted
    ServerUtils.isServerStarted = isStarted
  }

  /**
   * Starts polling for a valid IP address using a [Flow].
   * - Polls every [FINDING_IP_ADDRESS_RETRY_TIME] milliseconds.
   * - If a valid IP is found, invokes [IpAddressCallbacks.onIpAddressInvalid].
   * - If no valid IP is found within [FINDING_IP_ADDRESS_TIMEOUT] seconds,
   * invokes [IpAddressCallbacks.onIpAddressInvalid].
   * - The flow runs on [Dispatchers.IO] and results are collected on the Main thread.
   * - Automatically cancels if [serviceScope] is cancelled (e.g. lifecycle aware).
   */
  @OptIn(FlowPreview::class)
  @Suppress("InjectDispatcher")
  fun pollForValidIpAddress(serviceScope: CoroutineScope) {
    serviceScope.launch(Dispatchers.Main) {
      ipPollingFlow()
        .timeout(FINDING_IP_ADDRESS_TIMEOUT.seconds)
        .catch {
          Log.d(TAG, "Unable to turn on server", it)
          ipAddressCallbacks.onIpAddressInvalid()
        }.collect {
          ipAddressCallbacks.onIpAddressValid()
          Log.d(TAG, "onSuccess:  $it")
        }
    }
  }

  /**
   * Creates a [Flow] that emits the current IP address every [FINDING_IP_ADDRESS_RETRY_TIME] milliseconds.
   * - If the returned IP is not [INVALID_IP], the flow completes.
   * - The flow runs entirely on [Dispatchers.IO].
   */
  @Suppress("InjectDispatcher", "TooGenericExceptionCaught")
  private fun ipPollingFlow(): Flow<String?> = flow {
    while (true) {
      // if ip address is not found wait for 1 second to again getting the ip address.
      // this is equivalent to our `rxJava` code.
      delay(FINDING_IP_ADDRESS_RETRY_TIME)
      val ip = try {
        getIp()
      } catch (e: Exception) {
        Log.e(TAG, "Error getting IP address", e)
        INVALID_IP
      }
      emit(ip)

      if (ip != INVALID_IP) break
    }
  }.flowOn(Dispatchers.IO)

  companion object {
    private const val TAG = "WebServerHelper"
  }
}
