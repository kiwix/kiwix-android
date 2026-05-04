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

package org.kiwix.kiwixmobile.nav.destination.library.online.helper

import android.net.ConnectivityManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.assertj.core.api.Assertions.assertThat
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isWifi
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ObserveNetworkState.Result.WifiAvailable
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ObserveNetworkState.Result.ShowWifiOnlyMessage
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ObserveNetworkState.Result.MobileInternet
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ObserveNetworkState.Result.ShowNoInternetSnackBar

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveNetworkStateTest {
  private val connectivityManager: ConnectivityManager = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()

  private lateinit var observeNetworkState: ObserveNetworkState

  @BeforeEach
  fun setup() {
    observeNetworkState = ObserveNetworkState(connectivityManager, kiwixDataStore)
  }

  @Test
  fun `when connected and wifi then emit WifiAvailable`() = runTest {
    every { connectivityManager.isWifi() } returns true
    every { kiwixDataStore.wifiOnly } returns flowOf(true)

    val result = observeNetworkState(flowOf(NetworkState.CONNECTED)).first()
    assertThat(result).isEqualTo(WifiAvailable)
  }

  @Test
  fun `when connected mobile and wifiOnly true then emit ShowWifiOnlyMessage`() = runTest {
    every { connectivityManager.isWifi() } returns false
    every { kiwixDataStore.wifiOnly } returns flowOf(true)

    val result = observeNetworkState(flowOf(NetworkState.CONNECTED)).first()

    assertThat(result).isEqualTo(ShowWifiOnlyMessage)
  }

  @Test
  fun `when connected mobile and wifiOnly false then emit MobileInternet`() = runTest {
    every { connectivityManager.isWifi() } returns false
    every { kiwixDataStore.wifiOnly } returns flowOf(false)

    val result = observeNetworkState(flowOf(NetworkState.CONNECTED)).first()

    assertThat(MobileInternet).isEqualTo(result)
  }

  @Test
  fun `when not connected then emit ShowNoInternetSnackBar`() = runTest {
    every { connectivityManager.isWifi() } returns false
    every { kiwixDataStore.wifiOnly } returns flowOf(false)

    val result = observeNetworkState(flowOf(NetworkState.NOT_CONNECTED)).first()

    assertThat(ShowNoInternetSnackBar).isEqualTo(result)
  }

  @Test
  fun `emits correct sequence for multiple states`() = runTest {
    every { connectivityManager.isWifi() } returns false
    every { kiwixDataStore.wifiOnly } returns flowOf(false)

    val results = observeNetworkState(
      flowOf(
        NetworkState.NOT_CONNECTED,
        NetworkState.CONNECTED
      )
    ).toList()

    assertThat(listOf(ShowNoInternetSnackBar, MobileInternet)).isEqualTo(results)
  }
}
