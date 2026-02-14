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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.effects.ShowDeleteBookmarksDialog
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.effects.UpdateAllBookmarksPreference
import org.kiwix.kiwixmobile.core.page.bookmarkState
import org.kiwix.kiwixmobile.core.page.libkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.page.viewmodel.Action.UpdatePages
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.testFlow
import org.kiwix.sharedFunctions.InstantExecutorExtension
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantExecutorExtension::class)
internal class BookmarkViewModelTest {
  private val testDispatcher = UnconfinedTestDispatcher()
  private val libkiwixBookMarks: LibkiwixBookmarks = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val dialogShower: AlertDialogShower = mockk()
  private val viewModelScope = CoroutineScope(testDispatcher)

  private lateinit var viewModel: BookmarkViewModel

  private val itemsFromDb: MutableStateFlow<List<Page>> =
    MutableStateFlow(emptyList())

  @BeforeEach
  fun init() {
    clearAllMocks()
    Dispatchers.setMain(testDispatcher)
    every { zimReaderContainer.id } returns "id"
    every { zimReaderContainer.name } returns "zimName"
    every { kiwixDataStore.showBookmarksOfAllBooks } returns flowOf(true)
    every { libkiwixBookMarks.bookmarks() } returns itemsFromDb
    every { libkiwixBookMarks.pages() } returns libkiwixBookMarks.bookmarks()
    viewModel =
      BookmarkViewModel(libkiwixBookMarks, zimReaderContainer, kiwixDataStore).apply {
        alertDialogShower = dialogShower
        lifeCycleScope = viewModelScope
      }
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
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
    val zimReaderSource: ZimReaderSource = mockk()
    val databaseId = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
    every { zimReaderSource.toDatabase() } returns ""
    assertThat(
      viewModel.updatePages(
        bookmarkState(),
        UpdatePages(listOf(libkiwixBookmarkItem(databaseId, zimReaderSource = zimReaderSource)))
      )
    ).isEqualTo(
      bookmarkState(listOf(libkiwixBookmarkItem(databaseId, zimReaderSource = zimReaderSource)))
    )
  }

  @Test
  fun `offerUpdateToShowAllToggle offers UpdateAllBookmarksPreference`() = runTest {
    testFlow(
      flow = viewModel.effects,
      triggerAction = {
        viewModel.offerUpdateToShowAllToggle(
          Action.UserClickedShowAllToggle(false),
          bookmarkState()
        )
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(
          UpdateAllBookmarksPreference(
            kiwixDataStore,
            false,
            viewModelScope
          )
        )
      }
    )
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
    val zimReaderSource: ZimReaderSource = mockk()
    val databaseId = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
    every { zimReaderSource.toDatabase() } returns ""
    assertThat(
      viewModel.updatePages(
        bookmarkState(),
        UpdatePages(listOf(libkiwixBookmarkItem(databaseId, zimReaderSource = zimReaderSource)))
      )
    ).isEqualTo(
      bookmarkState(listOf(libkiwixBookmarkItem(databaseId, zimReaderSource = zimReaderSource)))
    )
  }

  @Test
  internal fun `deselectAllPages deselects bookmarks items`() {
    val zimReaderSource: ZimReaderSource = mockk()
    val databaseId = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
    every { zimReaderSource.toDatabase() } returns ""
    assertThat(
      viewModel.deselectAllPages(
        bookmarkState(
          bookmarks = listOf(
            libkiwixBookmarkItem(
              isSelected = true,
              databaseId = databaseId,
              zimReaderSource = zimReaderSource
            )
          )
        )
      )
    ).isEqualTo(
      bookmarkState(
        bookmarks = listOf(
          libkiwixBookmarkItem(
            isSelected = false,
            databaseId = databaseId,
            zimReaderSource = zimReaderSource
          )
        )
      )
    )
  }

  @Test
  internal fun `createDeletePageDialogEffect returns correct dialog`() =
    runTest {
      assertThat(
        viewModel.createDeletePageDialogEffect(bookmarkState(), viewModelScope)
      ).isEqualTo(
        ShowDeleteBookmarksDialog(
          viewModel.effects,
          bookmarkState(),
          libkiwixBookMarks,
          viewModelScope,
          dialogShower
        )
      )
    }

  @Test
  internal fun `copyWithNewItems returns state with copied items`() {
    val zimReaderSource: ZimReaderSource = mockk()
    val databaseId = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
    every { zimReaderSource.toDatabase() } returns ""
    assertThat(
      viewModel.copyWithNewItems(
        bookmarkState(),
        listOf(libkiwixBookmarkItem(databaseId, zimReaderSource = zimReaderSource))
      )
    ).isEqualTo(
      bookmarkState(listOf(libkiwixBookmarkItem(databaseId, zimReaderSource = zimReaderSource)))
    )
  }
}
