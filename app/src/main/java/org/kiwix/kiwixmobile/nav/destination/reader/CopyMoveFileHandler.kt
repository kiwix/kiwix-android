/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.reader

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleCoroutineScope
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.R.id
import org.kiwix.kiwixmobile.R.layout
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.Companion.FOUR_GIGABYTES_IN_KILOBYTES
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import java.io.File
import javax.inject.Inject

class CopyMoveFileHandler @Inject constructor(
  private val activity: Activity,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val alertDialogShower: AlertDialogShower,
  private val storageCalculator: StorageCalculator,
  private val fat32Checker: Fat32Checker
) {
  var fileCopyMoveCallback: FileCopyMoveCallback? = null
  private var selectedFileUri: Uri? = null
  private var selectedFile: File? = null
  private var copyMovePreparingDialog: Dialog? = null
  private var progressBarDialog: AlertDialog? = null
  var lifecycleScope: LifecycleCoroutineScope? = null
  private var progressBar: ProgressBar? = null
  private var progressBarTextView: TextView? = null
  private var isCopySelected = false
  var fileSystemDisposable: Disposable? = null

  private val copyMoveTitle: String by lazy {
    if (isCopySelected) {
      activity.getString(R.string.file_copying_in_progress)
    } else {
      activity.getString(R.string.file_moving_in_progress)
    }
  }

  private fun updateProgress(progress: Int) {
    progressBar?.post {
      progressBarTextView?.text = activity.getString(R.string.percentage, progress)
      progressBar?.setProgress(progress, true)
    }
  }

  fun showMoveFileToPublicDirectoryDialog(uri: Uri? = null, file: File? = null) {
    uri?.let { selectedFileUri = it }
    file?.let { selectedFile = it }
    if (!sharedPreferenceUtil.copyMoveZimFilePermissionDialog) {
      alertDialogShower.show(
        KiwixDialog.MoveFileToPublicDirectoryPermissionDialog,
        {
          sharedPreferenceUtil.copyMoveZimFilePermissionDialog = true
          validateAndShowCopyMoveDialog()
        }
      )
    } else {
      validateAndShowCopyMoveDialog()
    }
  }

  private fun isBookLessThan4GB(): Boolean =
    (selectedFile?.length() ?: 0L) < FOUR_GIGABYTES_IN_KILOBYTES

  private fun validateAndShowCopyMoveDialog() {
    hidePreparingCopyMoveDialog() // hide the dialog if already showing
    val availableSpace = storageCalculator.availableBytes()
    if (hasNotSufficientStorageSpace(availableSpace)) {
      fileCopyMoveCallback?.insufficientSpaceInStorage(availableSpace)
      return
    }
    when (fat32Checker.fileSystemStates.value) {
      DetectingFileSystem -> handleDetectingFileSystemState()
      CannotWrite4GbFile -> handleCannotWrite4GbFileState()
      else -> showCopyMoveDialog()
    }
  }

  private fun handleDetectingFileSystemState() {
    if (isBookLessThan4GB()) {
      showCopyMoveDialog()
    } else {
      showPreparingCopyMoveDialog()
      observeFileSystemState()
    }
  }

  private fun handleCannotWrite4GbFileState() {
    if (isBookLessThan4GB()) {
      showCopyMoveDialog()
    } else {
      // Show an error dialog indicating the file system limitation
      fileCopyMoveCallback?.filesystemDoesNotSupportedCopyMoveFilesOver4GB()
    }
  }

  private fun observeFileSystemState() {
    if (fileSystemDisposable?.isDisposed == false) return
    fileSystemDisposable = fat32Checker.fileSystemStates
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe {
        validateAndShowCopyMoveDialog()
      }
  }

  private fun showCopyMoveDialog() {
    alertDialogShower.show(
      KiwixDialog.CopyMoveFileToPublicDirectoryDialog,
      {
        isCopySelected = true
        copyZimFileToPublicAppDirectory()
      },
      {
        isCopySelected = false
        moveZimFileToPublicAppDirectory()
      }
    )
  }

  private fun copyZimFileToPublicAppDirectory() {
    lifecycleScope?.launch {
      val destinationFile = getDestinationFile()
      try {
        selectedFileUri?.let {
          showProgressDialog()
          val destinationUri = Uri.fromFile(destinationFile)
          copyFile(it, destinationUri)
          withContext(Dispatchers.Main) {
            dismissProgressDialog()
            fileCopyMoveCallback?.onFileCopied(destinationFile)
          }
        }
      } catch (ignore: Exception) {
        dismissProgressDialog()
        fileCopyMoveCallback?.onError(
          activity.getString(R.string.copy_file_error_message, ignore.message)
        ).also {
          // delete the temporary file if any error happens
          destinationFile.deleteFile()
          ignore.printStackTrace()
        }
      }
    }
  }

  private suspend fun copyFile(sourceUri: Uri, destinationUri: Uri) = withContext(Dispatchers.IO) {
    val inputStream = activity.contentResolver.openInputStream(sourceUri)
    val outputStream = activity.contentResolver.openOutputStream(destinationUri)
    if (inputStream != null && outputStream != null) {
      val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
      var bytesRead: Int
      var totalBytesCopied = 0L
      val fileSize = selectedFile?.length() ?: 0

      while (inputStream.read(buffer).also { bytesRead = it } != -1) {
        outputStream.write(buffer, 0, bytesRead)
        totalBytesCopied += bytesRead

        // Update progress (on main thread)
        withContext(Dispatchers.Main) {
          @Suppress("MagicNumber")
          val progress = (totalBytesCopied * 100 / fileSize).toInt()
          updateProgress(progress)
        }
      }

      outputStream.flush()
      inputStream.close()
      outputStream.close()
    } else {
      @Suppress("TooGenericExceptionThrown")
      throw Exception("Error accessing streams")
    }
  }

  @Suppress("UnsafeCallOnNullableType")
  private fun moveZimFileToPublicAppDirectory() {
    lifecycleScope?.launch {
      val destinationFile = getDestinationFile()
      try {
        selectedFileUri?.let { uri ->
          showProgressDialog()
          val destinationUri = Uri.fromFile(destinationFile)
          if (isSameStorage()) {
            // if we try to move the file in same storage.
            // then simply move the document.
            val contentResolver = activity.applicationContext.contentResolver
            val parentDocumentUri = getParentDocumentUri(uri)
            val destinationParentUri =
              getParentDocumentUri(DocumentFile.fromFile(destinationFile).uri)
            Log.e(
              "AUTHORITY",
              "moveZimFileToPublicAppDirectory: ${uri.authority} \n" +
                "destination = $parentDocumentUri \n uri ${parentDocumentUri?.authority}" +
                " after uri = ${destinationParentUri?.authority}"
            )
            DocumentsContract.moveDocument(
              contentResolver,
              uri,
              parentDocumentUri!!,
              destinationParentUri!!
            )
          } else {
            // if we move the document in other storage, then copy the file to configured storage
            // and delete the main file.
            copyFile(uri, destinationUri)
            DocumentsContract.deleteDocument(activity.applicationContext.contentResolver, uri)
          }
          withContext(Dispatchers.Main) {
            dismissProgressDialog()
            fileCopyMoveCallback?.onFileMoved(destinationFile)
          }
        }
      } catch (ignore: Exception) {
        dismissProgressDialog()
        fileCopyMoveCallback?.onError(
          activity.getString(R.string.move_file_error_message, ignore.message)
        ).also {
          // delete the temporary file if any error happens
          destinationFile.deleteFile()
          ignore.printStackTrace()
        }
      }
    }
  }

  private fun getParentDocumentUri(contentResolver: ContentResolver, documentUri: Uri): Uri? {
    val cursor = contentResolver.query(
      documentUri,
      arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
      null,
      null,
      null
    )

    cursor?.use {
      if (it.moveToFirst()) {
        val documentId = it.getString(0)
        val parentDocumentId = documentId.substringBeforeLast(':')
        val parentDocumentUri =
          DocumentsContract.buildDocumentUriUsingTree(documentUri, parentDocumentId)
        return@getParentDocumentUri parentDocumentUri
      }
    }
    return null
  }

  private fun getParentDocumentUri(documentUri: Uri): Uri? {
    val docId = DocumentsContract.getDocumentId(documentUri)
    val pathSegments = docId.split(":")
    val parentId = if (pathSegments.size > 1) pathSegments[1].substringBeforeLast("/") else ""
    val parentDocId = "${pathSegments[0]}:$parentId"

    return if (parentId.isNotEmpty() && documentUri.authority != null) {
      DocumentsContract.buildDocumentUri(documentUri.authority, parentDocId)
    } else {
      null
    }
  }

  private fun isSameStorage(): Boolean {
    return selectedFile?.path?.contains(
      sharedPreferenceUtil.prefStorage.substringBefore(
        activity.getString(R.string.android_directory_seperator)
      )
    ) == true
  }

  private fun getDestinationFile(): File =
    File("${sharedPreferenceUtil.prefStorage}/${selectedFile?.name}").also {
      if (!it.isFileExist()) it.createNewFile()
    }

  private fun hasNotSufficientStorageSpace(availableSpace: Long): Boolean =
    availableSpace < (selectedFile?.length() ?: 0L)

  @SuppressLint("InflateParams")
  private fun showPreparingCopyMoveDialog() {
    if (copyMovePreparingDialog == null) {
      val dialogView: View =
        activity.layoutInflater.inflate(R.layout.item_custom_spinner, null)
      copyMovePreparingDialog =
        alertDialogShower.create(KiwixDialog.PreparingCopyingFilesDialog { dialogView })
    }
    copyMovePreparingDialog?.show()
  }

  private fun hidePreparingCopyMoveDialog() {
    copyMovePreparingDialog?.dismiss()
  }

  @SuppressLint("InflateParams")
  private fun showProgressDialog() {
    val dialogView =
      activity.layoutInflater.inflate(layout.copy_move_progress_bar, null)
    progressBar =
      dialogView.findViewById<ProgressBar>(id.progressBar).apply {
        isIndeterminate = false
      }
    progressBarTextView =
      dialogView.findViewById(id.progressTextView)
    val builder = AlertDialog.Builder(activity).apply {
      setTitle(copyMoveTitle)
      setView(dialogView)
      setCancelable(false)
    }

    progressBarDialog = builder.create()
    progressBarDialog?.show()
  }

  private fun dismissProgressDialog() {
    if (progressBarDialog?.isShowing == true) {
      progressBarDialog?.dismiss()
    }
  }

  interface FileCopyMoveCallback {
    fun onFileCopied(file: File)
    fun onFileMoved(file: File)
    fun insufficientSpaceInStorage(availableSpace: Long)
    fun filesystemDoesNotSupportedCopyMoveFilesOver4GB()
    fun onError(errorMessage: String)
  }
}
