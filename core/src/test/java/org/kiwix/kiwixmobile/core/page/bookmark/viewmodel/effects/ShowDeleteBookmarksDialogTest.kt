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

package org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.effects

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.bookmarkState
import org.kiwix.kiwixmobile.core.page.libkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.viewmodel.effects.DeletePageItems
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.DeleteAllBookmarks
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.DeleteSelectedBookmarks
import java.util.UUID

internal class ShowDeleteBookmarksDialogTest {
  val effects = mockk<MutableSharedFlow<SideEffect<*>>>(relaxed = true)
  private val newBookmarksDao = mockk<NewBookmarksDao>()
  val activity = mockk<CoreMainActivity>()
  private val dialogShower = mockk<DialogShower>(relaxed = true)
  private val viewModelScope = CoroutineScope(Dispatchers.IO)

  @Test
  fun `invoke with shows dialog that offers ConfirmDelete action`() {
    val showDeleteBookmarksDialog =
      ShowDeleteBookmarksDialog(
        effects,
        bookmarkState(),
        newBookmarksDao,
        viewModelScope,
        dialogShower
      )
    mockkActivityInjection(showDeleteBookmarksDialog)
    val lambdaSlot = slot<() -> Unit>()
    showDeleteBookmarksDialog.invokeWith(activity)
    verify { dialogShower.show(any(), capture(lambdaSlot)) }
    lambdaSlot.captured.invoke()
    verify { effects.tryEmit(DeletePageItems(bookmarkState(), newBookmarksDao, viewModelScope)) }
  }

  private fun mockkActivityInjection(showDeleteBookmarksDialog: ShowDeleteBookmarksDialog) {
    every { activity.cachedComponent.inject(showDeleteBookmarksDialog) } answers {
      showDeleteBookmarksDialog.dialogShower = dialogShower
      Unit
    }
  }

  @Test
  fun `invoke with selected items shows dialog with DeleteSelectedBookmarks title`() =
    runBlocking {
      val zimReaderSource: ZimReaderSource = mockk()
      every { zimReaderSource.toDatabase() } returns ""
      val showDeleteBookmarksDialog =
        ShowDeleteBookmarksDialog(
          effects,
          bookmarkState(
            listOf(
              libkiwixBookmarkItem(
                isSelected = true,
                databaseId = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE,
                zimReaderSource = zimReaderSource
              )
            )
          ),
          newBookmarksDao,
          viewModelScope,
          dialogShower
        )
      mockkActivityInjection(showDeleteBookmarksDialog)
      showDeleteBookmarksDialog.invokeWith(activity)
      verify { dialogShower.show(DeleteSelectedBookmarks, any()) }
    }

  @Test
  fun `invoke with no selected items shows dialog with DeleteAllBookmarks title`() =
    runBlocking {
      val zimReaderSource: ZimReaderSource = mockk()
      every { zimReaderSource.toDatabase() } returns ""
      val showDeleteBookmarksDialog =
        ShowDeleteBookmarksDialog(
          effects,
          bookmarkState(
            listOf(
              libkiwixBookmarkItem(
                databaseId = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE,
                zimReaderSource = zimReaderSource
              )
            )
          ),
          newBookmarksDao,
          viewModelScope,
          dialogShower
        )
      mockkActivityInjection(showDeleteBookmarksDialog)
      showDeleteBookmarksDialog.invokeWith(activity)
      verify { dialogShower.show(DeleteAllBookmarks, any()) }
    }
}
