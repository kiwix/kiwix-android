/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.page.viewmodel

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.PageDao
import org.kiwix.kiwixmobile.core.page.PageImpl
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.pageState
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UpdatePages
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Exit
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedDeleteButton
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UserClickedDeleteSelectedPages
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.page.viewmodel.effects.OpenPage
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.PopFragmentBackstack
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.files.testFlow
import org.kiwix.sharedFunctions.InstantExecutorExtension

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantExecutorExtension::class)
internal class PageViewModelTest {
  private val pageDao: PageDao = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()

  private lateinit var viewModel: TestablePageViewModel
  private val itemsFromDb: MutableSharedFlow<List<Page>> =
    MutableSharedFlow<List<Page>>(0)

  @BeforeEach
  fun init() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    clearAllMocks()
    every { zimReaderContainer.id } returns "id"
    every { zimReaderContainer.name } returns "zimName"
    coEvery { kiwixDataStore.showHistoryOfAllBooks } returns flowOf(true)
    every { pageDao.pages() } returns itemsFromDb
    viewModel = TestablePageViewModel(zimReaderContainer, kiwixDataStore, pageDao)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is Initialising`() = runTest {
    testFlow(
      flow = viewModel.state,
      triggerAction = {},
      assert = { assertThat(awaitItem()).isEqualTo(pageState()) }
    )
  }

  @Test
  fun `Exit calls PopFragmentBackstack`() = runTest {
    testFlow(
      flow = viewModel.effects,
      triggerAction = { viewModel.actions.tryEmit(Exit) },
      assert = { assertThat(awaitItem()).isEqualTo(PopFragmentBackstack) }
    )
    testFlow(
      flow = viewModel.state,
      triggerAction = {},
      assert = { assertThat(awaitItem()).isEqualTo(pageState()) }
    )
  }

  @Test
  fun `ExitActionModeMenu calls deslectAllPages`() = runTest {
    testFlow(
      flow = viewModel.state,
      triggerAction = { viewModel.actions.tryEmit(ExitActionModeMenu) },
      assert = {
        assertThat(awaitItem()).isEqualTo(TestablePageState(searchTerm = ""))
        assertThat(awaitItem())
          .isEqualTo(TestablePageState(searchTerm = "deselectAllPagesCalled"))
      }
    )
  }

  @Test
  fun `UserClickedShowAllToggle calls offerUpdateToShowAllToggle`() = runTest {
    testFlow(
      flow = viewModel.state,
      triggerAction = {
        viewModel.actions.tryEmit(UserClickedShowAllToggle(true))
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(TestablePageState(searchTerm = ""))
        assertThat(awaitItem())
          .isEqualTo(TestablePageState(searchTerm = "offerUpdateToShowAllToggleCalled"))
      }
    )
  }

  @Test
  fun `UserClickedDeleteButton calls createDeletePageDialogEffect`() = runTest {
    viewModel.actions.tryEmit(UserClickedDeleteButton)
    advanceUntilIdle()
    assertThat(viewModel.createDeletePageDialogEffectCalled).isEqualTo(true)
  }

  @Test
  fun `UserClickedDeleteSelectedPages calls createDeletePageDialogEffect`() = runTest {
    viewModel.actions.tryEmit(UserClickedDeleteSelectedPages)
    advanceUntilIdle()
    assertThat(viewModel.createDeletePageDialogEffectCalled).isEqualTo(true)
  }

  @Test
  internal fun `OnItemClick selects item if one is selected`() = runTest {
    val zimReaderSource: ZimReaderSource = mockk()
    testFlow(
      viewModel.state,
      triggerAction = {
        val page = PageImpl(isSelected = true, zimReaderSource = zimReaderSource)
        viewModel.getMutableStateForTestCases().value = TestablePageState(listOf(page))
        viewModel.actions.tryEmit(OnItemClick(page))
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(TestablePageState())
        assertThat(awaitItem())
          .isEqualTo(
            TestablePageState(
              listOf(PageImpl(zimReaderSource = zimReaderSource))
            )
          )
      }
    )
  }

  @Test
  internal fun `OnItemClick offers OpenPage if none is selected`() = runTest {
    val zimReaderSource: ZimReaderSource = mockk()
    testFlow(
      viewModel.effects,
      triggerAction = {
        viewModel.getMutableStateForTestCases().value =
          TestablePageState(listOf(PageImpl(zimReaderSource = zimReaderSource)))
        viewModel.actions.tryEmit(OnItemClick(PageImpl(zimReaderSource = zimReaderSource)))
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(
          OpenPage(
            PageImpl(zimReaderSource = zimReaderSource),
            zimReaderContainer
          )
        )
      }
    )
    testFlow(
      viewModel.state,
      triggerAction = {
        viewModel.getMutableStateForTestCases().value =
          TestablePageState(listOf(PageImpl(zimReaderSource = zimReaderSource)))
        viewModel.actions.tryEmit(OnItemClick(PageImpl(zimReaderSource = zimReaderSource)))
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(
          TestablePageState(listOf(PageImpl(zimReaderSource = zimReaderSource)))
        )
      }
    )
  }

  @Test
  internal fun `OnItemLongClick selects item if none is selected`() = runTest {
    val zimReaderSource: ZimReaderSource = mockk()
    val page = PageImpl(zimReaderSource = zimReaderSource)
    testFlow(
      viewModel.state,
      triggerAction = {
        viewModel.getMutableStateForTestCases().value = TestablePageState(listOf(page))
        viewModel.actions.tryEmit(OnItemLongClick(page))
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(TestablePageState())
        assertThat(awaitItem()).isEqualTo(
          TestablePageState(
            listOf(
              PageImpl(
                isSelected = true,
                zimReaderSource = zimReaderSource
              )
            )
          )
        )
      }
    )
  }

  @Test
  fun `Filter calls updatePagesBasedOnFilter`() = runTest {
    testFlow(
      viewModel.state,
      triggerAction = { viewModel.actions.tryEmit(Filter("Called")) },
      assert = {
        assertThat(awaitItem()).isEqualTo(TestablePageState())
        assertThat(awaitItem()).isEqualTo(TestablePageState(searchTerm = "updatePagesBasedOnFilterCalled"))
      }
    )
  }

  @Test
  fun `UpdatePages calls updatePages`() = runTest {
    testFlow(
      viewModel.state,
      triggerAction = { viewModel.actions.tryEmit(UpdatePages(emptyList())) },
      assert = {
        assertThat(awaitItem()).isEqualTo(TestablePageState())
        assertThat(awaitItem()).isEqualTo(TestablePageState(searchTerm = "updatePagesCalled"))
      }
    )
  }
}
