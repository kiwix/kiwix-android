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

package org.kiwix.kiwixmobile.webserver.wifi_hotspot

import android.content.Context
import android.content.Intent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HotspotStateReceiverTest {
  private val callback: HotspotStateReceiver.Callback = mockk()
  private val context: Context = mockk(relaxed = true)

  private lateinit var receiver: HotspotStateReceiver

  @BeforeEach
  fun setUp() {
    every { callback.onHotspotDisabled() } just Runs
    receiver = HotspotStateReceiver(callback)
  }

  private fun createIntent(state: Int): Intent {
    return mockk {
      every { action } returns ACTION_WIFI_AP_STATE
      every { getIntExtra(EXTRA_WIFI_AP_STATE, -1) } returns state
    }
  }

  @Test
  fun `when hotspot state is disabled then callback is invoked`() {
    receiver.onReceive(context, createIntent(WIFI_AP_STATE_DISABLED))
    verify(exactly = 1) {
      callback.onHotspotDisabled()
    }
  }

  @Test
  fun `when hotspot state is enabled then callback is not invoked`() {
    receiver.onReceive(context, createIntent(WIFI_AP_STATE_ENABLED))
    verify(exactly = 0) {
      callback.onHotspotDisabled()
    }
  }

  @Test
  fun `when hotspot state is enabling then callback is not invoked`() {
    receiver.onReceive(context, createIntent(WIFI_AP_STATE_ENABLING))
    verify(exactly = 0) {
      callback.onHotspotDisabled()
    }
  }

  @Test
  fun `when hotspot state is disabling then callback is not invoked`() {
    receiver.onReceive(context, createIntent(WIFI_AP_STATE_DISABLING))
    verify(exactly = 0) {
      callback.onHotspotDisabled()
    }
  }

  @Test
  fun `when hotspot state is failed then callback is not invoked`() {
    receiver.onReceive(context, createIntent(WIFI_AP_STATE_FAILED))
    verify(exactly = 0) {
      callback.onHotspotDisabled()
    }
  }

  @Test
  fun `when hotspot state is invalid then callback is not invoked`() {
    receiver.onReceive(context, createIntent(-1))
    verify(exactly = 0) {
      callback.onHotspotDisabled()
    }
  }

  @Test
  fun `receiver action should match wifi hotspot action`() {
    assert(receiver.action == ACTION_WIFI_AP_STATE)
  }
}
