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
package org.kiwix.kiwixmobile.core.main

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import org.kiwix.kiwixmobile.core.CoreApp.Companion.coreComponent
import org.kiwix.kiwixmobile.core.CoreApp.Companion.instance
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.rememberSnackbarHostState
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Drawable
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Vector
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Created by @author Aditya-Sood (21/05/19) as a part of GSoC 2019
 *
 * AddNoteDialog extends DialogFragment and is used to display the note corresponding to a
 * particular article (of a particular zim file/wiki/book) as a full-screen dialog fragment.
 *
 * Notes are saved as text files at location: "{External Storage}/Kiwix/Notes/ZimFileName/ArticleUrl.txt"
 */

const val DISABLE_ICON_ITEM_ALPHA = 130
const val ENABLE_ICON_ITEM_ALPHA = 255

class AddNoteDialog : DialogFragment() {
  private lateinit var zimId: String
  private var zimFileName: String? = null
  private var zimFileTitle: String? = null
  private lateinit var zimFileUrl: String
  private var articleTitle: String? = null

  private val menuItems = mutableStateOf(actionMenuItems())
  private val noteText = mutableStateOf(TextFieldValue(""))
  lateinit var snackBarHostState: SnackbarHostState

  // Corresponds to "ArticleUrl" of "{External Storage}/Kiwix/Notes/ZimFileName/ArticleUrl.txt"
  private lateinit var articleNoteFileName: String
  private var noteFileExists = mutableStateOf(false)
  private var noteEdited = false

  // Keeps track of state of the note (whether edited since last save)
  // Stores path to directory for the currently open zim's notes
  private var zimNotesDirectory: String? = null

  @Inject
  lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  @Inject
  lateinit var zimReaderContainer: ZimReaderContainer

  @Inject
  lateinit var alertDialogShower: AlertDialogShower

  @Inject
  lateinit var mainRepositoryActions: MainRepositoryActions

  private var noteListItem: NoteListItem? = null
  private var zimReaderSource: ZimReaderSource? = null
  private var favicon: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    coreComponent
      .activityComponentBuilder()
      .activity(requireActivity())
      .build()
      .inject(this)

    if (arguments != null) {
      // For opening the note dialog from note screen.
      noteListItem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arguments?.getSerializable(NOTE_LIST_ITEM_TAG, NoteListItem::class.java)
      } else {
        @Suppress("DEPRECATION")
        arguments?.getSerializable(NOTE_LIST_ITEM_TAG) as NoteListItem
      }

