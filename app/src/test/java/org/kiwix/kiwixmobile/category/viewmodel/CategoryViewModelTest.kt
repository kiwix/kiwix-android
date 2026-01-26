/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.category.viewmodel

import android.app.Application
import android.os.Build
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.core.data.remote.CategoryFeed
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.language.viewmodel.flakyTest
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.Action
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.Action.UpdateCategory
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryListItem
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategorySessionCache
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.SaveCategoryAndFinish
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Content
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Loading
import org.kiwix.kiwixmobile.zimManager.testFlow
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.category

@ExtendWith(InstantExecutorExtension::class)
class CategoryViewModelTest {
  private val application: Application = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val kiwixService: KiwixService = mockk()
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()
  private val networkStates = MutableStateFlow(NetworkState.CONNECTED)
  private lateinit var categoryViewModel: CategoryViewModel
  private var categories: MutableStateFlow<List<Category>?> = MutableStateFlow(null)

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { application.getString(any()) } returns "Error"
    every { connectivityBroadcastReceiver.action } returns "test"
    every { connectivityBroadcastReceiver.networkStates } returns networkStates
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      every { application.registerReceiver(any(), any(), any()) } returns mockk()
    } else {
      @Suppress("UnspecifiedRegisterReceiverFlag")
      every { application.registerReceiver(any(), any()) } returns mockk()
    }
    every { application.unregisterReceiver(any()) } just Runs
    CategorySessionCache.hasFetched = false
    every { kiwixDataStore.cachedOnlineCategoryList } returns flowOf(categories.value)
    every { kiwixDataStore.selectedOnlineContentCategory } returns flowOf("")
  }

  private fun createViewModel() {
    categoryViewModel =
      CategoryViewModel(
        application,
        kiwixDataStore,
        kiwixService,
        connectivityBroadcastReceiver
      ).apply {
        setIsUnitTestCase()
      }
  }

  @Nested
  inner class Context {
    @Test
    fun `registers broadcastReceiver in init`() {
      createViewModel()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        verify {
          application.registerReceiver(connectivityBroadcastReceiver, any(), any())
        }
      } else {
        @Suppress("UnspecifiedRegisterReceiverFlag")
        verify {
          application.registerReceiver(connectivityBroadcastReceiver, any())
        }
      }
    }

    @Test
    fun `unregisters broadcastReceiver in onCleared`() {
      createViewModel()
      every { application.unregisterReceiver(any()) } returns mockk()
      categoryViewModel.onClearedExposed()
      verify {
        application.unregisterReceiver(connectivityBroadcastReceiver)
      }
    }
  }

  @Test
  fun `initial state is Loading`() = flakyTest {
    runTest {
      coEvery { kiwixService.getCategories() } returns CategoryFeed()
      createViewModel()
      assertThat(categoryViewModel.state.value).isEqualTo(Loading)
    }
  }

  @Test
  fun `an empty categories emission does not send update action`() = runTest {
    createViewModel()
    testFlow(
      categoryViewModel.actions,
      triggerAction = { categories.emit(listOf()) },
      assert = { expectNoEvents() }
    )
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Save uses active category`() = flakyTest {
    runTest {
      every { application.getString(any()) } returns ""
      createViewModel()
      val activeCategory = category(category = "wikipedia").copy(active = true)
      val inactiveCategory = category(category = "gutenburg").copy(active = false)
      val categoryItem = CategoryListItem.CategoryItem(activeCategory)
      categoryViewModel.effects.test {
        categoryViewModel.state.value = Content(listOf(activeCategory, inactiveCategory))
        categoryViewModel.actions.emit(Action.Select(categoryItem))
        advanceUntilIdle()
        advanceTimeBy(100)
        val effect = awaitItem() as SaveCategoryAndFinish
        assertThat(effect.category).isEqualTo(activeCategory)
      }
    }
  }

  @Test
  fun `online and api returns empty emits Error when no cache`() = flakyTest {
    runTest {
      coEvery { kiwixService.getCategories() } returns CategoryFeed()
      coEvery { application.getString(string.no_category_available) } returns "No category available"

      createViewModel()
      categoryViewModel.state.test {
        assertThat(awaitItem()).isEqualTo(Loading)
        val error = awaitItem() as State.Error
        assertThat(error.errorMessage).isEqualTo("No category available")
      }
    }
  }

  @Test
  fun `online api throws exception falls back to error`() = flakyTest {
    runTest {
      coEvery { kiwixService.getCategories() } throws RuntimeException()

      createViewModel()
      categoryViewModel.state.test {
        assertThat(awaitItem()).isEqualTo(Loading)
        val error = awaitItem() as State.Error
        assertThat(error.errorMessage).isEqualTo("Error")
      }
    }
  }

  @Test
  fun `offline uses cached categories`() = flakyTest {
    runTest {
      networkStates.value = NetworkState.NOT_CONNECTED

      val cached =
        listOf(
          Category(category = "Offline", active = true, id = 1)
        )

      coEvery { kiwixDataStore.cachedOnlineCategoryList } returns
        flowOf(cached)

      createViewModel()
      categoryViewModel.state.test {
        assertThat(awaitItem()).isEqualTo(Loading)
        val content = awaitItem() as Content
        assertThat(content.items.first().category)
          .isEqualTo("Offline")
      }
    }
  }

  @Test
  fun `offline and no cache emits no network error`() = flakyTest {
    runTest {
      networkStates.value = NetworkState.NOT_CONNECTED

      createViewModel()
      categoryViewModel.state.test {
        assertThat(awaitItem()).isEqualTo(Loading)
        val error = awaitItem() as State.Error
        assertThat(error.errorMessage).isEqualTo("Error")
      }
    }
  }

  @Test
  fun `session cache skips api call`() = flakyTest {
    runTest {
      CategorySessionCache.hasFetched = true

      val cached =
        listOf(
          Category(1, true, "Cached")
        )

      coEvery { kiwixDataStore.cachedOnlineCategoryList } returns
        flowOf(cached)

      createViewModel()
      verify(exactly = 0) {
        runBlocking { kiwixService.getCategories() }
      }

      assertThat(categoryViewModel.state.value)
        .isInstanceOf(Content::class.java)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `UpdateCategory changes Loading to Content`() = flakyTest {
    runTest {
      CategorySessionCache.hasFetched = true
      coEvery { kiwixDataStore.cachedOnlineCategoryList } returns flowOf(emptyList())
      coEvery { kiwixService.getCategories() } returns CategoryFeed()

      createViewModel()
      val categories = listOf(Category(1, false, "Test"))
      categoryViewModel.state.test {
        skipItems(1)

        categoryViewModel.actions.emit(UpdateCategory(categories))
        advanceUntilIdle()

        assertThat(awaitItem()).isEqualTo(Content(categories))
        cancelAndConsumeRemainingEvents()
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Filter updates content`() = flakyTest {
    runTest {
      coEvery { kiwixService.getCategories() } returns CategoryFeed()

      val categories =
        listOf(
          Category(1, false, "wikipedia"),
          Category(2, false, "Gutenburg")
        )

      createViewModel()
      categoryViewModel.state.test {
        skipItems(1)
        categoryViewModel.actions.emit(UpdateCategory(categories))
        advanceUntilIdle()

        categoryViewModel.actions.emit(Action.Filter("wiki"))
        advanceUntilIdle()
        val content = awaitItem() as Content
        val filteredItem: CategoryListItem.CategoryItem =
          content.viewItems.first { it is CategoryListItem.CategoryItem } as CategoryListItem.CategoryItem
        assertThat(filteredItem.category.category).isEqualTo("wikipedia")
        cancelAndConsumeRemainingEvents()
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Select emits side effect and moves to Saving`() = flakyTest {
    runTest {
      coEvery { kiwixService.getCategories() } returns CategoryFeed()

      val items =
        listOf(
          Category(0, true, ""),
          Category(1, false, "Wikipedia")
        )

      createViewModel()
      advanceUntilIdle()
      categoryViewModel.effects.test {
        categoryViewModel.actions.emit(UpdateCategory(items))
        advanceUntilIdle()
        categoryViewModel.actions.emit(
          Action.Select(
            CategoryListItem.CategoryItem(
              Category(1, false, "Wikipedia")
            )
          )
        )

        advanceUntilIdle()
        assertThat(awaitItem())
          .isInstanceOf(SaveCategoryAndFinish::class.java)

        cancelAndConsumeRemainingEvents()
      }
    }
  }
}
