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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.data.remote.CategoryEntry
import org.kiwix.kiwixmobile.core.data.remote.CategoryFeed
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.language.viewmodel.flakyTest
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.Action.Select
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.Action.UpdateCategory
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryListItem
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategorySessionCache
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.CategoryViewModel
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.SaveCategoryAndFinish
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Content
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.State.Loading
import org.kiwix.kiwixmobile.zimManager.TURBINE_TIMEOUT
import org.kiwix.kiwixmobile.zimManager.testFlow
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.category

fun categoryItem(category: Category = category()) =
  CategoryListItem.CategoryItem(category)

@ExtendWith(InstantExecutorExtension::class)
class CategoryViewModelTest {
  private val application: Application = mockk()
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
  private val kiwixService: KiwixService = mockk()
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()
  private val networkStates = MutableStateFlow(NetworkState.CONNECTED)
  private lateinit var categoryViewModel: CategoryViewModel
  private var categories: MutableStateFlow<List<Category>?> = MutableStateFlow(null)

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { application.getString(any()) } returns ""
    every { connectivityBroadcastReceiver.action } returns "test"
    every { connectivityBroadcastReceiver.networkStates } returns networkStates
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      every { application.registerReceiver(any(), any(), any()) } returns mockk()
    } else {
      @Suppress("UnspecifiedRegisterReceiverFlag")
      every { application.registerReceiver(any(), any()) } returns mockk()
    }
    categories.value = null
    networkStates.value = NetworkState.CONNECTED
    CategorySessionCache.hasFetched = true
    every { sharedPreferenceUtil.getCachedCategoryList() } returns categories.value
    every { sharedPreferenceUtil.selectedOnlineContentCategory } returns "wikipedia"
    categoryViewModel =
      CategoryViewModel(
        application,
        sharedPreferenceUtil,
        kiwixService,
        connectivityBroadcastReceiver
      )
    runBlocking { categoryViewModel.state.emit(Loading) }
  }

  @Nested
  inner class Context {
    @Test
    fun `registers broadcastReceiver in init`() {
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
      every { application.unregisterReceiver(any()) } returns mockk()
      categoryViewModel.onClearedExposed()
      verify {
        application.unregisterReceiver(connectivityBroadcastReceiver)
      }
    }
  }

  @Test
  fun `initial state is Loading`() = runTest {
    testFlow(
      flow = categoryViewModel.state,
      triggerAction = {},
      assert = { assertThat(awaitItem()).isEqualTo(Loading) }
    )
  }

  @Test
  fun `an empty categories emission does not send update action`() = runTest {
    testFlow(
      categoryViewModel.actions,
      triggerAction = { categories.emit(listOf()) },
      assert = { expectNoEvents() }
    )
  }

  @Test
  fun `observeCategories uses network when no cache and online`() = flakyTest {
    runTest {
      every { application.getString(any()) } returns ""
      val fetchedLanguages = listOf(category(category = "wikipedia"))
      CategorySessionCache.hasFetched = false
      categories.value = emptyList()

      every { sharedPreferenceUtil.getCachedLanguageList() } returns null
      coEvery { kiwixService.getCategories() } returns CategoryFeed().apply {
        entries = fetchedLanguages.map {
          CategoryEntry().apply {
            title = "wikipedia"
            id = "0"
            updated = ""
            content = ""
            link = null
          }
        }
      }
      every { sharedPreferenceUtil.selectedOnlineContentCategory } returns ""
      every { sharedPreferenceUtil.saveCategoryList(any()) } just Runs

      testFlow(
        categoryViewModel.actions,
        triggerAction = {},
        assert = {
          val result = awaitItem()
          assertThat(result).isInstanceOf(UpdateCategory::class.java)
          verify { sharedPreferenceUtil.saveCategoryList(any()) }
        }
      )
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Save uses active category`() = flakyTest {
    runTest {
      every { application.getString(any()) } returns ""
      val activeCategory = category(category = "wikipedia").copy(active = true)
      val inactiveCategory = category(category = "gutenburg").copy(active = false)
      val categoryItem = CategoryListItem.CategoryItem(activeCategory)
      categoryViewModel.effects.test {
        categoryViewModel.state.value = Content(listOf(activeCategory, inactiveCategory))
        categoryViewModel.actions.emit(Select(categoryItem))
        advanceUntilIdle()
        advanceTimeBy(100)
        val effect = awaitItem() as SaveCategoryAndFinish
        assertThat(effect.category).isEqualTo(activeCategory)
      }
    }
  }

  @Test
  fun `UpdateCategory Action changes state to Content when Loading`() = runTest {
    every { application.getString(any()) } returns ""
    testFlow(
      categoryViewModel.state,
      triggerAction = { categoryViewModel.actions.emit(UpdateCategory(listOf())) },
      assert = {
        assertThat(awaitItem()).isEqualTo(Loading)
        assertThat(awaitItem()).isEqualTo(Content(listOf()))
      },
      TURBINE_TIMEOUT
    )
  }

  @Test
  fun `UpdateCategory Action has no effect on other states`() = runTest {
    testFlow(
      categoryViewModel.state,
      triggerAction = {
        categoryViewModel.actions.emit(UpdateCategory(listOf()))
        categoryViewModel.actions.emit(UpdateCategory(listOf()))
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(Loading)
        assertThat(awaitItem()).isEqualTo(Content(listOf()))
      }
    )
  }

  @Test
  fun `Filter Action updates Content state `() = runTest {
    every { application.getString(any()) } returns ""
    categories.value = listOf()
    testFlow(
      categoryViewModel.state,
      triggerAction = {
        categoryViewModel.actions.tryEmit(UpdateCategory(listOf()))
        categoryViewModel.actions.tryEmit(Filter("filter"))
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(Loading)
        assertThat(awaitItem()).isEqualTo(Content(items = listOf(), filter = ""))
        assertThat(awaitItem()).isEqualTo(Content(listOf(), filter = "filter"))
      }
    )
  }

  @Test
  fun `Filter Action has no effect on other states`() = runTest {
    every { application.getString(any()) } returns ""
    testFlow(
      categoryViewModel.state,
      triggerAction = { categoryViewModel.actions.emit(Filter("")) },
      assert = {
        assertThat(awaitItem()).isEqualTo(Loading)
      }
    )
  }

  @Test
  fun `Select Action updates Content state`() = runTest {
    val categoriesList = listOf(category())
    categories.value = categoriesList
    testFlow(
      categoryViewModel.state,
      triggerAction = {
        categoryViewModel.actions.emit(UpdateCategory(categoriesList))
        categoryViewModel.actions.emit(Select(categoryItem()))
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(Loading)
        assertThat(awaitItem()).isEqualTo(Content(listOf(category())))
        assertThat(awaitItem()).isEqualTo(State.Saving)
      }
    )
  }

  @Test
  fun `Select Action has no effect on other states`() = runTest {
    testFlow(
      categoryViewModel.state,
      triggerAction = { categoryViewModel.actions.emit(Select(categoryItem())) },
      assert = {
        assertThat(awaitItem()).isEqualTo(Loading)
      }
    )
  }
}
