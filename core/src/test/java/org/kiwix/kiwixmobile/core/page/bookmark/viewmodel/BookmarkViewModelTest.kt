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

package org.kiwix.kiwixmobile.core.page.bookmark.viewmodel

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.bookmark
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.effects.ShowDeleteBookmarksDialog
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.effects.UpdateAllBookmarksPreference
import org.kiwix.kiwixmobile.core.page.bookmarkState
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UpdatePages
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.setScheduler

@ExtendWith(InstantExecutorExtension::class)
internal class BookmarkViewModelTest {
  private val libkiwixBookMarks: LibkiwixBookmarks = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()

  private lateinit var viewModel: BookmarkViewModel

  private val itemsFromDb: PublishProcessor<List<Page>> =
    PublishProcessor.create()

  init {
    setScheduler(Schedulers.trampoline())
    RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }
  }

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { zimReaderContainer.id } returns "id"
    every { zimReaderContainer.name } returns "zimName"
    every { sharedPreferenceUtil.showBookmarksAllBooks } returns true
    every { libkiwixBookMarks.bookmarks() } returns itemsFromDb.distinctUntilChanged()
    every { libkiwixBookMarks.pages() } returns libkiwixBookMarks.bookmarks()
    viewModel = BookmarkViewModel(libkiwixBookMarks, zimReaderContainer, sharedPreferenceUtil)
  }

  @Test
  fun `Initial state returns initial state`() {
    assertThat(viewModel.initialState()).isEqualTo(bookmarkState())
  }

  @Test
  internal fun `updatePagesBasedOnFilter returns state with new searchTerm`() {
    assertThat(
      viewModel.updatePagesBasedOnFilter(bookmarkState(), Filter("filter"))
    ).isEqualTo(
      bookmarkState(searchTerm = "filter")
    )
  }

  @Test
  fun `updatePages return state with bookmark items`() {
    assertThat(viewModel.updatePages(bookmarkState(), UpdatePages(listOf(bookmark())))).isEqualTo(
      bookmarkState(listOf(bookmark()))
    )
  }

  @Test
  fun `offerUpdateToShowAllToggle offers UpdateAllBookmarksPreference`() {
    viewModel.effects.test().also {
      viewModel.offerUpdateToShowAllToggle(
        Action.UserClickedShowAllToggle(false), bookmarkState()
      )
    }.assertValues(UpdateAllBookmarksPreference(sharedPreferenceUtil, false))
  }

  @Test
  fun `offerUpdateToShowAllToggle returns state with showAll set to input value`() {
    assertThat(
      viewModel.offerUpdateToShowAllToggle(
        Action.UserClickedShowAllToggle(false),
        bookmarkState()
      )
    ).isEqualTo(bookmarkState(showAll = false))
  }

  @Test
  internal fun `updatePages returns state with updated items`() {
    assertThat(
      viewModel.updatePages(bookmarkState(), UpdatePages(listOf(bookmark())))
    ).isEqualTo(
      bookmarkState(listOf(bookmark()))
    )
  }

  @Test
  internal fun `deselectAllPages deselects bookmarks items`() {
    assertThat(
      viewModel.deselectAllPages(bookmarkState(bookmarks = listOf(bookmark(isSelected = true))))
    ).isEqualTo(
      bookmarkState(bookmarks = listOf(bookmark(isSelected = false)))
    )
  }

  @Test
  internal fun `createDeletePageDialogEffect returns correct dialog`() {
    assertThat(
      viewModel.createDeletePageDialogEffect(bookmarkState())
    ).isEqualTo(
      ShowDeleteBookmarksDialog(viewModel.effects, bookmarkState(), libkiwixBookMarks)
    )
  }

  @Test
  internal fun `copyWithNewItems returns state with copied items`() {
    assertThat(
      viewModel.copyWithNewItems(bookmarkState(), listOf(bookmark()))
    ).isEqualTo(
      bookmarkState(listOf(bookmark()))
    )
  }
}
