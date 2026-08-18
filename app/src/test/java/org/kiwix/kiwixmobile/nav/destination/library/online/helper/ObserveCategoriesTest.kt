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
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.nav.destination.library.online.repository.CategoryRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveCategoriesTest {
  private val repository: CategoryRepository = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()
  private val networkStates = MutableStateFlow(NetworkState.CONNECTED)

  private lateinit var observeCategories: ObserveCategories

  @BeforeEach
  fun setup() {
    clearAllMocks()
    every { connectivityBroadcastReceiver.networkStates } returns networkStates
    observeCategories = ObserveCategories(repository, kiwixDataStore, connectivityBroadcastReceiver)
  }

  @Test
  fun `when already fetched and cached list exists returns success from cache`() = runTest {
    observeCategories.hasFetched = true
    val cachedCategories = listOf(Category(1L, true, "Wikipedia"))
    every { kiwixDataStore.cachedOnlineCategoryList } returns flowOf(cachedCategories)

    val result = observeCategories("Error No Category", "Error No Network")

    assertThat(result).isInstanceOf(ObserveCategories.Result.Success::class.java)
    assertThat((result as ObserveCategories.Result.Success).categories).isEqualTo(cachedCategories)
    coVerify(exactly = 0) { repository.fetchCategories() }
  }

  @Test
  fun `when online and repository returns categories saves to cache and returns success`() = runTest {
    networkStates.value = NetworkState.CONNECTED
    every { kiwixDataStore.cachedOnlineCategoryList } returns flowOf(null)
    val fetchedCategories = listOf(Category(1L, true, "Wikipedia"))
    every { repository.fetchCategories() } returns flowOf(fetchedCategories)
    coEvery { kiwixDataStore.saveOnlineCategoryList(any()) } returns Unit

    val result = observeCategories("Error No Category", "Error No Network")

    assertThat(result).isInstanceOf(ObserveCategories.Result.Success::class.java)
    assertThat((result as ObserveCategories.Result.Success).categories).isEqualTo(fetchedCategories)
    assertThat(observeCategories.hasFetched).isTrue()
    coVerify(exactly = 1) { kiwixDataStore.saveOnlineCategoryList(fetchedCategories) }
  }

  @Test
  fun `when online and repository returns empty falls back to cache`() = runTest {
    networkStates.value = NetworkState.CONNECTED
    val cachedCategories = listOf(Category(1L, true, "Wikipedia"))
    every { kiwixDataStore.cachedOnlineCategoryList } returns flowOf(cachedCategories)
    every { repository.fetchCategories() } returns flowOf(emptyList())

    val result = observeCategories("Error No Category", "Error No Network")

    assertThat(result).isInstanceOf(ObserveCategories.Result.Success::class.java)
    assertThat((result as ObserveCategories.Result.Success).categories).isEqualTo(cachedCategories)
  }

  @Test
  fun `when online and repository returns empty and cache is empty returns error`() = runTest {
    networkStates.value = NetworkState.CONNECTED
    every { kiwixDataStore.cachedOnlineCategoryList } returns flowOf(emptyList())
    every { repository.fetchCategories() } returns flowOf(emptyList())

    val result = observeCategories("Error No Category", "Error No Network")

    assertThat(result).isInstanceOf(ObserveCategories.Result.Error::class.java)
    assertThat((result as ObserveCategories.Result.Error).message).isEqualTo("Error No Category")
  }

  @Test
  fun `when offline and cache is not empty returns cache success`() = runTest {
    networkStates.value = NetworkState.NOT_CONNECTED
    val cachedCategories = listOf(Category(1L, true, "Wikipedia"))
    every { kiwixDataStore.cachedOnlineCategoryList } returns flowOf(cachedCategories)

    val result = observeCategories("Error No Category", "Error No Network")

    assertThat(result).isInstanceOf(ObserveCategories.Result.Success::class.java)
    assertThat((result as ObserveCategories.Result.Success).categories).isEqualTo(cachedCategories)
    coVerify(exactly = 0) { repository.fetchCategories() }
  }

  @Test
  fun `when offline and cache is empty returns error`() = runTest {
    networkStates.value = NetworkState.NOT_CONNECTED
    every { kiwixDataStore.cachedOnlineCategoryList } returns flowOf(emptyList())

    val result = observeCategories("Error No Category", "Error No Network")

    assertThat(result).isInstanceOf(ObserveCategories.Result.Error::class.java)
    assertThat((result as ObserveCategories.Result.Error).message).isEqualTo("Error No Network")
    coVerify(exactly = 0) { repository.fetchCategories() }
  }
}
