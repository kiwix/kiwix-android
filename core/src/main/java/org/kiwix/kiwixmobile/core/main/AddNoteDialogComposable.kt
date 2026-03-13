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

package org.kiwix.kiwixmobile.core.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.kiwix.kiwixmobile.core.CoreApp.Companion.instance
import org.kiwix.kiwixmobile.core.R
import android.view.inputmethod.InputMethodManager
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.page.notes.models.NoteListItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Drawable
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Vector
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.File
import java.io.IOException

const val ADD_NOTE_DIALOG_CLOSE_IMAGE_BUTTON_TESTING_TAG = "addNoteDialogCloseImageButtonTestingTag"

@JvmField val NOTES_DIRECTORY =
  instance.getExternalFilesDir("").toString() + "/Kiwix/Notes/"

/**
 * Configuration data for showing the AddNoteDialog composable.
 * Two modes of initialization:
 *   1. From the reader screen: [noteListItem] is null, data comes from [zimReaderContainer] and the WebView.
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

/** Holds computed metadata for a note, derived from config and zimReaderContainer. */
data class NoteMetadata(
  val zimFileName: String?,
  val zimFileTitle: String?,
  val zimId: String,
  val zimReaderSource: org.kiwix.kiwixmobile.core.reader.ZimReaderSource?,
  val favicon: String?,
  val articleTitle: String?,
  val zimFileUrl: String,
  val zimNoteDirectoryName: String,
  val articleNoteFileName: String,
  val zimNotesDirectory: String,
  val isZimFileExist: Boolean
) {
  fun getNoteTitle(): String =
    if (zimFileTitle != null && zimReaderSource != null) {
      zimFileTitle
    } else {
      "${zimFileTitle.orEmpty()}: $articleTitle"
    }
}

/** Creates NoteMetadata from config and zimReaderContainer. */
@Suppress("CyclomaticComplexMethod")
fun computeNoteMetadata(
  config: AddNoteDialogConfig,
  zimReaderContainer: ZimReaderContainer
): NoteMetadata {
  val noteListItem = config.noteListItem

  val zimFileName = noteListItem?.zimReaderSource?.toDatabase()
    ?: zimReaderContainer.zimReaderSource?.toDatabase()
    ?: zimReaderContainer.name
  val zimFileTitle = noteListItem?.title ?: zimReaderContainer.zimFileTitle
  val zimId = noteListItem?.zimId ?: zimReaderContainer.id.orEmpty()
  val zimReaderSource = noteListItem?.zimReaderSource ?: zimReaderContainer.zimReaderSource
  val favicon = noteListItem?.favicon ?: zimReaderContainer.favicon

  val articleTitle = if (noteListItem != null) {
    noteListItem.title.substringAfter(": ")
  } else {
    config.currentWebViewTitle ?: config.articleTitle
  }

  val zimFileUrl = if (noteListItem != null) {
    noteListItem.zimUrl
  } else {
    config.currentWebViewUrl.orEmpty()
  }

  val zimNoteDirectoryName = run {
    val name = getTextAfterLastSlashWithoutExtension(zimFileName.orEmpty())
    (if (name.isNotEmpty()) name else zimFileTitle).orEmpty()
  }

  val articleNoteFileName = if (noteListItem?.noteFilePath != null) {
    getTextAfterLastSlashWithoutExtension(noteListItem.noteFilePath)
  } else {
    val url = config.currentWebViewUrl
    val name = if (url != null) getTextAfterLastSlashWithoutExtension(url) else ""
    name.ifEmpty { articleTitle }.orEmpty()
  }

  val zimNotesDirectory = noteListItem?.noteFilePath?.substringBefore(articleNoteFileName)
    ?: "${NOTES_DIRECTORY}$zimNoteDirectoryName/"

  return NoteMetadata(
    zimFileName = zimFileName,
    zimFileTitle = zimFileTitle,
    zimId = zimId,
    zimReaderSource = zimReaderSource,
    favicon = favicon,
    articleTitle = articleTitle,
    zimFileUrl = zimFileUrl,
    zimNoteDirectoryName = zimNoteDirectoryName,
    articleNoteFileName = articleNoteFileName,
    zimNotesDirectory = zimNotesDirectory,
    isZimFileExist = zimFileName != null
  )
}

private fun getTextAfterLastSlashWithoutExtension(path: String): String =
  path.substringAfterLast('/', "").substringBeforeLast('.')

