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

import android.content.Context
import android.content.Intent
import org.kiwix.kiwixmobile.core.base.BaseBroadcastReceiver
import org.kiwix.kiwixmobile.core.webserver.wifi_hotspot.HotspotStateReceiver.HotspotState.DISABLED
import javax.inject.Inject

const val EXTRA_WIFI_AP_STATE = "wifi_state"
const val ACTION_WIFI_AP_STATE = "android.net.wifi.WIFI_AP_STATE_CHANGED"

const val WIFI_AP_STATE_DISABLING = 10
const val WIFI_AP_STATE_DISABLED = 11
const val WIFI_AP_STATE_ENABLING = 12
const val WIFI_AP_STATE_ENABLED = 13
const val WIFI_AP_STATE_FAILED = 14

class HotspotStateReceiver @Inject constructor(private val callback: Callback) :
  BaseBroadcastReceiver() {
  override val action: String = ACTION_WIFI_AP_STATE

  override fun onIntentWithActionReceived(context: Context, intent: Intent) {
    if (DISABLED == hotspotState(intent)) {
      callback.onHotspotDisabled()
    }
  }

  private fun hotspotState(intent: Intent) =
    HotspotState.from(
      intent.getIntExtra(
        EXTRA_WIFI_AP_STATE,
        -1
      )
    )

  interface Callback {
    fun onHotspotDisabled()
  }

  private enum class HotspotState(val state: Int) {

    DISABLING(WIFI_AP_STATE_DISABLING),
    DISABLED(WIFI_AP_STATE_DISABLED),
    ENABLING(WIFI_AP_STATE_ENABLING),
    ENABLED(WIFI_AP_STATE_ENABLED),
    FAILED(WIFI_AP_STATE_FAILED);

    companion object {
      fun from(state: Int) = HotspotState.values().firstOrNull { state == it.state }
    }
  }
}
