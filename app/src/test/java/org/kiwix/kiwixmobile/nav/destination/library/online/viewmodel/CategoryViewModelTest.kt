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

package org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel

import android.app.Application
import android.os.Build
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.core.data.remote.CategoryEntry
import org.kiwix.kiwixmobile.core.data.remote.CategoryFeed
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel.Action
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel.Action.UpdateCategory
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Loading
import org.kiwix.sharedFunctions.MainDispatcherRule
import org.kiwix.kiwixmobile.core.R as CoreR

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelTest {
  private val application: Application = mockk(relaxed = true)
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val kiwixService: KiwixService = mockk()
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()

  @RegisterExtension
  val mainDispatcher = MainDispatcherRule()
  private val networkStates = MutableStateFlow(NetworkState.CONNECTED)
  private lateinit var categoryViewModel: CategoryViewModel
  private var categories: MutableStateFlow<List<Category>?> = MutableStateFlow(null)

  private val errorMessage = "No Internet Connection"
  private fun createCategory(
    id: Long = 1,
    active: Boolean = false,
    category: String = "Wikipedia"
  ) = Category(id, active, category)

  @BeforeEach
  fun init() {
    networkStates.value = NetworkState.CONNECTED
    CategorySessionCache.hasFetched = false

    every {
      application.getString(CoreR.string.no_network_connection)
    } returns errorMessage

    every {
      application.getString(string.no_category_available)
    } returns "No categories"

    every { connectivityBroadcastReceiver.action } returns "test"
    every { connectivityBroadcastReceiver.networkStates } returns networkStates

    every {
      kiwixDataStore.cachedOnlineCategoryList
    } returns categories

    every {
      kiwixDataStore.selectedOnlineContentCategory
    } returns flowOf("")

    coEvery {
      kiwixService.getCategories()
    } returns CategoryFeed()
  }

  private fun createViewModel() {
    categoryViewModel =
      CategoryViewModel(
        application,
        kiwixDataStore,
        kiwixService,
        connectivityBroadcastReceiver,
        mainDispatcher.dispatcher
      ).apply {
        setOnDismissCallback { }
      }
  }

  @Nested
  inner class Init {
    @Test
    fun registersReceiver_invokesOnInit() = runTest {
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
    fun whenOnClearInvoked_UnregistersBroadcastReceiver() {
      createViewModel()
      every { application.unregisterReceiver(any()) } returns mockk()
      categoryViewModel.onClearedExposed()
      verify {
        application.unregisterReceiver(connectivityBroadcastReceiver)
      }
    }

    @Test
    fun categoryState_initially_isLoading() = runTest {
      createViewModel()
      assertThat(categoryViewModel.state.value).isEqualTo(Loading)
    }
  }

  @Nested
  inner class ObserveActions {
    @Nested
    inner class Reduce {
      @Nested
      inner class Error {
        @Test
        fun errorAction_emitsErrorMessage() = runTest {
          createViewModel()
          advanceUntilIdle()

          categoryViewModel.actions.emit(Action.Error(errorMessage))

          advanceUntilIdle()

          assertEquals(
            State.Error(errorMessage),
            categoryViewModel.state.value
          )
        }
      }

      @Nested
      inner class UpdateCategory {
        @Test
        fun updateCategory_whenLoadingState_emitsContent() = runTest {
          createViewModel()
          advanceUntilIdle()

          val categories = listOf(createCategory())

          categoryViewModel.state.value = Loading
          categoryViewModel.actions.emit(UpdateCategory(categories))

          advanceUntilIdle()

          assertEquals(
            State.Content(categories),
            categoryViewModel.state.value
          )
        }

        @Test
        fun updateCategory_whenNotLoading_emitsCurrentState() = runTest {
          createViewModel()
          advanceUntilIdle()

          val categories = listOf(createCategory())
          val currentState = State.Error(errorMessage)

          categoryViewModel.state.value = currentState
          categoryViewModel.actions.emit(UpdateCategory(categories))

          advanceUntilIdle()

          assertEquals(
            currentState,
            categoryViewModel.state.value
          )
        }
      }

      @Nested
      inner class Filter {
        @Test
        fun filter_whenContentState_updatesContent() = runTest {
          createViewModel()
          advanceUntilIdle()

          val categories =
            listOf(
              createCategory(category = "Wikipedia"),
              createCategory(id = 2, category = "Gutenburg")
            )

          categoryViewModel.state.value = State.Content(categories)
          categoryViewModel.actions.emit(Action.Filter("Wiki"))

          advanceUntilIdle()

          val content = categoryViewModel.state.value as State.Content

          assertEquals("Wikipedia", content.items.first().category)
        }

        @Test
        fun filter_whenNotContentState_emitsCurrentState() = runTest {
          createViewModel()
          advanceUntilIdle()

          val currentState = State.Error(errorMessage)

          categoryViewModel.state.value = currentState
          categoryViewModel.actions.emit(Action.Filter("Wiki"))

          advanceUntilIdle()

          assertEquals(
            currentState,
            categoryViewModel.state.value
          )
        }
      }

      @Nested
      inner class Select {
        @Test
        fun select_whenContentState_emitsSaving() = runTest {
          createViewModel()
          advanceUntilIdle()

          val categories = listOf(createCategory(active = false))
          val content = State.Content(categories)

          categoryViewModel.state.value = content
          categoryViewModel.actions.emit(
            Action.Select(CategoryListItem.CategoryItem(categories.first()))
          )

          advanceUntilIdle()
          val expectedContent = content.select(
            CategoryListItem.CategoryItem(categories.first())
          )
          assertEquals(State.Saving(expectedContent), categoryViewModel.state.value)
        }

        @Test
        fun select_whenNotContentState_emitsCurrentState() = runTest {
          createViewModel()
          advanceUntilIdle()

          val currentState = State.Error(errorMessage)

          categoryViewModel.state.value = currentState
          categoryViewModel.actions.emit(
            Action.Select(CategoryListItem.CategoryItem(createCategory()))
          )

          advanceUntilIdle()

          assertEquals(currentState, categoryViewModel.state.value)
        }
      }
    }
  }

  @Nested
  inner class ObserveCategories {
    @Nested
    inner class SessionCache {
      @Test
      fun sessionCacheHasFetchedAndCachedCategoryListNotNullOrEmpty_emitsCachedData() = runTest {
        val categories = listOf(createCategory())

        CategorySessionCache.hasFetched = true

        every {
          kiwixDataStore.cachedOnlineCategoryList
        } returns MutableStateFlow(categories)

        createViewModel()
        advanceUntilIdle()

        assertEquals(
          State.Content(categories),
          categoryViewModel.state.value
        )
      }

      @Test
      fun sessionCacheHasFetchedAndCacheEmpty_continuesFlow() = runTest {
        CategorySessionCache.hasFetched = true

        every {
          kiwixDataStore.cachedOnlineCategoryList
        } returns MutableStateFlow(emptyList())

        createViewModel()
        advanceUntilIdle()

        assertEquals(
          State.Error("No categories"),
          categoryViewModel.state.value
        )
      }

      @Test
      fun sessionCacheHasFetchedAndCacheNull_continuesFlow() = runTest {
        CategorySessionCache.hasFetched = true

        every {
          kiwixDataStore.cachedOnlineCategoryList
        } returns MutableStateFlow(null)

        createViewModel()
        advanceUntilIdle()

        assertEquals(
          State.Error("No categories"),
          categoryViewModel.state.value
        )
      }
    }

    @Nested
    inner class Online {
      @Test
      fun online_whenApiReturnsCategories_emitsContent() = runTest {
        val entry = CategoryEntry().apply {
          title = "Wikipedia"
        }

        val feed = CategoryFeed().apply {
          entries = listOf(entry)
        }

        coEvery {
          kiwixService.getCategories()
        } returns feed

        coEvery {
          kiwixDataStore.saveOnlineCategoryList(any())
        } just Runs

        createViewModel()
        advanceUntilIdle()

        val expected = listOf(
          createCategory(id = 0, active = true, category = ""),
          createCategory(id = 1, category = "Wikipedia")
        )

        assertEquals(
          State.Content(expected),
          categoryViewModel.state.value
        )
      }

      @Test
      fun online_apiEmpty_cacheExists_emitsCache() = runTest {
        val categories = listOf(createCategory())

        every {
          kiwixDataStore.cachedOnlineCategoryList
        } returns MutableStateFlow(categories)

        createViewModel()
        advanceUntilIdle()

        assertEquals(
          State.Content(categories),
          categoryViewModel.state.value
        )
      }

      @Test
      fun online_apiEmpty_cacheEmpty_emitsNoCategory() = runTest {
        every {
          kiwixDataStore.cachedOnlineCategoryList
        } returns MutableStateFlow(emptyList())

        createViewModel()
        advanceUntilIdle()

        assertEquals(
          State.Error("No categories"),
          categoryViewModel.state.value
        )
      }

      @Test
      fun online_apiEmpty_cacheNull_emitsNoCategory() = runTest {
        every {
          kiwixDataStore.cachedOnlineCategoryList
        } returns MutableStateFlow(null)

        createViewModel()
        advanceUntilIdle()

        assertEquals(
          State.Error("No categories"),
          categoryViewModel.state.value
        )
      }
    }

    @Nested
    inner class Offline {
      @Test
      fun offline_cacheExists_emitsCache() = runTest {
        networkStates.value = NetworkState.NOT_CONNECTED

        val categories = listOf(createCategory())

        every {
          kiwixDataStore.cachedOnlineCategoryList
        } returns MutableStateFlow(categories)

        createViewModel()
        advanceUntilIdle()

        assertEquals(
          State.Content(categories),
          categoryViewModel.state.value
        )
      }

      @Test
      fun offline_cacheEmpty_emitsNoInternet() = runTest {
        networkStates.value = NetworkState.NOT_CONNECTED

        every {
          kiwixDataStore.cachedOnlineCategoryList
        } returns MutableStateFlow(emptyList())

        every {
          application.getString(
            CoreR.string.no_network_connection
          )
        } returns errorMessage

        createViewModel()
        advanceUntilIdle()

        assertEquals(
          State.Error(errorMessage),
          categoryViewModel.state.value
        )
      }

      @Test
      fun offline_cacheNull_emitsNoInternet() = runTest {
        networkStates.value = NetworkState.NOT_CONNECTED

        every {
          kiwixDataStore.cachedOnlineCategoryList
        } returns MutableStateFlow(null)

        every {
          application.getString(
            CoreR.string.no_network_connection
          )
        } returns errorMessage

        createViewModel()
        advanceUntilIdle()

        assertEquals(
          State.Error(errorMessage),
          categoryViewModel.state.value
        )
      }
    }
  }

  @Nested
  inner class OnDialogOpen() {
    @Test
    fun onDialogOpened_whenSavingState_restoresContent() {
      createViewModel()

      val content = State.Content(
        listOf(createCategory())
      )

      categoryViewModel.state.value = State.Saving(content)

      categoryViewModel.onDialogOpened()

      assertEquals(
        content,
        categoryViewModel.state.value
      )
    }

    @Test
    fun onDialogOpened_whenNotSavingState_doesNothing() {
      createViewModel()

      val content = State.Content(
        listOf(createCategory())
      )

      categoryViewModel.state.value = content

      categoryViewModel.onDialogOpened()

      assertEquals(
        content,
        categoryViewModel.state.value
      )
    }
  }
}
