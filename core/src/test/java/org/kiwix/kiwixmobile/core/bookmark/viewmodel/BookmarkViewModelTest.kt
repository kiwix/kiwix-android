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

import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.ExitBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UpdateBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UserClickedDeleteButton
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UserClickedDeleteSelectedBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects.OpenBookmark
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects.ShowDeleteBookmarksDialog
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects.UpdateAllBookmarksPreference
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.Finish
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.setScheduler

@ExtendWith(InstantExecutorExtension::class)
internal class BookmarkViewModelTest {
  private val bookmarksDao: NewBookmarksDao = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()

  private lateinit var viewModel: BookmarkViewModel
  private val testScheduler = TestScheduler()

  private val itemsFromDb: PublishProcessor<List<BookmarkItem>> =
    PublishProcessor.create()

  init {
    setScheduler(testScheduler)
    RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
  }

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { zimReaderContainer.id } returns "id"
    every { zimReaderContainer.name } returns "zimName"
    every { sharedPreferenceUtil.showBookmarksAllBooks } returns true
    every { bookmarksDao.bookmarks() } returns itemsFromDb.distinctUntilChanged()
    viewModel = BookmarkViewModel(bookmarksDao, zimReaderContainer, sharedPreferenceUtil)
  }

  @Test
  fun `initial state is Initialising`() {
    viewModel.state.test().assertValue(bookmarkState())
  }

  @Test
  internal fun `ExitBookmarks finishes activity`() {
    viewModel.effects.test().also { viewModel.actions.offer(ExitBookmarks) }.assertValue(Finish)
    viewModel.state.test().assertValue(bookmarkState())
  }

  @Test
  internal fun `ExitActionModeMenu deselects bookmarks items`() {
    viewModel.state.postValue(bookmarkState(bookmarks = listOf(bookmark(isSelected = true))))
    viewModel.actions.offer(ExitActionModeMenu)
    viewModel.state.test().assertValue(
      bookmarkState(bookmarks = listOf(bookmark(isSelected = false)))
    )
  }

  @Test
  internal fun `UserClickedDeleteButton offers ShowDeleteBookmarkDialog`() {
    viewModel.effects.test().also { viewModel.actions.offer(UserClickedDeleteButton) }
      .assertValue(ShowDeleteBookmarksDialog(viewModel.effects, bookmarkState(), bookmarksDao))
    viewModel.state.test().assertValue(bookmarkState())
  }

  @Test
  internal fun `UserClickedDeleteSelectedBookmarks offers ShowDeleteBookmarksDialog`() {
    viewModel.effects.test().also { viewModel.actions.offer(UserClickedDeleteSelectedBookmarks) }
      .assertValue(ShowDeleteBookmarksDialog(viewModel.effects, bookmarkState(), bookmarksDao))
    viewModel.state.test().assertValue(bookmarkState())
  }

  @Test
  internal fun `UserClickedShowAllToggle offers UpdateAllBookmarksPreference`() {
    viewModel.effects.test()
      .also { viewModel.actions.offer(UserClickedShowAllToggle(false)) }
      .assertValue(UpdateAllBookmarksPreference(sharedPreferenceUtil, false))
    viewModel.state.test().assertValue(bookmarkState(showAll = false))
  }

  @Test
  internal fun `OnItemClick selects item if one is selected`() {
    val bookmark = bookmark(isSelected = true)
    viewModel.state.postValue(bookmarkState(listOf(bookmark)))
    viewModel.actions.offer(OnItemClick(bookmark))
    viewModel.state.test().assertValue(bookmarkState(listOf(bookmark())))
  }

  @Test
  internal fun `OnItemClick offers OpenBookmark if none is selected`() {
    viewModel.state.postValue(bookmarkState(listOf(bookmark())))
    viewModel.effects.test().also { viewModel.actions.offer(OnItemClick(bookmark())) }
      .assertValue(OpenBookmark(bookmark(), zimReaderContainer))
    viewModel.state.test().assertValue(bookmarkState(listOf(bookmark())))
  }

  @Test
  internal fun `OnItemLongClick selects item if none is selected`() {
    val bookmark = bookmark()
    viewModel.state.postValue(bookmarkState(listOf(bookmark)))
    viewModel.actions.offer(OnItemLongClick(bookmark))
    viewModel.state.test().assertValue(bookmarkState(listOf(bookmark(isSelected = true))))
  }

  @Test
  fun `Filter updates search term`() {
    val searchTerm = "searchTerm"
    viewModel.actions.offer(Filter(searchTerm))
    viewModel.state.test().assertValue(bookmarkState(searchTerm = searchTerm))
  }

  @Test
  internal fun `UpdateBookmarks updates bookmarks`() {
    viewModel.actions.offer(UpdateBookmarks(listOf(bookmark())))
    viewModel.state.test().assertValue(bookmarkState(listOf(bookmark())))
  }
}
