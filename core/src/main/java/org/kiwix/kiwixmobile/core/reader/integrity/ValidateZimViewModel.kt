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

package org.kiwix.kiwixmobile.core.reader.integrity

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import javax.inject.Inject

class ValidateZimViewModel @Inject constructor(
  private val zimIntegrityChecker: ZimIntegrityChecker
) : ViewModel() {
  data class ValidateZimItemState(
    val book: BookOnDisk,
    val status: ValidationStatus = ValidationStatus.Pending
  )

  sealed class ValidationStatus {
    data object Pending : ValidationStatus()
    data object InProgress : ValidationStatus()
    data object Success : ValidationStatus()
    data class Failed(val error: String?) : ValidationStatus()
  }

  private val _items = MutableStateFlow<List<ValidateZimItemState>>(emptyList())
  val items: StateFlow<List<ValidateZimItemState>> = _items

  private val _allZIMValidated = MutableStateFlow(false)
  val allZIMValidated: StateFlow<Boolean> = _allZIMValidated

  suspend fun startValidation(list: List<BookOnDisk>, isCustomApp: Boolean) {
    _items.value = list.map { ValidateZimItemState(it) }

    list.forEach { book ->
      updateStatus(book, ValidationStatus.InProgress)

      val result =
        zimIntegrityChecker.validateZIMFile(book.zimReaderSource, isCustomApp)

      if (result.isValid) {
        updateStatus(book, ValidationStatus.Success)
      } else {
        updateStatus(book, ValidationStatus.Failed(result.error))
      }
    }
    _allZIMValidated.value = true
  }

  private fun updateStatus(book: BookOnDisk, status: ValidationStatus) {
    _items.value = _items.value.map {
      if (it.book == book) it.copy(status = status) else it
    }
  }
}
