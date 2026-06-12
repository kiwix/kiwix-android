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

package org.kiwix.kiwixmobile.core.page.viewmodel.effects

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel
import org.kiwix.kiwixmobile.core.page.notes.models.NoteListItem
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog

class OpenNoteTest {
  @Test
  fun `invokeWith shows AddNoteDialogDialog`() {
    val noteListItem: NoteListItem = mockk()
    val alertDialogShower: AlertDialogShower = mockk(relaxed = true)
    val addNoteViewModel: AddNoteViewModel = mockk(relaxed = true)
    val activity: CoreMainActivity = mockk(relaxed = true)

    val slot = slot<KiwixDialog>()

    every { alertDialogShower.show(capture(slot)) } returns Unit

    OpenNote(
      noteListItem,
      alertDialogShower,
      addNoteViewModel
    ).invokeWith(activity)

    verify(exactly = 1) {
      alertDialogShower.show(any())
    }

    assert(slot.captured is KiwixDialog.AddNoteDialogDialog)
  }
}
