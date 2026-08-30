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

import android.content.Context
import android.net.ConnectivityManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.compat.CompatHelper
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isAirplaneModeOn
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isNetworkAvailable
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isWifi
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.AirplaneModeBlocked
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.NoInternetWithContent
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.NoInternetWithEmptyContent
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.Proceed
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ResolveRefreshLibraryAction.Result.WifiOnlyBlocked

@OptIn(ExperimentalCoroutinesApi::class)
class ResolveRefreshLibraryActionTest {
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val connectivityManager: ConnectivityManager = mockk()
  private val context: Context = mockk()

  private lateinit var resolver: ResolveRefreshLibraryAction

  @BeforeEach
  fun setup() {
    mockkObject(CompatHelper.Companion)
    every { any<Context>().isAirplaneModeOn() } returns false
    resolver = ResolveRefreshLibraryAction(kiwixDataStore, connectivityManager, context)
  }

  @AfterEach
  fun tearDown() {
    unmockkObject(CompatHelper.Companion)
  }

  @Test
  fun `returns AirplaneModeBlocked when airplane mode is on`() = runTest {
    every { any<Context>().isAirplaneModeOn() } returns true

    val result = resolver(hasItems = true)
    assertEquals(AirplaneModeBlocked, result)
  }

  @Test
  fun `returns NoInternetWithContent when no internet and items exist`() = runTest {
    every { connectivityManager.isNetworkAvailable() } returns false

    val result = resolver(hasItems = true)
    assertEquals(NoInternetWithContent, result)
  }

  @Test
  fun `returns NoInternetWithEmptyContent when no internet and no items`() = runTest {
    every { connectivityManager.isNetworkAvailable() } returns false

    val result = resolver(hasItems = false)
    assertEquals(NoInternetWithEmptyContent, result)
  }

  @Test
  fun `returns WifiOnlyBlocked when wifiOnly is true and not on wifi`() = runTest {
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns false
    every { kiwixDataStore.wifiOnly } returns MutableStateFlow(true)

    val result = resolver(hasItems = true)
    assertEquals(WifiOnlyBlocked, result)
  }

  @Test
  fun `returns Proceed when internet available and no restrictions`() = runTest {
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns true
    every { kiwixDataStore.wifiOnly } returns MutableStateFlow(false)

    val result = resolver(hasItems = true)
    assertEquals(Proceed, result)
  }

  @Test
  fun `returns Proceed when wifiOnly is true but device is on wifi`() = runTest {
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns true
    every { kiwixDataStore.wifiOnly } returns MutableStateFlow(true)

    val result = resolver(hasItems = false)

    assertEquals(Proceed, result)
  }
}
