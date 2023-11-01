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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import org.kiwix.kiwixmobile.core.CoreApp.Companion.coreComponent
import org.kiwix.kiwixmobile.core.CoreApp.Companion.instance
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.databinding.DialogAddNoteBinding
import org.kiwix.kiwixmobile.core.extensions.closeKeyboard
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.page.notes.adapter.NoteListItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.SimpleTextWatcher
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
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

// constant
const val DISABLE_ICON_ITEM_ALPHA = 130
const val ENABLE_ICON_ITEM_ALPHA = 255

class AddNoteDialog : DialogFragment() {
  private lateinit var zimId: String
  private var zimFileName: String? = null
  private var zimFileTitle: String? = null
  private lateinit var zimFileUrl: String
  private var articleTitle: String? = null

  // Corresponds to "ArticleUrl" of "{External Storage}/Kiwix/Notes/ZimFileName/ArticleUrl.txt"
  private lateinit var articleNoteFileName: String
  private var noteFileExists = false
  private var noteEdited = false

  private var dialogNoteAddNoteBinding: DialogAddNoteBinding? = null

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

  private val toolbar by lazy {
    dialogNoteAddNoteBinding?.root?.findViewById<Toolbar>(R.id.toolbar)
  }

  private val saveItem by lazy { toolbar?.menu?.findItem(R.id.save_note) }

  private val shareItem by lazy { toolbar?.menu?.findItem(R.id.share_note) }

  private val deleteItem by lazy { toolbar?.menu?.findItem(R.id.delete_note) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    coreComponent
      .activityComponentBuilder()
      .activity(requireActivity())
      .build()
      .inject(this)

    // Returns name of the form ".../Kiwix/granbluefantasy_en_all_all_nopic_2018-10.zim"
    zimFileName = zimReaderContainer.zimCanonicalPath ?: zimReaderContainer.name
    if (zimFileName != null) { // No zim file currently opened
      zimFileTitle = zimReaderContainer.zimFileTitle
      zimId = zimReaderContainer.id.orEmpty()

      if (arguments != null) {
        articleTitle = arguments?.getString(NOTES_TITLE)
        zimFileUrl = arguments?.getString(ARTICLE_URL).orEmpty()
      } else {
        val webView = (activity as WebViewProvider?)?.getCurrentWebView()
        articleTitle = webView?.title
        zimFileUrl = webView?.url.orEmpty()
      }

      // Corresponds to "ZimFileName" of "{External Storage}/Kiwix/Notes/ZimFileName/ArticleUrl.txt"
      articleNoteFileName = getArticleNoteFileName()
      zimNotesDirectory = "$NOTES_DIRECTORY$zimNoteDirectoryName/"
    }
  }

  private fun onFailureToCreateAddNoteDialog() {
    context.toast(R.string.error_file_not_found, Toast.LENGTH_LONG)
    closeKeyboard()
    parentFragmentManager.beginTransaction().remove(this).commit()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    super.onCreateView(inflater, container, savedInstanceState)
    dialogNoteAddNoteBinding = DialogAddNoteBinding.inflate(inflater, container, false)
    return dialogNoteAddNoteBinding?.root
  }

  private val zimNoteDirectoryName: String
    get() {
      val noteDirectoryName = getTextAfterLastSlashWithoutExtension(zimFileName ?: "")
      return (if (noteDirectoryName.isNotEmpty()) noteDirectoryName else zimFileTitle) ?: ""
    }

  private fun getArticleNoteFileName(): String {
    // Returns url of the form: "content://org.kiwix.kiwixmobile.zim.base/A/Main_Page.html"
    arguments?.getString(NOTE_FILE_PATH)?.let {
      return@getArticleNoteFileName getTextAfterLastSlashWithoutExtension(it)
    }

    val articleUrl = (activity as WebViewProvider?)?.getCurrentWebView()?.url
    var noteFileName = ""
    if (articleUrl == null) {
      onFailureToCreateAddNoteDialog()
    } else {
      noteFileName = getTextAfterLastSlashWithoutExtension(articleUrl)
    }
    return noteFileName.ifEmpty { articleTitle } ?: ""
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
    if (dialogNoteAddNoteBinding?.addNoteEditText?.isFocused == true) {
      dialogNoteAddNoteBinding?.addNoteEditText?.clearFocus()
    }
  }

  private fun disableMenuItems() {
    if (toolbar?.menu != null) {
      saveItem?.isEnabled = false
      shareItem?.isEnabled = false
      deleteItem?.isEnabled = false
      saveItem?.icon?.alpha = DISABLE_ICON_ITEM_ALPHA
      shareItem?.icon?.alpha = DISABLE_ICON_ITEM_ALPHA
      deleteItem?.icon?.alpha = DISABLE_ICON_ITEM_ALPHA
    } else {
      Log.d(TAG, "Toolbar without inflated menu")
    }
  }

  private fun disableSaveNoteMenuItem() {
    if (toolbar?.menu != null) {
      saveItem?.isEnabled = false
      saveItem?.icon?.alpha = DISABLE_ICON_ITEM_ALPHA
    } else {
      Log.d(TAG, "Toolbar without inflated menu")
    }
  }

  private fun enableSaveNoteMenuItem() {
    if (toolbar?.menu != null) {
      saveItem?.isEnabled = true
      saveItem?.icon?.alpha = ENABLE_ICON_ITEM_ALPHA
    } else {
      Log.d(TAG, "Toolbar without inflated menu")
    }
  }

