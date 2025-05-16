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
import io.mockk.every
import io.mockk.mockk
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.PageDao
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.setScheduler

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantExecutorExtension::class)
internal class PageViewModelTest {
  private val pageDao: PageDao = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()

  private lateinit var viewModel: TestablePageViewModel
  private val testScheduler = TestScheduler()
  private val itemsFromDb: MutableSharedFlow<List<Page>> =
    MutableSharedFlow<List<Page>>(0)

  init {
    setScheduler(testScheduler)
    RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
  }

  @BeforeEach
  fun init() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    clearAllMocks()
    every { zimReaderContainer.id } returns "id"
    every { zimReaderContainer.name } returns "zimName"
    every { sharedPreferenceUtil.showHistoryAllBooks } returns true
    every { pageDao.pages() } returns itemsFromDb
    viewModel = TestablePageViewModel(zimReaderContainer, sharedPreferenceUtil, pageDao)
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is Initialising`() {
    // viewModel.state.test().assertValue(pageState())
  }

  @Test
  fun `Exit calls PopFragmentBackstack`() {
    // viewModel.effects.test().also { viewModel.actions.offer(Exit) }
    //   .assertValue(PopFragmentBackstack)
    // viewModel.state.test().assertValue(pageState())
  }

  @Test
  fun `ExitActionModeMenu calls deslectAllPages`() {
    // viewModel.actions.offer(ExitActionModeMenu)
    // viewModel.state.test().assertValue(TestablePageState(searchTerm = "deselectAllPagesCalled"))
  }

  @Test
  fun `UserClickedShowAllToggle calls offerUpdateToShowAllToggle`() {
    // val action = UserClickedShowAllToggle(true)
    // viewModel.actions.offer(action)
    // viewModel.state.test()
    //   .assertValue(TestablePageState(searchTerm = "offerUpdateToShowAllToggleCalled"))
  }

  @Test
  fun `UserClickedDeleteButton calls createDeletePageDialogEffect`() {
    // viewModel.actions.offer(UserClickedDeleteButton)
    // assertThat(viewModel.createDeletePageDialogEffectCalled).isEqualTo(true)
  }

  @Test
  fun `UserClickedDeleteSelectedPages calls createDeletePageDialogEffect`() {
    // viewModel.actions.offer(UserClickedDeleteSelectedPages)
    // assertThat(viewModel.createDeletePageDialogEffectCalled).isEqualTo(true)
  }

  @Test
  internal fun `OnItemClick selects item if one is selected`() {
    // val zimReaderSource: ZimReaderSource = mockk()
    // val page = PageImpl(isSelected = true, zimReaderSource = zimReaderSource)
    // viewModel.state.postValue(TestablePageState(listOf(page)))
    // viewModel.actions.offer(OnItemClick(page))
    // viewModel.state.test()
    //   .assertValue(TestablePageState(listOf(PageImpl(zimReaderSource = zimReaderSource))))
  }

  @Test
  internal fun `OnItemClick offers OpenPage if none is selected`() {
    // val zimReaderSource: ZimReaderSource = mockk()
    // viewModel.state.postValue(
    //   TestablePageState(listOf(PageImpl(zimReaderSource = zimReaderSource)))
    // )
    // viewModel.effects.test()
    //   .also { viewModel.actions.offer(OnItemClick(PageImpl(zimReaderSource = zimReaderSource))) }
    //   .assertValue(OpenPage(PageImpl(zimReaderSource = zimReaderSource), zimReaderContainer))
    // viewModel.state.test()
    //   .assertValue(TestablePageState(listOf(PageImpl(zimReaderSource = zimReaderSource))))
  }

  @Test
  internal fun `OnItemLongClick selects item if none is selected`() {
    // val zimReaderSource: ZimReaderSource = mockk()
    // val page = PageImpl(zimReaderSource = zimReaderSource)
    // viewModel.state.postValue(TestablePageState(listOf(page)))
    // viewModel.actions.offer(OnItemLongClick(page))
    // viewModel.state.test().assertValue(
    //   TestablePageState(
    //     listOf(
    //       PageImpl(
    //         isSelected = true,
    //         zimReaderSource = zimReaderSource
    //       )
    //     )
    //   )
    // )
  }

  @Test
  fun `Filter calls updatePagesBasedOnFilter`() {
    // viewModel.actions.offer(Filter("Called"))
    // viewModel.state.test()
    //   .assertValue(TestablePageState(searchTerm = "updatePagesBasedOnFilterCalled"))
  }

  @Test
  fun `UpdatePages calls updatePages`() {
    // viewModel.actions.offer(UpdatePages(emptyList()))
    // viewModel.state.test()
    //   .assertValue(TestablePageState(searchTerm = "updatePagesCalled"))
  }
}
