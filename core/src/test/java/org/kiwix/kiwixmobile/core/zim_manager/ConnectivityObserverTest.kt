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
package org.kiwix.kiwixmobile.core.zim_manager

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import android.net.NetworkRequest
import android.os.Build
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState.CONNECTED
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState.NOT_CONNECTED
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Runs under Robolectric because [ConnectivityObserver.register] builds a real
 * [NetworkRequest] - on the plain unit-test Android stub jar `NetworkRequest.Builder`
 * methods return null instead of running, which masks bugs like requesting the
 * unsupported [NET_CAPABILITY_VALIDATED] capability.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU], application = TestApplication::class)
class ConnectivityObserverTest {
  private val connectivityManager: ConnectivityManager = mockk()
  private val network: Network = mockk()
  private val networkCapabilities: NetworkCapabilities = mockk()
  private val callbackSlot = slot<NetworkCallback>()

  @Before
  fun setUp() {
    withNetworkAvailability(available = false)
    every {
      connectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(callbackSlot))
    } just Runs
    every { connectivityManager.unregisterNetworkCallback(any<NetworkCallback>()) } just Runs
  }

  private fun withNetworkAvailability(available: Boolean) {
    every { connectivityManager.activeNetwork } returns network
    every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
    every { networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET) } returns available
  }

  @Test
  fun `initial state reflects the network state at construction`() {
    withNetworkAvailability(available = true)
    val connectivityObserver = ConnectivityObserver(connectivityManager)

    assertEquals(CONNECTED, connectivityObserver.networkStates.value)
  }

  @Test
  fun `register does not request the unsupported VALIDATED capability`() {
    val connectivityObserver = ConnectivityObserver(connectivityManager)
    val requestSlot = slot<NetworkRequest>()
    every {
      connectivityManager.registerNetworkCallback(capture(requestSlot), any<NetworkCallback>())
    } just Runs

    connectivityObserver.register()

    assertFalse(requestSlot.captured.hasCapability(NET_CAPABILITY_VALIDATED))
  }

  @Test
  fun `register refreshes a stale initial state`() {
    withNetworkAvailability(available = false)
    val connectivityObserver = ConnectivityObserver(connectivityManager)
    // Network became available after construction but before register() is called.
    withNetworkAvailability(available = true)

    connectivityObserver.register()

    assertEquals(CONNECTED, connectivityObserver.networkStates.value)
  }

  @Test
  fun `onAvailable and onLost update the network state`() {
    val connectivityObserver = ConnectivityObserver(connectivityManager)
    connectivityObserver.register()

    withNetworkAvailability(available = true)
    callbackSlot.captured.onAvailable(network)
    assertEquals(CONNECTED, connectivityObserver.networkStates.value)

    withNetworkAvailability(available = false)
    callbackSlot.captured.onLost(network)
    assertEquals(NOT_CONNECTED, connectivityObserver.networkStates.value)
  }

  @Test
  fun `onCapabilitiesChanged updates the network state`() {
    val connectivityObserver = ConnectivityObserver(connectivityManager)
    connectivityObserver.register()

    withNetworkAvailability(available = true)
    callbackSlot.captured.onCapabilitiesChanged(network, networkCapabilities)

    assertEquals(CONNECTED, connectivityObserver.networkStates.value)
  }

  @Test
  fun `onUnavailable marks the network as not connected`() {
    val connectivityObserver = ConnectivityObserver(connectivityManager)
    connectivityObserver.register()

    withNetworkAvailability(available = true)
    callbackSlot.captured.onAvailable(network)
    assertEquals(CONNECTED, connectivityObserver.networkStates.value)

    callbackSlot.captured.onUnavailable()
    assertEquals(NOT_CONNECTED, connectivityObserver.networkStates.value)
  }

  @Test
  fun `calling register twice only registers once with ConnectivityManager`() {
    val connectivityObserver = ConnectivityObserver(connectivityManager)

    connectivityObserver.register()
    connectivityObserver.register()

    verify(exactly = 1) {
      connectivityManager.registerNetworkCallback(any<NetworkRequest>(), any<NetworkCallback>())
    }
  }

  @Test
  fun `calling unregister twice only unregisters once with ConnectivityManager`() {
    val connectivityObserver = ConnectivityObserver(connectivityManager)

    connectivityObserver.register()
    connectivityObserver.unregister()
    connectivityObserver.unregister()

    verify(exactly = 1) {
      connectivityManager.unregisterNetworkCallback(any<NetworkCallback>())
    }
  }

  @Test
  fun `unregister without a matching register is a no-op`() {
    val connectivityObserver = ConnectivityObserver(connectivityManager)

    connectivityObserver.unregister()

    verify(exactly = 0) {
      connectivityManager.unregisterNetworkCallback(any<NetworkCallback>())
    }
  }
}
