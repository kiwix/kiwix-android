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
package org.kiwix.kiwixmobile.core.webserver.wifi_hotspot

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import org.kiwix.kiwixmobile.core.CoreApp.Companion.coreComponent
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.registerReceiver
import org.kiwix.kiwixmobile.core.utils.ServerUtils.getSocketAddress
import org.kiwix.kiwixmobile.core.webserver.WebServerHelper
import org.kiwix.kiwixmobile.core.webserver.ZimHostCallbacks
import org.kiwix.kiwixmobile.core.webserver.ZimHostFragment
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * HotspotService is used to add a foreground service for the wifi hotspot.
 * Created by Adeel Zafar on 07/01/2019.
 */
class HotspotService :
  Service(),
  IpAddressCallbacks,
  HotspotStateReceiver.Callback {
  @set:Inject
  var webServerHelper: WebServerHelper? = null

  @set:Inject
  var hotspotNotificationManager: HotspotNotificationManager? = null

  @set:Inject
  var hotspotStateReceiver: HotspotStateReceiver? = null

  private var zimHostCallbacks: ZimHostCallbacks? = null
  private val serviceBinder: IBinder = HotspotBinder(this)

  override fun onCreate() {
    coreComponent
      .coreServiceComponent()
      .service(this)
      .build()
      .inject(this)
    super.onCreate()
    hotspotStateReceiver?.let(this::registerReceiver)
  }

  override fun onDestroy() {
    webServerHelper?.dispose()
    hotspotStateReceiver?.let(this@HotspotService::unregisterReceiver)
    super.onDestroy()
  }

  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    when (intent.action) {
      ACTION_START_SERVER ->
        intent.getStringArrayListExtra(ZimHostFragment.SELECTED_ZIM_PATHS_KEY)?.let {
          if (webServerHelper?.startServerHelper(it) == true) {
            zimHostCallbacks?.onServerStarted(getSocketAddress())
            startForegroundNotificationHelper()
            Toast.makeText(
              this, R.string.server_started_successfully_toast_message,
              Toast.LENGTH_SHORT
            ).show()
          } else {
            onServerFailedToStart()
          }
        } ?: kotlin.run(::onServerFailedToStart)

      ACTION_STOP_SERVER -> {
        Toast.makeText(
          this, R.string.server_stopped_successfully_toast_message,
          Toast.LENGTH_SHORT
        ).show()
        stopHotspotAndDismissNotification()
      }

      ACTION_CHECK_IP_ADDRESS -> webServerHelper?.pollForValidIpAddress()
      else -> {}
    }
    return START_NOT_STICKY
  }

  override fun onBind(intent: Intent?): IBinder = serviceBinder

  // Dismiss notification and turn off hotspot for devices>=O
  private fun stopHotspotAndDismissNotification() {
    webServerHelper?.stopAndroidWebServer()
    zimHostCallbacks?.onServerStopped()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
    hotspotStateReceiver = null
    hotspotNotificationManager?.dismissNotification()
  }

  private fun onServerFailedToStart() {
    zimHostCallbacks?.onServerFailedToStart()
  }

  fun registerCallBack(myCallback: ZimHostCallbacks?) {
    zimHostCallbacks = myCallback
  }

  private fun startForegroundNotificationHelper() {
    startForeground(
      HotspotNotificationManager.HOTSPOT_NOTIFICATION_ID,
      hotspotNotificationManager?.buildForegroundNotification()
    )
  }

  override fun onIpAddressValid() {
    zimHostCallbacks?.onIpAddressValid()
  }

  override fun onIpAddressInvalid() {
    zimHostCallbacks?.onIpAddressInvalid()
  }

  override fun onHotspotDisabled() {
    stopHotspotAndDismissNotification()
  }

  class HotspotBinder(hotspotService: HotspotService) : Binder() {
    val service: WeakReference<HotspotService>

    init {
      service = WeakReference<HotspotService>(hotspotService)
    }
  }

  companion object {
    const val ACTION_START_SERVER = "start_server"
    const val ACTION_STOP_SERVER = "stop_server"
    const val ACTION_CHECK_IP_ADDRESS = "check_ip_address"
  }
}
