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

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel.AddNoteEffect.DismissDialog
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel.AddNoteEffect.RequestStoragePermission
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel.AddNoteEffect.ShareNote
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel.AddNoteEffect.ShowDiscardConfirmationDialog
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel.AddNoteEffect.ShowToast
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel.AddNoteEffect.ShowUndoDeleteSnackbar
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel.AddNoteEffect.ReadPermissionRequiredDialog
import org.kiwix.kiwixmobile.core.main.note.helper.NoteMetadata
import org.kiwix.kiwixmobile.core.main.note.helper.NoteMetadataFactory
import org.kiwix.kiwixmobile.core.main.note.repository.NoteRepository
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.StorageUtils.isExternalStorageWritable
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.File
import javax.inject.Inject

class AddNoteViewModel @Inject constructor(
  private val noteRepository: NoteRepository,
  val zimReaderContainer: ZimReaderContainer,
  private val noteMetadataFactory: NoteMetadataFactory,
  private val kiwixPermissionChecker: KiwixPermissionChecker,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
  data class AddNoteUiState(
    val articleTitle: String = "",
    val noteTextFieldValue: TextFieldValue = TextFieldValue(""),
    val noteEdited: Boolean = false,
    val isSaveMenuButtonEnable: Boolean = false,
    val isDeleteMenuButtonEnable: Boolean = false,
    val isShareMenuButtonEnable: Boolean = false
  )

  sealed interface AddNoteEffect {
    data class ShowToast(
      @StringRes val messageRes: Int,
      val duration: Int = Toast.LENGTH_SHORT
    ) : AddNoteEffect

    data class ShareNote(val noteFile: File) : AddNoteEffect

    data object RequestStoragePermission : AddNoteEffect

    data class ShowUndoDeleteSnackbar(val deletedText: String) : AddNoteEffect
    data object DismissDialog : AddNoteEffect
    data object ShowDiscardConfirmationDialog : AddNoteEffect
    data object ReadPermissionRequiredDialog : AddNoteEffect
  }

  private val _uiState = MutableStateFlow(AddNoteUiState())
  val uiState = _uiState.asStateFlow()

  private val _effects = MutableSharedFlow<AddNoteEffect>()
  val effects = _effects.asSharedFlow()
  private var noteMetadata: NoteMetadata? = null

  fun initialize(config: AddNoteDialogConfig) {
    noteMetadata = noteMetadataFactory.create(config, zimReaderContainer)
    _uiState.update {
      it.copy(articleTitle = requireNoteMetadata().articleTitle.orEmpty())
    }
  }

  fun setInitialNoteText() {
    viewModelScope.launch {
      val noteFileContent = withContext(ioDispatcher) {
        noteRepository.loadNote(requireNoteMetadata())
      }
      val textFieldValue =
        TextFieldValue(noteFileContent.text, TextRange(noteFileContent.text.length))
      _uiState.update { it.copy(noteTextFieldValue = textFieldValue) }
      updateMenuState(
        shareEnabled = noteFileContent.fileExists,
        deleteEnabled = noteFileContent.fileExists,
        saveEnabled = requireNoteMetadata().isZimFileExist
      )
    }
  }

  private fun requireNoteMetadata() = requireNotNull(noteMetadata) {
    "NoteMetadata is not set. Check the AddNoteViewModel.initialize method"
  }

  fun onTextChanged(textFieldValue: TextFieldValue) {
    val currentText = _uiState.value.noteTextFieldValue.text

    if (currentText != textFieldValue.text) {
      _uiState.update {
        it.copy(noteTextFieldValue = textFieldValue, noteEdited = true)
      }
      updateMenuState(saveEnabled = true, shareEnabled = true)
    } else {
      _uiState.update {
        it.copy(noteTextFieldValue = textFieldValue)
      }
    }
  }

  fun restoreNoteText(restoreNoteTextFieldValue: TextFieldValue) {
    if (uiState.value.noteTextFieldValue.text != restoreNoteTextFieldValue.text) {
      _uiState.update { it.copy(noteEdited = true, noteTextFieldValue = restoreNoteTextFieldValue) }
      updateMenuState(saveEnabled = true, shareEnabled = true)
    }
  }

  fun saveNote() {
    viewModelScope.launch {
      if (!isExternalStorageWritable()) {
        sendEffect(ShowToast(R.string.note_save_error_storage_not_writable))
        return@launch
      }
      if (!kiwixPermissionChecker.hasWriteExternalStoragePermission()) {
        Log.d("AddNoteDialog", "WRITE_EXTERNAL_STORAGE permission not granted")
        sendEffect(RequestStoragePermission)
        return@launch
      }
      val noteSaved = withContext(ioDispatcher) {
        noteRepository.saveNote(requireNoteMetadata(), uiState.value.noteTextFieldValue.text)
      }
      if (noteSaved) {
        _uiState.update { it.copy(noteEdited = false) }
        updateMenuState(deleteEnabled = true, saveEnabled = false)
        sendEffect(ShowToast(R.string.note_save_successful))
      } else {
        sendEffect(ShowToast(R.string.note_save_unsuccessful))
      }
    }
  }

  fun shareNote() {
    if (uiState.value.noteEdited && requireNoteMetadata().isZimFileExist) {
      saveNote()
    }
    val noteFile =
      File("${requireNoteMetadata().zimNotesDirectory}${requireNoteMetadata().articleNoteFileName}.txt")
    if (noteFile.exists()) {
      sendEffect(ShareNote(noteFile))
    } else {
      sendEffect(ShowToast(R.string.note_share_error_file_missing))
    }
  }

  fun deleteNote() {
    viewModelScope.launch {
      val editedNoteText = uiState.value.noteTextFieldValue.text
      val noteDeleted = withContext(ioDispatcher) {
        noteRepository.deleteNote(requireNoteMetadata())
      }
      if (noteDeleted) {
        _uiState.update { it.copy(noteTextFieldValue = TextFieldValue("")) }
        updateMenuState(saveEnabled = false, deleteEnabled = false, shareEnabled = false)
        sendEffect(ShowUndoDeleteSnackbar(editedNoteText))
      } else {
        sendEffect(ShowToast(R.string.note_delete_unsuccessful))
      }
    }
  }

  fun sendEffect(effect: AddNoteEffect) {
    viewModelScope.launch { _effects.emit(effect) }
  }

  private fun updateMenuState(
    saveEnabled: Boolean = uiState.value.isSaveMenuButtonEnable,
    deleteEnabled: Boolean = uiState.value.isDeleteMenuButtonEnable,
    shareEnabled: Boolean = uiState.value.isShareMenuButtonEnable
  ) {
    _uiState.update {
      it.copy(
        isShareMenuButtonEnable = shareEnabled,
        isSaveMenuButtonEnable = saveEnabled,
        isDeleteMenuButtonEnable = deleteEnabled
      )
    }
  }

  fun closeDialog() {
    if (uiState.value.noteEdited) {
      sendEffect(ShowDiscardConfirmationDialog)
    } else {
      sendEffect(DismissDialog)
    }
  }

  fun onStoragePermissionResult(isGranted: Boolean, activity: Activity) {
    if (isGranted) {
      saveNote()
      return
    }
    val effect = if (kiwixPermissionChecker.shouldShowRationale(activity, WRITE_EXTERNAL_STORAGE)) {
      ShowToast(R.string.ext_storage_permission_rationale_add_note)
    } else {
      ReadPermissionRequiredDialog
    }
    sendEffect(effect)
  }
}
