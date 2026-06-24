/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.zimManager.fileselectView.effects

import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.DeleteZims
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk

data class DeleteFiles(
  private val booksOnDiskListItems: List<BookOnDisk>,
  private val dialogShower: DialogShower,
  private val deleteFilesUseCase: DeleteFilesUseCase,
  private val viewModelScope: CoroutineScope,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    dialogShower.show(
      DeleteZims(dialogTitle()),
      { deleteBooks(activity) }
    )
  }

  private fun dialogTitle() =
    booksOnDiskListItems.joinToString("\n") {
      it.book.title
    }

  private fun deleteBooks(
    activity: AppCompatActivity
  ) {
    viewModelScope.launch {
      val deleted =
        withContext(ioDispatcher) {
          deleteFilesUseCase(booksOnDiskListItems)
        }

      showResult(activity, deleted)
    }
  }

  private fun showResult(
    activity: AppCompatActivity,
    success: Boolean
  ) {
    activity.toast(
      if (success) {
        R.string.delete_zims_toast
      } else {
        R.string.delete_zim_failed
      }
    )
  }
}
