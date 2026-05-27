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

package org.kiwix.kiwixmobile.nav.destination.library.online.repository

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.data.remote.CategoryEntry
import org.kiwix.kiwixmobile.core.data.remote.CategoryFeed
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryRepositoryImplTest {
  @RegisterExtension
  val dispatcherRule = MainDispatcherRule()

  private val kiwixService: KiwixService = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()

  private lateinit var repository: CategoryRepositoryImpl

  @BeforeEach
  fun setup() {
    repository = CategoryRepositoryImpl(
      kiwixService = kiwixService,
      kiwixDataStore = kiwixDataStore,
      ioDispatcher = dispatcherRule.dispatcher
    )
  }

  @Test
  fun `fetchCategories emits correct list including all categories item`() = runTest {
    val categoryFeed = CategoryFeed().apply {
      entries = listOf(
        CategoryEntry().apply {
          title = "Wikipedia"
        },
        CategoryEntry().apply {
          title = "Gutenberg"
        }
      )
    }

    coEvery { kiwixService.getCategories() } returns categoryFeed
    every { kiwixDataStore.selectedOnlineContentCategory } returns flowOf("Wikipedia")

    val result = repository.fetchCategories().toList()
    advanceUntilIdle()

    assertThat(result).hasSize(1)
    val categories = result[0]
    assertThat(categories).hasSize(3)

    // First category is empty title (all categories)
    assertThat(categories[0].category).isEmpty()
    assertThat(categories[0].active).isFalse()

    // Second is Wikipedia
    assertThat(categories[1].category).isEqualTo("Wikipedia")
    assertThat(categories[1].active).isTrue()

    // Third is Gutenberg
    assertThat(categories[2].category).isEqualTo("Gutenberg")
    assertThat(categories[2].active).isFalse()
  }

  @Test
  fun `fetchCategories emits empty list when feed has no entries`() = runTest {
    val categoryFeed = CategoryFeed().apply {
      entries = emptyList()
    }

    coEvery { kiwixService.getCategories() } returns categoryFeed
    every { kiwixDataStore.selectedOnlineContentCategory } returns flowOf("")

    val result = repository.fetchCategories().toList()
    advanceUntilIdle()

    assertThat(result).hasSize(1)
    assertThat(result[0]).isEmpty()
  }

  @Test
  fun `fetchCategories retries and eventually emits emptyList on continuous failure`() = runTest {
    coEvery { kiwixService.getCategories() } throws RuntimeException("Network Error")

    val result = repository.fetchCategories().toList()
    advanceUntilIdle()

    assertThat(result).hasSize(1)
    assertThat(result[0]).isEmpty()
  }
}
