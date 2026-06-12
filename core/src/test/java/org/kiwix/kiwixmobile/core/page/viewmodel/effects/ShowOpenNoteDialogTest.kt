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

package org.kiwix.kiwixmobile.core.page.viewmodel.effects

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Before
import org.junit.Test
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.di.components.CoreActivityComponent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.notes.models.NoteListItem
import org.kiwix.kiwixmobile.core.page.notes.viewmodel.effects.ShowOpenNoteDialog
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog

class ShowOpenNoteDialogTest {
  private lateinit var zimReaderContainer: ZimReaderContainer
  private lateinit var dialogShower: AlertDialogShower
  private lateinit var addNoteViewModel: AddNoteViewModel
  private lateinit var activity: CoreMainActivity
  private lateinit var component: CoreActivityComponent

  @Before
  fun setup() {
    zimReaderContainer = mockk()
    dialogShower = mockk(relaxed = true)
    addNoteViewModel = mockk(relaxed = true)
    activity = mockk(relaxed = true)
    component = mockk(relaxed = true)

    every { activity.cachedComponent } returns component
  }

  @Test
  fun `invokeWith shows ShowNoteDialog`() {
    val effects = MutableSharedFlow<SideEffect<*>>()
    val page: Page = mockk()
    createShowOpenNoteDialog(effects, page).invokeWith(activity)
    verify {
      dialogShower.show(
        KiwixDialog.ShowNoteDialog,
        any(),
        any()
      )
    }
  }

  @Test
  fun `positive action emits OpenPage`() {
    val effects = mockk<MutableSharedFlow<SideEffect<*>>>(relaxed = true)

    val page: Page = mockk()

    val openPageSlot = slot<() -> Unit>()

    every {
      dialogShower.show(
        KiwixDialog.ShowNoteDialog,
        capture(openPageSlot),
        any()
      )
    } returns Unit
    createShowOpenNoteDialog(effects, page).invokeWith(activity)

    openPageSlot.captured.invoke()

    verify {
      effects.tryEmit(any<OpenPage>())
    }
  }

  @Test
  fun `negative action emits OpenNote`() {
    val effects = mockk<MutableSharedFlow<SideEffect<*>>>(relaxed = true)
    val noteItem: NoteListItem = mockk()

    val openNoteSlot = slot<() -> Unit>()

    every {
      dialogShower.show(
        KiwixDialog.ShowNoteDialog,
        any(),
        capture(openNoteSlot)
      )
    } returns Unit
    createShowOpenNoteDialog(effects, noteItem).invokeWith(activity)
    openNoteSlot.captured.invoke()

    verify {
      effects.tryEmit(any<OpenNote>())
    }
  }

  private fun createShowOpenNoteDialog(
    effects: MutableSharedFlow<SideEffect<*>>,
    page: Page
  ) = ShowOpenNoteDialog(
    effects = effects,
    page = page,
    zimReaderContainer = zimReaderContainer,
    dialogShower = dialogShower,
    addNoteViewModel = addNoteViewModel
  )
}
