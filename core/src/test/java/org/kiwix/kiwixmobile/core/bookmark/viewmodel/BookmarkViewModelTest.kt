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

package org.kiwix.kiwixmobile.core.bookmark.viewmodel

import DeleteSelectedOrAllBookmarkItems
import OpenBookmark
import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import junit.framework.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State.NoResults
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State.SelectionResults
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects.ToggleShowAllBookmarksSwitchAndSaveItsStateToPrefs
import org.kiwix.kiwixmobile.core.data.Repository
import org.kiwix.kiwixmobile.core.history.viewmodel.effects.ShowDeleteBookmarkDialog
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.Finish
import org.kiwix.kiwixmobile.core.utils.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.setScheduler
import java.util.concurrent.TimeUnit

@ExtendWith(InstantExecutorExtension::class)
internal class BookmarkViewModelTest {
  private val bookmarksRepository: Repository = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()

  lateinit var viewModel: BookmarkViewModel
  private val testScheduler = TestScheduler()

  private var latestItems: List<BookmarkItem> = listOf()

  init {
    setScheduler(testScheduler)
  }

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { zimReaderContainer.id } returns "id"
    every { zimReaderContainer.name } returns "zimName"
    every { sharedPreferenceUtil.showBookmarksAllBooks } returns true
    every { bookmarksRepository.getBookmarks(any()) } returns Single.just(latestItems)
    viewModel = BookmarkViewModel(bookmarksRepository, zimReaderContainer, sharedPreferenceUtil)
  }

  private fun resultsIn(st: State) {
    viewModel.state.test()
      .also { testScheduler.advanceTimeBy(100, TimeUnit.MILLISECONDS) }
      .assertValue(st)
  }

  private fun emissionOf(searchTerm: String, databaseResults: List<BookmarkItem>) {
    latestItems = databaseResults
    every { bookmarksRepository.getBookmarks(true) } returns Single.just(latestItems)
    viewModel.actions.offer(Filter(searchTerm))
  }

  @Nested
  inner class StateTests {

    @Test
    fun `initial state is Initialising`() {
      viewModel.state.test().assertValue(NoResults(listOf()))
    }

    @Test
    fun `non empty search term with search results shows Results`() {
      val searchTerm = "searchTerm"
      val item = createSimpleBookmarkItem("searchTermTitle")
      emissionOf(
        searchTerm = searchTerm,
        databaseResults = listOf(item)
      )
      resultsIn(Results((listOf(item))))
    }

    @Test
    fun `non empty search string with no search results is NoResults`() {
      emissionOf(
        searchTerm = "a",
        databaseResults = listOf(
          createSimpleBookmarkItem(
            ""
          )
        )
      )
      resultsIn(NoResults(emptyList()))
    }

    @Test
    fun `empty search string with database results shows Results`() {
      val item = createSimpleBookmarkItem()
      emissionOf(searchTerm = "", databaseResults = listOf(item))
      resultsIn(Results(listOf(item)))
    }

    @Test
    fun `empty search string with no database results is NoResults`() {
      emissionOf(
        searchTerm = "",
        databaseResults = emptyList()
      )
      resultsIn(NoResults(emptyList()))
    }

    @Test
    fun `only latest search term is used`() {
      val item =
        createSimpleBookmarkItem("b")
      emissionOf(
        searchTerm = "a",
        databaseResults = emptyList()
      )
      emissionOf(
        searchTerm = "b",
        databaseResults = listOf(item)
      )
      resultsIn(Results(listOf(item)))
    }

    @Test
    fun `enters selection state if item is selected`() {
      val item =
        createSimpleBookmarkItem(
          "b", isSelected = true
        )
      emissionOf(
        searchTerm = "a",
        databaseResults = emptyList()
      )
      emissionOf(
        searchTerm = "b",
        databaseResults = listOf(item)
      )
      resultsIn(SelectionResults(listOf(item)))
    }

    @Test
    fun `OnItemLongClick enters selection state`() {
      val item1 =
        createSimpleBookmarkItem(
          "a"
        )
      emissionOf(
        searchTerm = "",
        databaseResults = listOf(item1)
      )
      viewModel.actions.offer(Action.OnItemLongClick(item1))
      item1.isSelected = true
      resultsIn(SelectionResults(listOf(item1)))
    }

    @Test
    fun `Deselection via OnItemClick exits selection state if last item is deselected`() {
      val item1 = createSimpleBookmarkItem("a")
      val item2 = createSimpleBookmarkItem("a")
      emissionOf(searchTerm = "", databaseResults = listOf(item1, item2))
      viewModel.actions.offer(Action.OnItemLongClick(item1))
      viewModel.actions.offer(Action.OnItemClick(item1))
      resultsIn(Results(listOf(item1, item2)))
    }

    @Test
    fun `Deselection via OnItemLongClick exits selection state if last item is deselected`() {
      val item1 = createSimpleBookmarkItem("a")
      val item2 = createSimpleBookmarkItem("a")
      emissionOf(searchTerm = "", databaseResults = listOf(item1, item2))
      viewModel.actions.offer(Action.OnItemLongClick(item1))
      viewModel.actions.offer(Action.OnItemLongClick(item1))
      resultsIn(Results(listOf(item1, item2)))
    }

    @Test
    fun `ExitActionMode deselects all items`() {
      val item1 = createSimpleBookmarkItem("a", isSelected = true)
      val item2 = createSimpleBookmarkItem("a", isSelected = true)
      emissionOf(searchTerm = "", databaseResults = listOf(item1, item2))
      viewModel.actions.offer(Action.ExitActionModeMenu)
      item1.isSelected = false
      item2.isSelected = false
      resultsIn(Results(listOf(item1, item2)))
    }
  }

  @Nested
  inner class ActionMapping {
    @Test
    fun `ExitedSearch offers Finish`() {
      actionResultsInEffects(Action.ExitBookmarks, Finish)
    }

    @Test
    fun `ExitActionModeMenu deselects all history items from state`() {
      val item1 = createSimpleBookmarkItem("a", isSelected = true)
      emissionOf(searchTerm = "", databaseResults = listOf(item1))
      viewModel.actions.offer(Action.ExitActionModeMenu)
      assertItemIsDeselected(item1)
    }

    @Test
    fun `OnItemLongClick selects history item from state`() {
      val item1 = createSimpleBookmarkItem("a")
      emissionOf(searchTerm = "", databaseResults = listOf(item1))
      viewModel.actions.offer(Action.OnItemLongClick(item1))
      assertItemIsSelected(item1)
    }

    @Test
    fun `OnItemLongClick selects history item from state if in SelectionMode`() {
      val item1 = createSimpleBookmarkItem("a", id = 2)
      val item2 = createSimpleBookmarkItem("a", id = 3)
      emissionOf(searchTerm = "", databaseResults = listOf(item1, item2))
      viewModel.actions.offer(Action.OnItemLongClick(item1))
      viewModel.actions.offer(Action.OnItemLongClick(item2))
      assertItemIsSelected(item1)
      assertItemIsSelected(item2)
    }

    @Test
    fun `OnItemLongClick deselects history item from state if in SelectionMode`() {
      val item1 = createSimpleBookmarkItem("a", id = 2)
      emissionOf(searchTerm = "", databaseResults = listOf(item1))
      viewModel.actions.offer(Action.OnItemLongClick(item1))
      viewModel.actions.offer(Action.OnItemLongClick(item1))
      assertItemIsDeselected(item1)
    }

    @Test
    fun `OnItemClick selects history item from state if in SelectionMode`() {
      val item1 = createSimpleBookmarkItem("a", id = 2)
      val item2 = createSimpleBookmarkItem("a", id = 3)
      emissionOf(searchTerm = "", databaseResults = listOf(item1, item2))
      viewModel.actions.offer(Action.OnItemLongClick(item1))
      viewModel.actions.offer(Action.OnItemClick(item2))
      assertItemIsSelected(item1)
      assertItemIsSelected(item2)
    }

    @Test
    fun `OnItemClick offers OpenHistoryItem if not in selection mode `() {
      val item1 = createSimpleBookmarkItem("a", id = 2)
      emissionOf(searchTerm = "", databaseResults = listOf(item1))
      actionResultsInEffects(Action.OnItemClick(item1), OpenBookmark(item1, zimReaderContainer))
    }

    @Test
    fun `ToggleShowHistoryFromAllBooks switches show all books toggle`() {
      actionResultsInEffects(
        Action.ToggleShowBookmarksFromAllBooks(true),
        ToggleShowAllBookmarksSwitchAndSaveItsStateToPrefs(
          viewModel.showAllSwitchToggle,
          sharedPreferenceUtil,
          true
        )
      )
    }

    @Test
    fun `RequestDeleteAllHistoryItems opens dialog to request deletion`() {
      actionResultsInEffects(
        Action.RequestDeleteAllBookmarks,
        ShowDeleteBookmarkDialog(viewModel.actions, KiwixDialog.DeleteBookmarks)
      )
    }

    @Test
    fun `RequestDeleteSelectedBookmarks opens dialog to request deletion`() {
      actionResultsInEffects(
        Action.RequestDeleteSelectedBookmarks,
        ShowDeleteBookmarkDialog(viewModel.actions, KiwixDialog.DeleteBookmarks)
      )
    }

    @Test
    fun `DeleteHistoryItems calls DeleteSelectedOrAllHistoryItems side effect`() {
      actionResultsInEffects(
        Action.DeleteBookmarks,
        DeleteSelectedOrAllBookmarkItems(viewModel.state, bookmarksRepository, viewModel.actions)
      )
    }

    private fun actionResultsInEffects(action: Action, vararg effects: SideEffect<*>) {
      viewModel.effects.test().also { viewModel.actions.offer(action) }.assertValues(*effects)
    }

    private fun assertItemIsDeselected(item: BookmarkItem) {
      Assert.assertFalse(
        viewModel.state.value?.bookmarkItems?.find {
          it.databaseId == item.databaseId
        }?.isSelected == true
      )
    }

    private fun assertItemIsSelected(item: BookmarkItem) {
      Assert.assertTrue(
        viewModel.state.value?.bookmarkItems?.find {
          it.databaseId == item.databaseId
        }?.isSelected == true
      )
    }
  }
}
