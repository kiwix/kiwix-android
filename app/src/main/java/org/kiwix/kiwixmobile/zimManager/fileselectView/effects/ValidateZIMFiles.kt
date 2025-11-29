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

package org.kiwix.kiwixmobile.zimManager.fileselectView.effects

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.ValidateZimFilesConfirmation
import org.kiwix.kiwixmobile.core.utils.dialog.ValidateZimDialog
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk

data class ValidateZIMFiles(
  private val booksOnDiskListItems: List<BookOnDisk>,
  private val dialogShower: DialogShower,
  private val validateZimViewModel: ValidateZimViewModel
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    (activity as BaseActivity).cachedComponent.inject(this)

    val name = booksOnDiskListItems.joinToString(separator = "\n") { it.book.title }
    dialogShower.show(ValidateZimFilesConfirmation(name), {
      startValidatingZimFiles(booksOnDiskListItems, validateZimViewModel, dialogShower)
    })
  }

  private fun startValidatingZimFiles(
    booksOnDiskListItems: List<BookOnDisk>,
    validateZimViewModel: ValidateZimViewModel,
    dialogShower: DialogShower,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
  ) {
    CoroutineScope(dispatcher).launch {
      validateZimViewModel.startValidation(booksOnDiskListItems, false)
    }
    dialogShower.show(
      KiwixDialog.ValidatingZimFiles(
        customGetView = {
          val items by validateZimViewModel.items.collectAsStateWithLifecycle()
          val allValidated by validateZimViewModel.allZIMValidated.collectAsStateWithLifecycle()
          ValidateZimDialog(
            items,
            android.R.string.ok,
            {
              if (allValidated) {
                (dialogShower as AlertDialogShower).dismiss()
              }
            },
            cancelButtonText = R.string.cancel,
            onCancelButtonClick = {
              validateZimViewModel.cancelValidation()
              (dialogShower as AlertDialogShower).dismiss()
            }
          )
        }
      )
    )
  }
}
