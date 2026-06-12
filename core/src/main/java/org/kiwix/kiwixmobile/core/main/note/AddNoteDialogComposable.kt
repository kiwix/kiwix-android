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

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.CoroutineScope
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.navigateToAppSettings
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel.AddNoteEffect
import org.kiwix.kiwixmobile.core.main.note.AddNoteViewModel.AddNoteEffect.DismissDialog
import org.kiwix.kiwixmobile.core.page.notes.models.NoteListItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Drawable
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Vector
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.TEN_DP
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixBasicDialogFrame
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import java.io.File

const val ADD_NOTE_DIALOG_CLOSE_IMAGE_BUTTON_TESTING_TAG = "addNoteDialogCloseImageButtonTestingTag"

/**
 * Configuration data for showing the AddNoteDialog composable.
 * Two modes of initialization:
 *   1. From the reader screen: [noteListItem] is null, data comes from [ZimReaderContainer] and the WebView.
 *   2. From the notes screen: [noteListItem] is provided with all note metadata.
 */
data class AddNoteDialogConfig(
  val noteListItem: NoteListItem? = null,
  /** Data from reader screen (when noteListItem is null) */
  val articleTitle: String? = null,
  val zimFileUrl: String = "",
  val currentWebViewUrl: String? = null,
  val currentWebViewTitle: String? = null
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AddNoteDialogComposable(
  addNoteViewModel: AddNoteViewModel,
  config: AddNoteDialogConfig,
  onDismiss: () -> Unit
) {
  // Use a separate AlertDialogShower because this dialog is already hosted by one.
  // Reusing the same instance for NotesDiscardConfirmation would dismiss the
  // AddNote dialog instead of showing the confirmation dialog above it.
  val alertDialogShower = remember { AlertDialogShower() }
  val uiState by addNoteViewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val activity = LocalActivity.current ?: return
  val snackBarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  val writePermissionState =
    rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE) { isGranted ->
      addNoteViewModel.onStoragePermissionResult(isGranted, activity)
    }

  HandleSideEffects(
    context = context,
    addNoteViewModel = addNoteViewModel,
    scope = scope,
    snackBarHostState = snackBarHostState,
    writePermissionState = writePermissionState,
    onDismissDialog = {
      activity.currentFocus?.closeKeyboard()
      onDismiss()
    },
    alertDialogShower = alertDialogShower
  )

  LaunchedEffect(Unit) {
    // Display existing note
    addNoteViewModel.apply {
      initialize(config)
      setInitialNoteText()
    }
  }

  KiwixBasicDialogFrame(
    onDismissRequest = { addNoteViewModel.closeDialog() },
    dialogPadding = TEN_DP,
    topPaddingForContent = ZERO.dp
  ) {
    AddNoteDialogScreen(
      articleTitle = uiState.articleTitle,
      navigationIcon = {
        NavigationIcon(
          iconItem = Drawable(R.drawable.ic_close_white_24dp),
          onClick = { addNoteViewModel.closeDialog() },
          testingTag = ADD_NOTE_DIALOG_CLOSE_IMAGE_BUTTON_TESTING_TAG
        )
      },
      noteText = uiState.noteTextFieldValue,
      actionMenuItems = buildMenuItems(uiState, addNoteViewModel),
      onTextChange = { textFieldValue -> addNoteViewModel.onTextChanged(textFieldValue) },
      snackBarHostState = snackBarHostState
    )
    DialogHost(alertDialogShower)
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun HandleSideEffects(
  addNoteViewModel: AddNoteViewModel,
  context: Context,
  scope: CoroutineScope,
  snackBarHostState: SnackbarHostState,
  writePermissionState: PermissionState,
  alertDialogShower: AlertDialogShower,
  onDismissDialog: () -> Unit
) {
  LaunchedEffect(Unit) {
    addNoteViewModel.effects.collect { effect ->
      when (effect) {
        is AddNoteEffect.ShowToast -> {
          context.toast(effect.messageRes, effect.duration)
        }

        is AddNoteEffect.RequestStoragePermission -> {
          writePermissionState.launchPermissionRequest()
        }

        is AddNoteEffect.ShareNote -> {
          shareNoteFile(effect.noteFile, context)
        }

        is AddNoteEffect.ShowUndoDeleteSnackbar -> {
          showUndoSnackbar(context, effect.deletedText, snackBarHostState, addNoteViewModel, scope)
        }

        is AddNoteEffect.ShowDiscardConfirmationDialog -> {
          alertDialogShower.show(
            KiwixDialog.NotesDiscardConfirmation,
            {
              alertDialogShower.dismiss()
              onDismissDialog.invoke()
            }
          )
        }

        is AddNoteEffect.ReadPermissionRequiredDialog -> {
          alertDialogShower.show(
            KiwixDialog.ReadPermissionRequired,
            { context.navigateToAppSettings() }
          )
        }

        is DismissDialog -> onDismissDialog.invoke()
      }
    }
  }
}

private fun shareNoteFile(noteFile: File, context: Context) {
  val noteFileUri = FileProvider.getUriForFile(
    context,
    "${context.packageName}.fileprovider",
    noteFile
  )
  val noteFileShareIntent = Intent(Intent.ACTION_SEND).apply {
    type = "application/octet-stream"
    putExtra(Intent.EXTRA_STREAM, noteFileUri)
    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
  }
  val shareChooser = Intent.createChooser(
    noteFileShareIntent,
    context.getString(R.string.note_share_app_chooser_title)
  )
  if (noteFileShareIntent.resolveActivity(context.packageManager) != null) {
    context.startActivity(shareChooser)
  }
}

private fun showUndoSnackbar(
  context: Context,
  editedNoteText: String,
  snackBarHostState: SnackbarHostState,
  addNoteViewModel: AddNoteViewModel,
  scope: CoroutineScope
) {
  snackBarHostState.snack(
    message = context.getString(R.string.note_delete_successful),
    actionLabel = context.getString(R.string.undo),
    actionClick = {
      val restoreNoteTextFieldValue = TextFieldValue(
        text = editedNoteText,
        selection = TextRange(editedNoteText.length)
      )
      addNoteViewModel.restoreNoteText(restoreNoteTextFieldValue)
    },
    lifecycleScope = scope
  )
}

private fun buildMenuItems(
  uiState: AddNoteViewModel.AddNoteUiState,
  addNoteViewModel: AddNoteViewModel
) = listOf(
  ActionMenuItem(
    Vector(Icons.Default.Delete),
    R.string.delete,
    { addNoteViewModel.deleteNote() },
    isEnabled = uiState.isDeleteMenuButtonEnable,
    testingTag = DELETE_MENU_BUTTON_TESTING_TAG
  ),
  ActionMenuItem(
    Vector(Icons.Default.Share),
    R.string.share,
    { addNoteViewModel.shareNote() },
    isEnabled = uiState.isShareMenuButtonEnable,
    testingTag = SHARE_MENU_BUTTON_TESTING_TAG
  ),
  ActionMenuItem(
    Drawable(R.drawable.ic_save),
    R.string.save,
    { addNoteViewModel.saveNote() },
    isEnabled = uiState.isSaveMenuButtonEnable,
    testingTag = SAVE_MENU_BUTTON_TESTING_TAG
  )
)
