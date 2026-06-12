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
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.R.string
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.nav.destination.library.online.helper.ObserveCategories
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel.Action
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel.Action.UpdateCategory
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Loading
import org.kiwix.sharedFunctions.MainDispatcherRule
import org.kiwix.kiwixmobile.core.R as CoreR

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelTest {
  private val application: Application = mockk(relaxed = true)
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val observeCategories: ObserveCategories = mockk()
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()

  @RegisterExtension
  @JvmField
  val mainDispatcher = MainDispatcherRule()
  private val networkStates = MutableStateFlow(NetworkState.CONNECTED)
  private lateinit var categoryViewModel: CategoryViewModel

  private val errorMessage = "No Internet Connection"
  private fun createCategory(
    id: Long = 1,
    active: Boolean = false,
    category: String = "Wikipedia"
  ) = Category(id, active, category)

  @BeforeEach
  fun init() {
    networkStates.value = NetworkState.CONNECTED

    every {
      application.getString(CoreR.string.no_network_connection)
    } returns errorMessage

    every {
      application.getString(string.no_category_available)
    } returns "No categories"

    every { connectivityBroadcastReceiver.action } returns "test"
    every { connectivityBroadcastReceiver.networkStates } returns networkStates
    every { kiwixDataStore.selectedOnlineContentCategory } returns flowOf("")
  }

  private fun createViewModel() {
    categoryViewModel =
      CategoryViewModel(
        application,
        kiwixDataStore,
        observeCategories,
        connectivityBroadcastReceiver
      ).apply {
        setOnDismissCallback { }
      }
  }

  @Nested
  inner class Init {
    @Test
    fun registersReceiver_invokesOnInit() = runTest {
      coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
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
      coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
      createViewModel()
      every { application.unregisterReceiver(any()) } returns mockk()
      categoryViewModel.onClearedExposed()
      verify {
        application.unregisterReceiver(connectivityBroadcastReceiver)
      }
    }

    @Test
    fun categoryState_initially_isLoading() = runTest {
      coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
      createViewModel()
      assertThat(categoryViewModel.state.value).isEqualTo(Loading)
    }
  }

  @Nested
  inner class ObserveCategoriesTests {
    @Test
    fun whenObserveCategoriesReturnsSuccess_emitsContent() = runTest {
      val categoriesList = listOf(createCategory())
      coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(categoriesList)

      createViewModel()
      advanceUntilIdle()

      assertEquals(
        State.Content(categoriesList),
        categoryViewModel.state.value
      )
    }

    @Test
    fun whenObserveCategoriesReturnsError_emitsError() = runTest {
      coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Error(errorMessage)

      createViewModel()
      advanceUntilIdle()

      assertEquals(
        State.Error(errorMessage),
        categoryViewModel.state.value
      )
    }
  }

  @Nested
  inner class OnDialogOpen {
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

  @Nested
  inner class ObserveActions {
    @Nested
    inner class Reduce {
      @Nested
      inner class Error {
        @Test
        fun errorAction_emitsErrorMessage() = runTest {
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
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
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
          createViewModel()
          advanceUntilIdle()

          val categoriesList = listOf(createCategory())

          categoryViewModel.state.value = Loading
          categoryViewModel.actions.emit(UpdateCategory(categoriesList))

          advanceUntilIdle()

          assertEquals(
            State.Content(categoriesList),
            categoryViewModel.state.value
          )
        }

        @Test
        fun updateCategory_whenNotLoading_emitsCurrentState() = runTest {
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
          createViewModel()
          advanceUntilIdle()

          val categoriesList = listOf(createCategory())
          val currentState = State.Error(errorMessage)

          categoryViewModel.state.value = currentState
          categoryViewModel.actions.emit(UpdateCategory(categoriesList))

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
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
          createViewModel()
          advanceUntilIdle()

          val categoriesList =
            listOf(
              createCategory(category = "Wikipedia"),
              createCategory(id = 2, category = "Gutenburg")
            )

          categoryViewModel.state.value = State.Content(categoriesList)
          categoryViewModel.actions.emit(Action.Filter("Wiki"))

          advanceUntilIdle()

          val content = categoryViewModel.state.value as State.Content

          assertEquals("Wikipedia", content.items.first().category)
        }

        @Test
        fun filter_whenNotContentState_emitsCurrentState() = runTest {
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
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
        fun select_whenContentState_togglesActiveState() = runTest {
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
          createViewModel()
          advanceUntilIdle()

          val wikipedia = createCategory(active = false)
          val gutenberg = createCategory(id = 2, active = true, category = "Gutenberg")

          categoryViewModel.state.value = State.Content(listOf(wikipedia, gutenberg))
          categoryViewModel.actions.emit(
            Action.Select(CategoryListItem.CategoryItem(wikipedia))
          )

          advanceUntilIdle()

          val content = categoryViewModel.state.value as State.Content
          assertTrue(content.items.first { it.id == wikipedia.id }.active)
          assertTrue(content.items.first { it.id == gutenberg.id }.active)
        }

        @Test
        fun select_whenNotContentState_emitsCurrentState() = runTest {
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
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

      @Nested
      inner class ClearAll {
        @Test
        fun clearAll_whenContentState_clearsAllSelections() = runTest {
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
          createViewModel()
          advanceUntilIdle()

          val wikipedia = createCategory(active = true)
          val gutenberg = createCategory(id = 2, active = true, category = "Gutenberg")

          categoryViewModel.state.value = State.Content(listOf(wikipedia, gutenberg))
          categoryViewModel.actions.emit(Action.ClearAll)

          advanceUntilIdle()

          val content = categoryViewModel.state.value as State.Content
          assertTrue(content.items.none { it.active })
        }

        @Test
        fun clearAll_whenNotContentState_emitsCurrentState() = runTest {
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
          createViewModel()
          advanceUntilIdle()

          val currentState = State.Error(errorMessage)

          categoryViewModel.state.value = currentState
          categoryViewModel.actions.emit(Action.ClearAll)

          advanceUntilIdle()

          assertEquals(currentState, categoryViewModel.state.value)
        }
      }

      @Nested
      inner class SelectAll {
        @Test
        fun selectAll_whenContentState_selectAllSelections() = runTest {
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
          createViewModel()
          advanceUntilIdle()

          val wikipedia = createCategory(active = false)
          val gutenberg = createCategory(id = 2, active = false, category = "Gutenberg")

          categoryViewModel.state.value = State.Content(listOf(wikipedia, gutenberg))
          categoryViewModel.actions.emit(Action.SelectAll)

          advanceUntilIdle()

          val content = categoryViewModel.state.value as State.Content
          assertTrue(content.items.all { it.active })
        }

        @Test
        fun selectAll_whenNotContentState_emitsCurrentState() = runTest {
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
          createViewModel()
          advanceUntilIdle()

          val currentState = State.Error(errorMessage)

          categoryViewModel.state.value = currentState
          categoryViewModel.actions.emit(Action.SelectAll)

          advanceUntilIdle()

          assertEquals(currentState, categoryViewModel.state.value)
        }
      }

      @Nested
      inner class Cancel {
        @Test
        fun cancel_whenContentState_emitsCancelSideEffect() = runTest {
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
          createViewModel()
          advanceUntilIdle()

          categoryViewModel.state.value = State.Content(listOf(createCategory()))

          var sideEffect: SideEffect<*>? = null
          val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            categoryViewModel.effects.collect {
              sideEffect = it
            }
          }

          categoryViewModel.actions.emit(Action.Cancel)
          advanceUntilIdle()

          assertThat(sideEffect).isNotNull
          collectJob.cancel()
        }

        @Test
        fun cancel_whenNotContentState_emitsCurrentState() = runTest {
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
          createViewModel()
          advanceUntilIdle()

          val currentState = State.Error(errorMessage)

          categoryViewModel.state.value = currentState
          categoryViewModel.actions.emit(Action.Cancel)

          advanceUntilIdle()

          assertEquals(currentState, categoryViewModel.state.value)
        }
      }

      @Nested
      inner class Save {
        @Test
        fun save_whenContentState_returnsSavingAndEmitsSideEffect() = runTest {
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
          createViewModel()
          advanceUntilIdle()

          val wikipedia = createCategory(active = true)
          val gutenberg = createCategory(id = 2, active = false, category = "Gutenberg")

          categoryViewModel.state.value = State.Content(listOf(wikipedia, gutenberg))

          var sideEffect: SideEffect<*>? = null
          val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            categoryViewModel.effects.collect {
              sideEffect = it
            }
          }

          categoryViewModel.actions.emit(Action.Save)
          advanceUntilIdle()

          assertThat(categoryViewModel.state.value).isInstanceOf(State.Saving::class.java)

          assertThat(sideEffect).isInstanceOf(SaveCategoryAndFinish::class.java)
          val saveEffect = sideEffect as SaveCategoryAndFinish
          assertThat(saveEffect.categories.map { it.category }).containsExactly("Wikipedia")

          collectJob.cancel()
        }

        @Test
        fun save_whenNotContentState_emitsCurrentState() = runTest {
          coEvery { observeCategories(any(), any()) } returns ObserveCategories.Result.Success(emptyList())
          createViewModel()
          advanceUntilIdle()

          val currentState = State.Error(errorMessage)

          categoryViewModel.state.value = currentState
          categoryViewModel.actions.emit(Action.Save)

          advanceUntilIdle()

          assertEquals(currentState, categoryViewModel.state.value)
        }
      }
    }
  }
}
