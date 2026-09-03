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

package org.kiwix.kiwixmobile.language.helper

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.language.repository.LanguageRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveLanguagesTest {
  private val repository: LanguageRepository = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()
  private val networkStates = MutableStateFlow(NetworkState.CONNECTED)

  private lateinit var observeLanguages: ObserveLanguages

  @BeforeEach
  fun setup() {
    clearAllMocks()
    every { connectivityBroadcastReceiver.networkStates } returns networkStates
    observeLanguages = ObserveLanguages(repository, kiwixDataStore, connectivityBroadcastReceiver)
  }

  @Test
  fun `when already fetched and cached list exists returns success from cache`() = runTest {
    observeLanguages.hasFetched = true
    val cachedLanguages = listOf(Language("eng", true, 10, 1L))
    every { kiwixDataStore.cachedLanguageList } returns flowOf(cachedLanguages)

    val result = observeLanguages("Error No Language", "Error No Network")

    assertThat(result).isInstanceOf(ObserveLanguages.Result.Success::class.java)
    assertThat((result as ObserveLanguages.Result.Success).languages).isEqualTo(cachedLanguages)
    coVerify(exactly = 0) { repository.fetchLanguages() }
  }

  @Test
  fun `when online and repository returns languages saves to cache and returns success`() = runTest {
    networkStates.value = NetworkState.CONNECTED
    every { kiwixDataStore.cachedLanguageList } returns flowOf(null)
    val fetchedLanguages = listOf(Language("eng", true, 10, 1L))
    every { repository.fetchLanguages() } returns flowOf(fetchedLanguages)
    coEvery { kiwixDataStore.saveLanguageList(any()) } returns Unit

    val result = observeLanguages("Error No Language", "Error No Network")

    assertThat(result).isInstanceOf(ObserveLanguages.Result.Success::class.java)
    assertThat((result as ObserveLanguages.Result.Success).languages).isEqualTo(fetchedLanguages)
    assertThat(observeLanguages.hasFetched).isTrue()
    coVerify(exactly = 1) { kiwixDataStore.saveLanguageList(fetchedLanguages) }
  }

  @Test
  fun `when online and repository returns empty falls back to cache`() = runTest {
    networkStates.value = NetworkState.CONNECTED
    val cachedLanguages = listOf(Language("eng", true, 10, 1L))
    every { kiwixDataStore.cachedLanguageList } returns flowOf(cachedLanguages)
    every { repository.fetchLanguages() } returns flowOf(emptyList())

    val result = observeLanguages("Error No Language", "Error No Network")

    assertThat(result).isInstanceOf(ObserveLanguages.Result.Success::class.java)
    assertThat((result as ObserveLanguages.Result.Success).languages).isEqualTo(cachedLanguages)
  }

  @Test
  fun `when online and repository returns empty and cache is empty returns error`() = runTest {
    networkStates.value = NetworkState.CONNECTED
    every { kiwixDataStore.cachedLanguageList } returns flowOf(emptyList())
    every { repository.fetchLanguages() } returns flowOf(emptyList())

    val result = observeLanguages("Error No Language", "Error No Network")

    assertThat(result).isInstanceOf(ObserveLanguages.Result.Error::class.java)
    assertThat((result as ObserveLanguages.Result.Error).message).isEqualTo("Error No Language")
  }

  @Test
  fun `when offline and cache is not empty returns cache success`() = runTest {
    networkStates.value = NetworkState.NOT_CONNECTED
    val cachedLanguages = listOf(Language("eng", true, 10, 1L))
    every { kiwixDataStore.cachedLanguageList } returns flowOf(cachedLanguages)

    val result = observeLanguages("Error No Language", "Error No Network")

    assertThat(result).isInstanceOf(ObserveLanguages.Result.Success::class.java)
    assertThat((result as ObserveLanguages.Result.Success).languages).isEqualTo(cachedLanguages)
    coVerify(exactly = 0) { repository.fetchLanguages() }
  }

  @Test
  fun `when offline and cache is empty returns error`() = runTest {
    networkStates.value = NetworkState.NOT_CONNECTED
    every { kiwixDataStore.cachedLanguageList } returns flowOf(emptyList())

    val result = observeLanguages("Error No Language", "Error No Network")

    assertThat(result).isInstanceOf(ObserveLanguages.Result.Error::class.java)
    assertThat((result as ObserveLanguages.Result.Error).message).isEqualTo("Error No Network")
    coVerify(exactly = 0) { repository.fetchLanguages() }
  }
}
