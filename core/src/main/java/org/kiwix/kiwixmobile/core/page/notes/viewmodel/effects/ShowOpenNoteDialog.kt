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
import androidx.core.net.toUri
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
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.getAssetFileDescriptorFromUri
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
        // Check if zimFilePath is not null, and then set it in zimReaderContainer.
        // For custom apps, we are currently using fileDescriptor, and they only have a single file in them,
        // which is already set in zimReaderContainer, so there's no need to set it again.
        item.zimFilePath?.let {
          // Obtain the asset file descriptor for the provided URI.
          // Given that we now exclusively use the fileDescriptor for all our functionalities,
          // we have modified this method to assign the fileDescriptor instead of the file
          // to address the crash triggered when clicking the 'OPEN NOTE' button.
          getAssetFileDescriptorFromUri(
            activity,
            it.toUri()
          )?.let { assetFileDescriptor ->
            zimReaderContainer.setZimFileOrFileDescriptor(
              assetFileDescriptor = assetFileDescriptor,
              filePath = it
            )
          }
        }
        effects.offer(OpenNote(item.noteFilePath, item.zimUrl, item.title))
      }
    )
  }
}
