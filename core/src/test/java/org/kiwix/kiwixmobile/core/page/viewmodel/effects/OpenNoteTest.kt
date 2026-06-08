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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderFragment
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem

class OpenNoteTest {
  @Test
  fun `invokeWith finds CoreReaderFragment and calls showAddNoteDialogForNote`() {
    val noteListItem: NoteListItem = mockk()
    val readerFragment: CoreReaderFragment = mockk(relaxed = true)
    val fragmentManager: FragmentManager = mockk()
    val activity: CoreMainActivity = mockk(relaxed = true)

    every { activity.supportFragmentManager } returns fragmentManager
    every { fragmentManager.fragments } returns listOf(readerFragment)
    every { readerFragment.showAddNoteDialogForNote(noteListItem) } just Runs

    val openNote = OpenNote(noteListItem)
    openNote.invokeWith(activity)

    verify { readerFragment.showAddNoteDialogForNote(noteListItem) }
  }

  @Test
  fun `invokeWith does nothing when no CoreReaderFragment is present`() {
    val noteListItem: NoteListItem = mockk()
    val otherFragment: Fragment = mockk()
    val fragmentManager: FragmentManager = mockk()
    val activity: CoreMainActivity = mockk(relaxed = true)

    every { activity.supportFragmentManager } returns fragmentManager
    every { fragmentManager.fragments } returns listOf(otherFragment)

    val openNote = OpenNote(noteListItem)
    // Should not throw or crash
    openNote.invokeWith(activity)
  }

  @Test
  fun `invokeWith does nothing when fragment list is empty`() {
    val noteListItem: NoteListItem = mockk()
    val fragmentManager: FragmentManager = mockk()
    val activity: CoreMainActivity = mockk(relaxed = true)

    every { activity.supportFragmentManager } returns fragmentManager
    every { fragmentManager.fragments } returns emptyList()

    val openNote = OpenNote(noteListItem)
    // Should not throw or crash
    openNote.invokeWith(activity)
  }
}
