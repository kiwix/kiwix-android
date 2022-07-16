/*
 * Kiwix Android
 * Copyright (c) 2021 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.page.notes.viewmodel.effects

import androidx.appcompat.app.AppCompatActivity
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.cachedComponent
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem
import org.kiwix.kiwixmobile.core.page.viewmodel.effects.OpenNote
import org.kiwix.kiwixmobile.core.page.viewmodel.effects.OpenPage
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.ShowNoteDialog
import java.io.File
import javax.inject.Inject

data class ShowOpenNoteDialog(
  private val effects: PublishProcessor<SideEffect<*>>,
  private val page: Page,
  private val zimReaderContainer: ZimReaderContainer
) : SideEffect<Unit> {
  @Inject lateinit var dialogShower: DialogShower
  override fun invokeWith(activity: AppCompatActivity) {
    activity.cachedComponent.inject(this)
    dialogShower.show(
      ShowNoteDialog,
      { effects.offer(OpenPage(page, zimReaderContainer)) },
      {
        val item = page as NoteListItem
        val file = File(item.zimFilePath.orEmpty())
        zimReaderContainer.setZimFile(file)
        effects.offer(OpenNote(item.noteFilePath, item.zimUrl, item.title))
      }
    )
  }
}