/** Handles note file CRUD operations. */
@Suppress("TooManyFunctions", "LongParameterList")
class NoteOperations(
  private val context: Context,
  private val scope: CoroutineScope,
  private val metadata: NoteMetadata,
  private val kiwixDataStore: KiwixDataStore,
  private val mainRepositoryActions: MainRepositoryActions,
  private val noteText: androidx.compose.runtime.MutableState<TextFieldValue>,
  private val noteEdited: androidx.compose.runtime.MutableState<Boolean>,
  private val snackBarHostState: SnackbarHostState,
  private val menuController: NoteMenuController
) {
  fun saveNote() {
    if (!instance.isExternalStorageWritable) {
      context.toast(R.string.note_save_error_storage_not_writable, Toast.LENGTH_LONG)
      return
    }
    if (!hasWritePermission()) {
      Log.d("AddNoteDialog", "WRITE_EXTERNAL_STORAGE permission not granted")
      context.toast(R.string.note_save_unsuccessful, Toast.LENGTH_LONG)
      return
    }
    writeNoteToFile()
  }

  private fun hasWritePermission(): Boolean {
    if (ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      ) != PackageManager.PERMISSION_GRANTED &&
      runBlocking { !kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() } &&
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    ) {
      return false
    }
    return true
  }

  private fun writeNoteToFile() {
    val notesFolder = File(metadata.zimNotesDirectory)
    if (!notesFolder.exists() && !notesFolder.mkdirs()) {
      context.toast(R.string.note_save_unsuccessful, Toast.LENGTH_LONG)
      Log.d("AddNoteDialog", "Required folder doesn't exist")
      return
    }
    val noteFile = File(notesFolder.absolutePath, "${metadata.articleNoteFileName}.txt")
    try {
      noteFile.writeText(noteText.value.text)
      context.toast(R.string.note_save_successful, Toast.LENGTH_SHORT)
      noteEdited.value = false
      menuController.enableDeleteMenuItem()
      addNoteToDao(noteFile.canonicalPath, metadata.getNoteTitle())
      menuController.disableSaveMenuItem()
    } catch (e: IOException) {
      e.printStackTrace()
      context.toast(R.string.note_save_unsuccessful, Toast.LENGTH_LONG)
    }
  }

  private fun addNoteToDao(noteFilePath: String?, title: String) {
    scope.launch {
      noteFilePath?.let { filePath ->
        if (filePath.isNotEmpty() && metadata.zimFileUrl.isNotEmpty()) {
          val noteToSave = NoteListItem(
            zimId = metadata.zimId,
            title = title,
            url = metadata.zimFileUrl,
            noteFilePath = noteFilePath,
            zimReaderSource = metadata.zimReaderSource,
            favicon = metadata.favicon,
          )
          mainRepositoryActions.saveNote(noteToSave)
        } else {
          Log.d("AddNoteDialog", "Cannot process with empty zim url or noteFilePath")
        }
      }
    }
  }

  fun deleteNote() {
    scope.launch {
      val noteFile = File(
        metadata.zimNotesDirectory,
        "${metadata.articleNoteFileName}.txt"
      )
      val noteDeleted = noteFile.delete()
      val editedNoteText = noteText.value.text
      if (noteDeleted) {
        noteText.value = TextFieldValue("")
        mainRepositoryActions.deleteNote(metadata.getNoteTitle())
        menuController.disableAllMenuItems()
        showUndoSnackbar(editedNoteText)
      } else {
        context.toast(R.string.note_delete_unsuccessful, Toast.LENGTH_LONG)
      }
    }
  }

  private suspend fun showUndoSnackbar(editedNoteText: String) {
    snackBarHostState.snack(
      message = context.getString(R.string.note_delete_successful),
      actionLabel = context.getString(R.string.undo),
      actionClick = {
        val restoreNoteTextFieldValue = TextFieldValue(
          text = editedNoteText,
          selection = TextRange(editedNoteText.length)
        )
        if (noteText.value.text != restoreNoteTextFieldValue.text) {
          noteEdited.value = true
          menuController.enableSaveMenuItem()
          menuController.enableShareMenuItem()
        }
        noteText.value = restoreNoteTextFieldValue
      },
      lifecycleScope = scope
    )
  }

  fun shareNote() {
    if (noteEdited.value && metadata.isZimFileExist) {
      saveNote()
    }
    val noteFile = File("${metadata.zimNotesDirectory}${metadata.articleNoteFileName}.txt")
    if (noteFile.exists()) {
      shareNoteFile(noteFile)
    } else {
      context.toast(R.string.note_share_error_file_missing, Toast.LENGTH_SHORT)
    }
  }

  private fun shareNoteFile(noteFile: File) {
    val noteFileUri = FileProvider.getUriForFile(
      context,
      context.packageName + ".fileprovider",
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
}

/** Controls the enable/disable state of menu items. */
class NoteMenuController(
  private val menuItems: androidx.compose.runtime.MutableState<List<ActionMenuItem>>,
  private val isZimFileExist: Boolean
) {
  private fun updateMenuItem(vararg contentDescription: Int, isEnabled: Boolean) {
    menuItems.value = menuItems.value.map { item ->
      if (contentDescription.contains(item.contentDescription)) {
        item.copy(isEnabled = isEnabled)
      } else {
        item
      }
    }
  }

  fun disableAllMenuItems() {
    updateMenuItem(R.string.delete, R.string.share, R.string.save, isEnabled = false)
  }

  fun disableSaveMenuItem() {
    updateMenuItem(R.string.save, isEnabled = false)
  }

  fun enableDeleteMenuItem() {
    updateMenuItem(R.string.delete, isEnabled = true)
  }

  fun enableSaveMenuItem() {
    if (isZimFileExist) {
      updateMenuItem(R.string.save, isEnabled = true)
    }
  }

  fun enableShareMenuItem() {
    updateMenuItem(R.string.share, isEnabled = true)
  }
}

/**
 * Pure composable replacement for the old AddNoteDialog DialogFragment.
 * Contains all the business logic for note CRUD operations.
 */
@Suppress("LongMethod")
@Composable
fun AddNoteDialogComposable(
  config: AddNoteDialogConfig,
  zimReaderContainer: ZimReaderContainer,
  kiwixDataStore: KiwixDataStore,
  alertDialogShower: AlertDialogShower,
  mainRepositoryActions: MainRepositoryActions,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackBarHostState = remember { SnackbarHostState() }
  val metadata = remember(config) { computeNoteMetadata(config, zimReaderContainer) }

  val noteText = remember { mutableStateOf(TextFieldValue("")) }
  val noteEdited = remember { mutableStateOf(false) }

  val menuItems = remember {
    mutableStateOf(
      listOf(
        ActionMenuItem(
          Vector(Icons.Default.Delete),
          R.string.delete,
          { /* placeholder */ },
          isEnabled = false,
          testingTag = DELETE_MENU_BUTTON_TESTING_TAG
        ),
        ActionMenuItem(
          Vector(Icons.Default.Share),
          R.string.share,
          { /* placeholder */ },
          isEnabled = false,
          testingTag = SHARE_MENU_BUTTON_TESTING_TAG
        ),
        ActionMenuItem(
          Drawable(R.drawable.ic_save),
          R.string.save,
          { /* placeholder */ },
          isEnabled = false,
          testingTag = SAVE_MENU_BUTTON_TESTING_TAG
        )
      )
    )
  }

  val menuController = remember(metadata) {
    NoteMenuController(menuItems, metadata.isZimFileExist)
  }

  val noteOps = remember(metadata) {
    NoteOperations(
      context, scope, metadata, kiwixDataStore, mainRepositoryActions,
      noteText, noteEdited, snackBarHostState, menuController
    )
  }

  fun hideKeyboard() {
    val imm = context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
      as? InputMethodManager
    (context as? android.app.Activity)?.currentFocus?.windowToken?.let { token ->
      imm?.hideSoftInputFromWindow(token, 0)
    }
  }

  fun exitAddNoteDialog() {
    if (noteEdited.value) {
      alertDialogShower.show(
        KiwixDialog.NotesDiscardConfirmation,
        {
          onDismiss()
          hideKeyboard()
        }
      )
    } else {
      onDismiss()
      hideKeyboard()
    }
  }

  // Wire up menu items with actual actions and load existing note
  LaunchedEffect(Unit) {
    menuItems.value = listOf(
      ActionMenuItem(
        Vector(Icons.Default.Delete),
        R.string.delete,
        { noteOps.deleteNote() },
        isEnabled = false,
        testingTag = DELETE_MENU_BUTTON_TESTING_TAG
      ),
      ActionMenuItem(
        Vector(Icons.Default.Share),
        R.string.share,
        { noteOps.shareNote() },
        isEnabled = false,
        testingTag = SHARE_MENU_BUTTON_TESTING_TAG
      ),
      ActionMenuItem(
        Drawable(R.drawable.ic_save),
        R.string.save,
        { noteOps.saveNote() },
        isEnabled = false,
        testingTag = SAVE_MENU_BUTTON_TESTING_TAG
      )
    )

    // Display existing note
    val noteFile = File("${metadata.zimNotesDirectory}${metadata.articleNoteFileName}.txt")
    if (noteFile.exists()) {
      val noteFileText = noteFile.readText()
      noteText.value =
        TextFieldValue(noteFileText, selection = TextRange(noteFileText.length))
      menuController.enableShareMenuItem()
      menuController.enableDeleteMenuItem()
      if (!metadata.isZimFileExist) {
        menuController.disableSaveMenuItem()
      }
    }
  }

  Dialog(
    onDismissRequest = { exitAddNoteDialog() },
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false
    )
  ) {
    Surface(
      modifier = Modifier.fillMaxSize(),
      color = MaterialTheme.colorScheme.surface
    ) {
      AddNoteDialogScreen(
        articleTitle = metadata.articleTitle.toString(),
        navigationIcon = {
          NavigationIcon(
            iconItem = IconItem.Drawable(R.drawable.ic_close_white_24dp),
            onClick = { exitAddNoteDialog() },
            testingTag = ADD_NOTE_DIALOG_CLOSE_IMAGE_BUTTON_TESTING_TAG
          )
        },
        noteText = noteText.value,
        actionMenuItems = menuItems.value,
        onTextChange = { textFieldValue ->
          if (noteText.value.text != textFieldValue.text) {
            noteEdited.value = true
            menuController.enableSaveMenuItem()
            menuController.enableShareMenuItem()
          }
          noteText.value = textFieldValue
        },
        snackBarHostState = snackBarHostState
      )
      DialogHost(alertDialogShower)
    }
  }
}
