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
import android.net.Uri
import android.provider.DocumentsContract
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.Companion.FOUR_GIGABYTES_IN_KILOBYTES
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import java.io.File
import java.io.FileNotFoundException
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
  private var isMoveOperation = false
  var fileSystemDisposable: Disposable? = null

  private val copyMoveTitle: String by lazy {
    if (isMoveOperation) {
      activity.getString(R.string.file_moving_in_progress)
    } else {
      activity.getString(R.string.file_copying_in_progress)
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
        isMoveOperation = false
        copyMoveZimFileToPublicAppDirectory()
      },
      {
        isMoveOperation = true
        copyMoveZimFileToPublicAppDirectory()
      }
    )
  }

  private fun copyMoveZimFileToPublicAppDirectory() {
    lifecycleScope?.launch {
      val destinationFile = getDestinationFile()
      try {
        if (selectedFileUri == null) {
          throw FileNotFoundException("Selected file not found")
        }
        selectedFileUri?.let {
          showProgressDialog()
          val destinationUri = Uri.fromFile(destinationFile)
          copyFile(it, destinationUri)
          if (isMoveOperation) {
            // delete the source file after successfully moved.
            deleteSourceFile(it)
          }
          // val contentResolver = activity.applicationContext.contentResolver
          // val parentDocumentUri = getParentDocumentUri(it)
          // val documentFile = DocumentFile.fromSingleUri(activity, it)
          // val destinationParentUri = getContentUriFromFilePath(destinationFile.parentFile.path)
          // Log.e(
          //   "AUTHORITY",
          //   "moveZimFileToPublicAppDirectory: ${it.authority} \n" +
          //     "destination = $parentDocumentUri \n uri ${parentDocumentUri?.authority}" +
          //     " after uri = ${destinationParentUri?.authority}"
          // )
          // if (isSameStorage()) {
          //   // if we try to move the file in same storage.
          //   // then simply move the document.
          //   DocumentsContract.copyDocument(
          //     contentResolver,
          //     it,
          //     destinationParentUri!!
          //   )
          // } else {
          //   // if we move the document in other storage, then copy the file to configured storage
          //   // and delete the main file.
          //   copyFile(it, destinationUri)
          //   deleteSourceFile(it)
          // }
          withContext(Dispatchers.Main) {
            notifyFileOperationSuccess(destinationFile)
          }
        }
      } catch (ignore: Exception) {
        ignore.printStackTrace()
        handleFileOperationError(ignore.message, destinationFile)
      }
    }
  }

  // private fun getContentUriFromFilePath(filePath: String): Uri? {
  //   val basePath = Environment.getExternalStorageDirectory().absolutePath
  //   val relativePath = filePath.removePrefix(basePath).removePrefix("/")
  //   val documentId = "primary:$relativePath"
  //   return Uri.parse("content://com.android.externalstorage.documents/document/$documentId")
  // }

  private fun handleFileOperationError(
    errorMessage: String?,
    destinationFile: File
  ) {
    dismissProgressDialog()
    val userFriendlyMessage = if (isMoveOperation) {
      activity.getString(R.string.move_file_error_message, errorMessage)
    } else {
      activity.getString(R.string.copy_file_error_message, errorMessage)
    }
    fileCopyMoveCallback?.onError(userFriendlyMessage).also {
      // Clean up the destination file if an error occurs
      destinationFile.deleteFile()
    }
  }

  private fun notifyFileOperationSuccess(destinationFile: File) {
    dismissProgressDialog()
    if (isMoveOperation) {
      fileCopyMoveCallback?.onFileMoved(destinationFile)
    } else {
      fileCopyMoveCallback?.onFileCopied(destinationFile)
    }
  }

  private fun deleteSourceFile(uri: Uri) {
    try {
      DocumentsContract.deleteDocument(activity.applicationContext.contentResolver, uri)
    } catch (ignore: Exception) {
      ignore.printStackTrace()
    }
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
      throw FileNotFoundException("The selected zim file could not open")
    }
  }

  private fun getDestinationFile(): File {
    val root = File(sharedPreferenceUtil.prefStorage)
    val fileName = selectedFile?.name ?: ""

    val destinationFile = sequence {
      yield(File(root, fileName))
      yieldAll(
        generateSequence(1) { it + 1 }.map {
          File(root, fileName.replace(".", "_$it."))
        }
      )
    }.first { !it.isFileExist() }

    destinationFile.createNewFile()
    return destinationFile
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