  private fun enableDeleteNoteMenuItem() {
    if (toolbar?.menu != null) {
      deleteItem?.isEnabled = true
      deleteItem?.icon?.alpha = ENABLE_ICON_ITEM_ALPHA
    } else {
      Log.d(TAG, "Toolbar without inflated menu")
    }
  }

  private fun enableShareNoteMenuItem() {
    if (toolbar?.menu != null) {
      shareItem?.isEnabled = true
      shareItem?.icon?.alpha = ENABLE_ICON_ITEM_ALPHA
    } else {
      Log.d(TAG, "Toolbar without inflated menu")
    }
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    toolbar?.setTitle(R.string.note)
    toolbar?.setNavigationIcon(R.drawable.ic_close_white_24dp)
    toolbar?.setNavigationOnClickListener {
      exitAddNoteDialog()
      closeKeyboard()
    }
    toolbar?.setOnMenuItemClickListener { item: MenuItem ->
      when (item.itemId) {
        R.id.share_note -> shareNote()
        R.id.save_note -> saveNote(dialogNoteAddNoteBinding?.addNoteEditText?.text.toString())
        R.id.delete_note -> deleteNote()
      }
      true
    }
    toolbar?.inflateMenu(R.menu.menu_add_note_dialog)
    // 'Share' disabled for empty notes, 'Save' disabled for unedited notes
    disableMenuItems()
    dialogNoteAddNoteBinding?.addNoteTextView?.text = articleTitle

    // Show the previously saved note if it exists
    displayNote()
    dialogNoteAddNoteBinding?.addNoteEditText?.addTextChangedListener(
      SimpleTextWatcher { _, _, _, _ ->
        noteEdited = true
        enableSaveNoteMenuItem()
        enableShareNoteMenuItem()
      }
    )
    if (!noteFileExists) {
      // Prepare for input in case of empty/new note
      dialogNoteAddNoteBinding?.addNoteEditText?.apply {
        requestFocus()
        showKeyboard(this)
      }
    }
  }

  @Suppress("MagicNumber")
  private fun showKeyboard(editText: EditText) {
    val inputMethodManager =
      requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    editText.postDelayed(
      {
        inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
      }, 100
    )
  }

  private fun saveNote(noteText: String) {
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
          noteFile.writeText(noteText)
          context.toast(R.string.note_save_successful, Toast.LENGTH_SHORT)
          noteEdited = false // As no unsaved changes remain
          enableDeleteNoteMenuItem()
          // adding only if saving file is success
          addNoteToDao(noteFile.canonicalPath, "${zimFileTitle.orEmpty()}: $articleTitle")
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

  private fun addNoteToDao(noteFilePath: String?, title: String) {
    noteFilePath?.let { filePath ->
      if (filePath.isNotEmpty() && zimFileUrl.isNotEmpty()) {
        val zimReader = zimReaderContainer.zimFileReader
        if (zimReader != null) {
          val noteToSave = NoteListItem(
            title = title,
            url = zimFileUrl,
            noteFilePath = noteFilePath,
            zimFileReader = zimReader
          )
          mainRepositoryActions.saveNote(noteToSave)
        } else {
          Log.d(TAG, "zim reader found null")
        }
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
    val noteText = dialogNoteAddNoteBinding?.addNoteEditText?.text.toString()
    if (noteDeleted) {
      dialogNoteAddNoteBinding?.addNoteEditText?.text?.clear()
      mainRepositoryActions.deleteNote(articleNoteFileName)
      disableMenuItems()
      view?.snack(
        stringId = R.string.note_delete_successful,
        actionStringId = R.string.undo,
        actionClick = { restoreDeletedNote(noteText) }
      )
    } else {
      context.toast(R.string.note_delete_unsuccessful, Toast.LENGTH_LONG)
    }
  }

  private fun restoreDeletedNote(text: String) {
    dialogNoteAddNoteBinding?.addNoteEditText?.setText(text)
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
    noteFileExists = true
    val contents = noteFile.readText()
    dialogNoteAddNoteBinding?.addNoteEditText?.apply {
      setText(contents) // Display the note content
      text?.takeIf(Editable::isNotEmpty)?.let { text ->
        val selection = text.length - 1
        setSelection(selection.coerceAtLeast(0))
      }
    }
    enableShareNoteMenuItem() // As note content exists which can be shared
    enableDeleteNoteMenuItem()
  }

  private fun shareNote() {

    /* The note text file corresponding to the currently open article, given at:
     *    "{External Storage}/Kiwix/Notes/ZimFileTitle/ArticleTitle.txt"
     * is shared via an app-chooser intent
     * */
    if (noteEdited) {
      // Save edited note before sharing the text file
      saveNote(dialogNoteAddNoteBinding?.addNoteEditText?.text.toString())
    }
    val noteFile = File("$zimNotesDirectory$articleNoteFileName.txt")
    if (noteFile.exists()) {
      // From Nougat 7 (API 24) access to files is shared temporarily with other apps
      // Need to use FileProvider for the same
      val noteFileUri = FileProvider.getUriForFile(
        requireContext(), requireContext().packageName + ".fileprovider",
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
    dialogNoteAddNoteBinding = null
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
    const val NOTE_FILE_PATH = "NoteFilePath"
    const val ARTICLE_URL = "ArticleUrl"
    const val NOTES_TITLE = "NotesTitle"
  }
}