      zimFileName = noteListItem?.zimReaderSource?.toDatabase()
      zimFileTitle = noteListItem?.title
      zimId = noteListItem?.zimId.orEmpty()
      zimReaderSource = noteListItem?.zimReaderSource
      favicon = noteListItem?.favicon
      articleNoteFileName = getArticleNoteFileName()
      zimNotesDirectory = noteListItem?.noteFilePath
        ?.substringBefore(articleNoteFileName)
      getArticleTitleAndZimFileUrlFromArguments()
    } else {
      // Note is opened from the reader screen.
      // Returns name of the form ".../Kiwix/granbluefantasy_en_all_all_nopic_2018-10.zim"
      zimFileName = zimReaderContainer.zimReaderSource?.toDatabase() ?: zimReaderContainer.name
      zimFileTitle = zimReaderContainer.zimFileTitle
      zimId = zimReaderContainer.id.orEmpty()
      zimReaderSource = zimReaderContainer.zimReaderSource
      favicon = zimReaderContainer.favicon
      val webView = (activity as WebViewProvider?)?.getCurrentWebView()
      articleTitle = webView?.title
      zimFileUrl = webView?.url.orEmpty()

      // Corresponds to "ZimFileName" of "{External Storage}/Kiwix/Notes/ZimFileName/ArticleUrl.txt"
      articleNoteFileName = getArticleNoteFileName()
      zimNotesDirectory = "$NOTES_DIRECTORY$zimNoteDirectoryName/"
    }
  }

  private fun getArticleTitleAndZimFileUrlFromArguments() {
    articleTitle = noteListItem?.title?.substringAfter(": ")
    zimFileUrl = noteListItem?.zimUrl.orEmpty()
  }

  private fun isZimFileExist() = zimFileName != null

  private fun onFailureToCreateAddNoteDialog() {
    context.toast(getString(R.string.error_file_not_found, zimFileName), Toast.LENGTH_LONG)
    parentFragmentManager.beginTransaction().remove(this).commit()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? = ComposeView(requireContext()).apply {
    setContent {
      snackBarHostState = rememberSnackbarHostState()
      AddNoteDialogScreen(
        articleTitle.toString(),
        navigationIcon = {
          NavigationIcon(
            iconItem = IconItem.Drawable(R.drawable.ic_close_white_24dp),
            onClick = {
              exitAddNoteDialog()
              closeKeyboard()
            }
          )
        },
        noteText = noteText.value,
        actionMenuItems = menuItems.value,
        onTextChange = { text -> enableSaveAndShareMenuButtonAndSetTextEdited(text) },
        isNoteFileExist = noteFileExists.value,
        snackBarHostState = snackBarHostState
      )
    }
  }

  fun updateMenuItem(vararg contentDescription: Int, isEnabled: Boolean) {
    menuItems.value = menuItems.value.map { item ->
      if (contentDescription.contains(item.contentDescription)) {
        item.copy(isEnabled = isEnabled)
      } else {
        item
      }
    }
  }

  private fun actionMenuItems() = listOf(
    ActionMenuItem(
      Vector(Icons.Default.Delete),
      R.string.delete,
      { deleteNote() },
      isEnabled = false
    ),
    ActionMenuItem(
      Vector(Icons.Default.Share),
      R.string.share,
      { shareNote() },
      isEnabled = false
    ),
    ActionMenuItem(
      Drawable(R.drawable.ic_save),
      R.string.save,
      { saveNote() },
      isEnabled = false
    )
  )

  private fun enableSaveAndShareMenuButtonAndSetTextEdited(text: String) {
    noteEdited = true
    noteText.value = TextFieldValue(
      text = text,
      // Moves cursor to end
      selection = TextRange(text.length)
    )
    enableSaveNoteMenuItem()
    enableShareNoteMenuItem()
  }

  private val zimNoteDirectoryName: String
    get() {
      val noteDirectoryName = getTextAfterLastSlashWithoutExtension(zimFileName.orEmpty())
      return (if (noteDirectoryName.isNotEmpty()) noteDirectoryName else zimFileTitle).orEmpty()
    }

  private fun getArticleNoteFileName(): String {
    // Returns url of the form: "content://org.kiwix.kiwixmobile.zim.base/A/Main_Page.html"
    noteListItem?.noteFilePath?.let {
      return@getArticleNoteFileName getTextAfterLastSlashWithoutExtension(it)
    }

    val articleUrl = (activity as WebViewProvider?)?.getCurrentWebView()?.url
    var noteFileName = ""
    if (articleUrl == null) {
      onFailureToCreateAddNoteDialog()
    } else {
      noteFileName = getTextAfterLastSlashWithoutExtension(articleUrl)
    }
    return noteFileName.ifEmpty { articleTitle }.orEmpty()
  }

  /* From ".../Kiwix/granbluefantasy_en_all_all_nopic_2018-10.zim", returns "granbluefantasy_en_all_all_nopic_2018-10"
   * From "content://org.kiwix.kiwixmobile.zim.base/A/Main_Page.html", returns "Main_Page"
   * For null input or on being unable to find required text, returns null
   */
  private fun getTextAfterLastSlashWithoutExtension(path: String): String =
    path.substringAfterLast('/', "").substringBeforeLast('.')

  // Add onBackPressedCallBack to respond to user pressing 'Back' button on navigation bar
  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = Dialog(requireContext(), theme)
    requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallBack)
    return dialog
  }

  private val onBackPressedCallBack =
    object : OnBackPressedCallback(true) {
      override fun handleOnBackPressed() {
        exitAddNoteDialog()
      }
    }

  private fun exitAddNoteDialog() {
    if (noteEdited) {
      alertDialogShower.show(KiwixDialog.NotesDiscardConfirmation, ::dismissAddNoteDialog)
    } else {
      // Closing unedited note dialog straightaway
      dismissAddNoteDialog()
    }
  }

  private fun disableMenuItems() {
    updateMenuItem(R.string.delete, R.string.share, R.string.save, isEnabled = false)
  }

  private fun disableSaveNoteMenuItem() {
    updateMenuItem(R.string.save, isEnabled = false)
  }

  private fun enableSaveNoteMenuItem() {
    if (isZimFileExist()) {
      updateMenuItem(R.string.save, isEnabled = true)
    }
  }

  private fun enableDeleteNoteMenuItem() {
    updateMenuItem(R.string.delete, isEnabled = true)
  }

  private fun enableShareNoteMenuItem() {
    updateMenuItem(R.string.share, isEnabled = true)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    // 'Share' disabled for empty notes, 'Save' disabled for unedited notes
    disableMenuItems()
    // Show the previously saved note if it exists
    displayNote()
  }

  private fun saveNote() {
    /* String content of the EditText, given by noteText, is saved into the text file given by:
     *    "{External Storage}/Kiwix/Notes/ZimFileTitle/ArticleTitle.txt"
     * */
    if (instance.isExternalStorageWritable) {
      if (ContextCompat.checkSelfPermission(
          requireContext(),
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED &&
        !sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove() &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
      ) {
        Log.d(
          TAG,
          "WRITE_EXTERNAL_STORAGE permission not granted"
        )
        context.toast(R.string.note_save_unsuccessful, Toast.LENGTH_LONG)
        return
      }
      val notesFolder = File(zimNotesDirectory)
      var folderExists = true
      if (!notesFolder.exists()) {
        // Try creating folder if it doesn't exist
        folderExists = notesFolder.mkdirs()
      }
      if (folderExists) {
        val noteFile =
          File(notesFolder.absolutePath, "$articleNoteFileName.txt")

        // Save note text-file code:
        try {
          noteFile.writeText(noteText.value.text)
          context.toast(R.string.note_save_successful, Toast.LENGTH_SHORT)
          noteEdited = false // As no unsaved changes remain
          enableDeleteNoteMenuItem()
          // adding only if saving file is success
          addNoteToDao(noteFile.canonicalPath, getNoteTitle())
          disableSaveNoteMenuItem()
        } catch (e: IOException) {
          e.printStackTrace()
            .also { context.toast(R.string.note_save_unsuccessful, Toast.LENGTH_LONG) }
        }
      } else {
        context.toast(R.string.note_save_unsuccessful, Toast.LENGTH_LONG)
        Log.d(TAG, "Required folder doesn't exist")
      }
    } else {
      context.toast(R.string.note_save_error_storage_not_writable, Toast.LENGTH_LONG)
    }
  }

  /**
   * This method determines the note title to be saved in the database.
   * - If the note is opened from the Reader screen, it combines the `zimFileTitle`
   *   and `articleTitle`, as it previously did.
   * - If `noteListItem` is not null, it means the note is opened from the Notes screen,
   *   as this item is passed in the bundle from the Notes screen. In this case, it
   *   returns `zimFileTitle`, which represents the current note's title.
   */
  private fun getNoteTitle(): String =
    noteListItem?.let {
      zimFileTitle
    } ?: run {
      "${zimFileTitle.orEmpty()}: $articleTitle"
    }

  private fun addNoteToDao(noteFilePath: String?, title: String) {
    noteFilePath?.let { filePath ->
      if (filePath.isNotEmpty() && zimFileUrl.isNotEmpty()) {
        val noteToSave = NoteListItem(
          zimId = zimId,
          title = title,
          url = zimFileUrl,
          noteFilePath = noteFilePath,
          zimReaderSource = zimReaderSource,
          favicon = favicon,
        )
        mainRepositoryActions.saveNote(noteToSave)
      } else {
        Log.d(TAG, "Cannot process with empty zim url or noteFilePath")
      }
    }
  }

  private fun deleteNote() {
    val notesFolder = File(zimNotesDirectory)
    val noteFile =
      File(notesFolder.absolutePath, "$articleNoteFileName.txt")
    val noteDeleted = noteFile.delete()
    val editedNoteText = noteText.value.text
    if (noteDeleted) {
      noteText.value = TextFieldValue("")
      mainRepositoryActions.deleteNote(getNoteTitle())
      disableMenuItems()
      snackBarHostState.snack(
        message = requireActivity().getString(R.string.note_delete_successful),
        actionLabel = requireActivity().getString(R.string.undo),
        actionClick = { restoreDeletedNote(editedNoteText) },
        lifecycleScope = lifecycleScope
      )
    } else {
      context.toast(R.string.note_delete_unsuccessful, Toast.LENGTH_LONG)
    }
  }

  private fun restoreDeletedNote(text: String) {
    enableSaveAndShareMenuButtonAndSetTextEdited(text)
  }

  /* String content of the note text file given at:
   *    "{External Storage}/Kiwix/Notes/ZimFileTitle/ArticleTitle.txt"
   * is displayed in the EditText field (note content area)
   */
  private fun displayNote() {
    val noteFile = File("$zimNotesDirectory$articleNoteFileName.txt")
    if (noteFile.exists()) {
      readNoteFromFile(noteFile)
    }

    // No action in case the note file for the currently open article doesn't exist
  }

  private fun readNoteFromFile(noteFile: File) {
    noteFileExists.value = true
    noteText.value = TextFieldValue(noteFile.readText())
    enableShareNoteMenuItem() // As note content exists which can be shared
    enableDeleteNoteMenuItem()
    if (!isZimFileExist()) {
      // hide the save button if the ZIM file is not exist.
      disableSaveNoteMenuItem()
    }
  }

  private fun shareNote() {
    /* The note text file corresponding to the currently open article, given at:
     *    "{External Storage}/Kiwix/Notes/ZimFileTitle/ArticleTitle.txt"
     * is shared via an app-chooser intent
     * */
    if (noteEdited && isZimFileExist()) {
      // Save edited note before sharing the text file
      saveNote()
    }
    val noteFile = File("$zimNotesDirectory$articleNoteFileName.txt")
    if (noteFile.exists()) {
      // From Nougat 7 (API 24) access to files is shared temporarily with other apps
      // Need to use FileProvider for the same
      val noteFileUri = FileProvider.getUriForFile(
        requireContext(),
        requireContext().packageName + ".fileprovider",
        noteFile
      )
      val noteFileShareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, noteFileUri)
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
      }
      val shareChooser = Intent.createChooser(
        noteFileShareIntent,
        getString(R.string.note_share_app_chooser_title)
      )
      if (noteFileShareIntent.resolveActivity(requireActivity().packageManager) != null) {
        startActivity(shareChooser)
      }
    } else {
      context.toast(R.string.note_share_error_file_missing, Toast.LENGTH_SHORT)
    }
  }

  private fun dismissAddNoteDialog() {
    dialog?.dismiss()
    closeKeyboard()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    mainRepositoryActions.dispose()
    onBackPressedCallBack.remove()
  }

  override fun onStart() {
    super.onStart()
    dialog?.let {
      it.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
  }

  companion object {
    @JvmField val NOTES_DIRECTORY =
      instance.getExternalFilesDir("").toString() + "/Kiwix/Notes/"
    const val TAG = "AddNoteDialog"
    const val NOTE_LIST_ITEM_TAG = "NoteListItemTag"
  }
}
