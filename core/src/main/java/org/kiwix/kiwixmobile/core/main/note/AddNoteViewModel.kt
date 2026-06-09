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

package org.kiwix.kiwixmobile.core.main.note

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore

class AddNoteViewModel(
  val kiwixDataStore: KiwixDataStore,
  val repositoryActions: MainRepositoryActions,
  val zimReaderContainer: ZimReaderContainer
) : ViewModel() {
  data class AddNoteUiState(
    val articleTitle: String = "",
    val noteText: TextFieldValue = TextFieldValue(""),
    val noteEdited: Boolean = false,
    val isSaveMenuButtonEnable: Boolean = false,
    val isDeleteMenuButtonEnable: Boolean = false,
    val isShareMenuButtonEnable: Boolean = false
  )

  private val _uiState = MutableStateFlow(AddNoteUiState())
  val uiState = _uiState.asStateFlow()

  fun onTextChanged(textFieldValue: TextFieldValue) {
    val currentText = _uiState.value.noteText.text

    if (currentText != textFieldValue.text) {
      _uiState.update {
        it.copy(
          noteText = textFieldValue,
          noteEdited = true,
          isSaveMenuButtonEnable = true,
          isShareMenuButtonEnable = true
        )
      }
    } else {
      _uiState.update {
        it.copy(noteText = textFieldValue)
      }
    }
  }

  fun disableAllMenuItems() {
    _uiState.update {
      it.copy(
        isDeleteMenuButtonEnable = false,
        isShareMenuButtonEnable = false,
        isSaveMenuButtonEnable = false
      )
    }
  }

  fun disableSaveMenuItem() {
    _uiState.update { it.copy(isSaveMenuButtonEnable = false) }
  }

  fun enableDeleteMenuItem() {
    _uiState.update { it.copy(isDeleteMenuButtonEnable = true) }
  }

  fun enableSaveMenuItem() {
    if (isZimFileExist) {
      _uiState.update { it.copy(isSaveMenuButtonEnable = true) }
    }
  }

  fun enableShareMenuItem() {
    _uiState.update { it.copy(isShareMenuButtonEnable = true) }
  }

  fun setInitialNoteText(textFieldValue: TextFieldValue, isZimFileExist: Boolean) {
    _uiState.update { it.copy(noteText = textFieldValue) }
    enableShareMenuItem()
    enableDeleteMenuItem()
    if (!isZimFileExist) {
      disableSaveMenuItem()
    }
  }

  fun restoreNoteText(restoreNoteTextFieldValue: TextFieldValue) {
    if (uiState.value.noteText.text != restoreNoteTextFieldValue.text) {
      _uiState.update { it.copy(noteEdited = true) }
      enableSaveMenuItem()
      enableShareMenuItem()
    }
  }
}
