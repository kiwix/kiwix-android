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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isNetworkAvailable
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isWifi
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.NoInternetConnection
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.WifiOnlyException
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Loading
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Success
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Idle
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryRequest
import org.kiwix.kiwixmobile.nav.destination.library.online.repository.OnlineLibraryRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveOnlineLibraryTest {
  private val repository: OnlineLibraryRepository = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val connectivityManager: ConnectivityManager = mockk()

  private lateinit var observeOnlineLibrary: ObserveOnlineLibrary

  @BeforeEach
  fun setup() {
    observeOnlineLibrary = ObserveOnlineLibrary(
      repository,
      kiwixDataStore,
      connectivityManager
    )
  }

  private fun onlineLibraryRequest(
    query: String = "",
    category: String = "",
    lang: String = "",
    isLoadMoreItem: Boolean = false,
    page: Int = 0
  ) = OnlineLibraryRequest(
    query = query,
    category = category,
    lang = lang,
    isLoadMoreItem = isLoadMoreItem,
    page = page
  )

  @Test
  fun `no internet emits NoInternetConnection and skips repository`() = runTest {
    every { connectivityManager.isNetworkAvailable() } returns false
    val request = onlineLibraryRequest()
    val result = observeOnlineLibrary(
      flowOf(request),
      null
    ).first()

    assertThat(NoInternetConnection).isEqualTo(result)

    coVerify(exactly = 0) {
      repository.fetchOnlineLibrary(request, any())
    }
  }

  @Test
  fun `mobile with wifiOnly true emits WifiOnlyException`() = runTest {
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns false
    every { kiwixDataStore.wifiOnly } returns flowOf(true)
    val request = onlineLibraryRequest()
    val results = observeOnlineLibrary(
      flowOf(request),
      null
    ).toList()

    assertThat(listOf(WifiOnlyException)).isEqualTo(results)

    coVerify(exactly = 0) {
      repository.fetchOnlineLibrary(request, any())
    }
  }

  @Test
  fun `wifi emits Idle then repository data`() = runTest {
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns true
    val request = onlineLibraryRequest()
    val repoFlow = flowOf(
      Loading(false),
      Success(request, emptyList(), 1)
    )

    coEvery {
      repository.fetchOnlineLibrary(request, any())
    } returns repoFlow

    val results = observeOnlineLibrary(
      flowOf(request),
      null
    ).toList()

    assertEquals(
      listOf(
        Idle(false),
        Loading(false),
        Success(request, emptyList(), 1)
      ),
      results
    )
  }

  @Test
  fun `mobile with wifiOnly false proceeds to repository`() = runTest {
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns false
    every { kiwixDataStore.wifiOnly } returns flowOf(false)
    val request = onlineLibraryRequest()
    val repoFlow = flowOf(
      Success(request, emptyList(), 1)
    )

    coEvery {
      repository.fetchOnlineLibrary(request, any())
    } returns repoFlow

    val results = observeOnlineLibrary(
      flowOf(request),
      null
    ).toList()

    assertEquals(
      listOf(
        Idle(false),
        Success(request, emptyList(), 1)
      ),
      results
    )
  }

  @Test
  fun `latest request cancels previous one`() = runTest {
    every { connectivityManager.isNetworkAvailable() } returns true
    every { connectivityManager.isWifi() } returns true

    val requestFlow = MutableSharedFlow<OnlineLibraryRequest>()

    val request1 = onlineLibraryRequest(page = 1)
    val request2 = onlineLibraryRequest(page = 2)

    val slowFlow = flow {
      emit(Loading(false))
      delay(1000)
      emit(Success(request1, emptyList(), 1))
    }

    val fastFlow = flowOf(
      Success(request2, emptyList(), 1)
    )

    coEvery {
      repository.fetchOnlineLibrary(match { it.page == 1 }, any())
    } returns slowFlow

    coEvery {
      repository.fetchOnlineLibrary(match { it.page == 2 }, any())
    } returns fastFlow

    val results = mutableListOf<OnlineLibraryState>()

    val job = launch {
      observeOnlineLibrary(requestFlow, null).toList(results)
    }
    requestFlow.emit(request1)
    advanceTimeBy(1)
    requestFlow.emit(request2)

    advanceUntilIdle()
    assertTrue(results.any { it is Success && it.request.page == 2 })
    assertFalse(results.any { it is Success && it.request.page == 1 })

    job.cancel()
  }
}
