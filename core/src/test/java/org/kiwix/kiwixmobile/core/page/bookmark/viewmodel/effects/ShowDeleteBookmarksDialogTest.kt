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

import androidx.lifecycle.lifecycleScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.reactivex.processors.PublishProcessor
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.bookmark
import org.kiwix.kiwixmobile.core.page.bookmarkState
import org.kiwix.kiwixmobile.core.page.viewmodel.effects.DeletePageItems
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.DeleteAllBookmarks
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.DeleteSelectedBookmarks

internal class ShowDeleteBookmarksDialogTest {
  val effects = mockk<PublishProcessor<SideEffect<*>>>(relaxed = true)
  private val newBookmarksDao = mockk<NewBookmarksDao>()
  val activity = mockk<CoreMainActivity>()
  private val dialogShower = mockk<DialogShower>(relaxed = true)

  @Test
  fun `invoke with shows dialog that offers ConfirmDelete action`() {
    val showDeleteBookmarksDialog =
      ShowDeleteBookmarksDialog(effects, bookmarkState(), newBookmarksDao)
    mockkActivityInjection(showDeleteBookmarksDialog)
    val lambdaSlot = slot<() -> Unit>()
    showDeleteBookmarksDialog.invokeWith(activity)
    verify { dialogShower.show(any(), capture(lambdaSlot)) }
    lambdaSlot.captured.invoke()
    verify {
      effects.offer(
        DeletePageItems(
          bookmarkState(),
          newBookmarksDao,
          activity.lifecycleScope
        )
      )
    }
  }

  private fun mockkActivityInjection(showDeleteBookmarksDialog: ShowDeleteBookmarksDialog) {
    every { activity.cachedComponent.inject(showDeleteBookmarksDialog) } answers {
      showDeleteBookmarksDialog.dialogShower = dialogShower
      Unit
    }
  }

  @Test
  fun `invoke with selected items shows dialog with DeleteSelectedBookmarks title`() {
    val showDeleteBookmarksDialog =
      ShowDeleteBookmarksDialog(
        effects,
        bookmarkState(listOf(bookmark(isSelected = true))),
        newBookmarksDao
      )
    mockkActivityInjection(showDeleteBookmarksDialog)
    showDeleteBookmarksDialog.invokeWith(activity)
    verify { dialogShower.show(DeleteSelectedBookmarks, any()) }
  }

  @Test
  fun `invoke with no selected items shows dialog with DeleteAllBookmarks title`() {
    val showDeleteBookmarksDialog =
      ShowDeleteBookmarksDialog(
        effects,
        bookmarkState(listOf(bookmark())),
        newBookmarksDao
      )
    mockkActivityInjection(showDeleteBookmarksDialog)
    showDeleteBookmarksDialog.invokeWith(activity)
    verify { dialogShower.show(DeleteAllBookmarks, any()) }
  }
}
