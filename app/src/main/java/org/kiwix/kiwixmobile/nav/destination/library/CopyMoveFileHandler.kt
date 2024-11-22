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

package org.kiwix.kiwixmobile.nav.destination.library

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
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentManager
import eu.mhutti1.utils.storage.STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
import eu.mhutti1.utils.storage.StorageDevice
import eu.mhutti1.utils.storage.StorageDeviceUtils
import eu.mhutti1.utils.storage.StorageSelectDialog
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.R.id
import org.kiwix.kiwixmobile.R.layout
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.Companion.FOUR_GIGABYTES_IN_KILOBYTES
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import org.kiwix.libzim.Archive
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import javax.inject.Inject

class CopyMoveFileHandler @Inject constructor(
  private val activity: Activity,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val alertDialogShower: AlertDialogShower,
  private val storageCalculator: StorageCalculator,
  private val fat32Checker: Fat32Checker
) {
  private var fileCopyMoveCallback: FileCopyMoveCallback? = null
  private var selectedFileUri: Uri? = null
  private var selectedFile: DocumentFile? = null
  private var copyMovePreparingDialog: Dialog? = null
  private var progressBarDialog: AlertDialog? = null
  private var lifecycleScope: CoroutineScope? = null
  private var progressBar: ProgressBar? = null
  private var progressBarTextView: TextView? = null
  var isMoveOperation = false
  var shouldValidateZimFile: Boolean = false
  private var fileSystemDisposable: Disposable? = null
  private lateinit var fragmentManager: FragmentManager
  val storageDeviceList by lazy {
    StorageDeviceUtils.getWritableStorage(activity)
  }

  private val copyMoveTitle: String by lazy {
    if (isMoveOperation) {
      activity.getString(R.string.moving_zim_file)
    } else {
      activity.getString(R.string.copying_zim_file)
    }
  }

  private fun updateProgress(progress: Int) {
    progressBar?.post {
      progressBarTextView?.text = activity.getString(R.string.percentage, progress)
      progressBar?.setProgress(progress, true)
    }
  }

  fun showMoveFileToPublicDirectoryDialog(
    uri: Uri? = null,
    documentFile: DocumentFile? = null,
    shouldValidateZimFile: Boolean = false,
    fragmentManager: FragmentManager
  ) {
    this.shouldValidateZimFile = shouldValidateZimFile
    this.fragmentManager = fragmentManager
    setSelectedFileAndUri(uri, documentFile)
    if (sharedPreferenceUtil.shouldShowStorageSelectionDialog && storageDeviceList.size > 1) {
      // Show dialog to select storage if more than one storage device is available, and user
      // have not configured the storage yet.
      showCopyMoveDialog(true)
    } else {
      if (storageDeviceList.size == 1) {
        // If only internal storage is currently available, set shouldShowStorageSelectionDialog
        // to true. This allows the storage configuration dialog to be shown again if the
        // user removes an external storage device (like an SD card) and then reinserts it.
        // This ensures they are prompted to configure storage settings upon SD card reinsertion.
        sharedPreferenceUtil.shouldShowStorageSelectionDialog = true
      }
      if (validateZimFileCanCopyOrMove()) {
        showCopyMoveDialog()
      }
    }
  }

  fun setSelectedFileAndUri(uri: Uri?, documentFile: DocumentFile?) {
    uri?.let { selectedFileUri = it }
    documentFile?.let { selectedFile = it }
  }

  fun setFileCopyMoveCallback(fileCopyMoveCallback: FileCopyMoveCallback?) {
    this.fileCopyMoveCallback = fileCopyMoveCallback
  }

  fun setLifeCycleScope(coroutineScope: CoroutineScope?) {
    lifecycleScope = coroutineScope
  }

  fun showStorageSelectDialog() = StorageSelectDialog()
    .apply {
      onSelectAction = ::copyMoveZIMFileInSelectedStorage
      titleSize = STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
      setStorageDeviceList(storageDeviceList)
      setShouldShowCheckboxSelected(false)
    }
    .show(
      fragmentManager,
      activity.getString(R.string.choose_storage_to_copy_move_zim_file)
    )

  fun copyMoveZIMFileInSelectedStorage(storageDevice: StorageDevice) {
    sharedPreferenceUtil.apply {
      shouldShowStorageSelectionDialog = false
      putPrefStorage(sharedPreferenceUtil.getPublicDirectoryPath(storageDevice.name))
      putStoragePosition(
        if (storageDevice.isInternal) INTERNAL_SELECT_POSITION
        else EXTERNAL_SELECT_POSITION
      )
    }
    if (validateZimFileCanCopyOrMove()) {
      performCopyMoveOperation()
    }
  }

  private fun performCopyMoveOperation() {
    if (isMoveOperation) {
      performMoveOperation()
    } else {
      performCopyOperation()
    }
  }

  fun isBookLessThan4GB(): Boolean =
    (selectedFile?.length() ?: 0L) < FOUR_GIGABYTES_IN_KILOBYTES

  private fun hasNotSufficientStorageSpace(availableSpace: Long): Boolean =
    availableSpace < (selectedFile?.length() ?: 0L)

  fun validateZimFileCanCopyOrMove(file: File = File(sharedPreferenceUtil.prefStorage)): Boolean {
    hidePreparingCopyMoveDialog() // hide the dialog if already showing
    val availableSpace = storageCalculator.availableBytes(file)
    if (hasNotSufficientStorageSpace(availableSpace)) {
      fileCopyMoveCallback?.insufficientSpaceInStorage(availableSpace)
      return false
    }
    return when (fat32Checker.fileSystemStates.value) {
      DetectingFileSystem -> {
        handleDetectingFileSystemState()
        false
      }

      CannotWrite4GbFile -> {
        handleCannotWrite4GbFileState()
        false
      }

      else -> true
    }
  }

  fun handleDetectingFileSystemState() {
    if (isBookLessThan4GB()) {
      performCopyMoveOperationIfSufficientSpaceAvailable()
    } else {
      showPreparingCopyMoveDialog()
      observeFileSystemState()
    }
  }

  fun handleCannotWrite4GbFileState() {
    if (isBookLessThan4GB()) {
      performCopyMoveOperationIfSufficientSpaceAvailable()
    } else {
      // Show an error dialog indicating the file system limitation
      fileCopyMoveCallback?.filesystemDoesNotSupportedCopyMoveFilesOver4GB()
    }
  }

  fun observeFileSystemState() {
    if (fileSystemDisposable?.isDisposed == false) return
    fileSystemDisposable = fat32Checker.fileSystemStates
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe {
        hidePreparingCopyMoveDialog()
        if (validateZimFileCanCopyOrMove()) {
          performCopyMoveOperation()
        }
      }
  }

  fun performCopyMoveOperationIfSufficientSpaceAvailable() {
    val availableSpace = storageCalculator.availableBytes(File(sharedPreferenceUtil.prefStorage))
    if (hasNotSufficientStorageSpace(availableSpace)) {
      fileCopyMoveCallback?.insufficientSpaceInStorage(availableSpace)
    } else {
      performCopyMoveOperation()
    }
  }

  fun showCopyMoveDialog(showStorageSelectionDialog: Boolean = false) {
    alertDialogShower.show(
      KiwixDialog.CopyMoveFileToPublicDirectoryDialog,
      { performCopyOperation(showStorageSelectionDialog) },
      { performMoveOperation(showStorageSelectionDialog) }
    )
  }

  fun performCopyOperation(showStorageSelectionDialog: Boolean = false) {
    isMoveOperation = false
    if (showStorageSelectionDialog) {
      showStorageSelectDialog()
    } else {
      copyZimFileToPublicAppDirectory()
    }
  }

  fun performMoveOperation(showStorageSelectionDialog: Boolean = false) {
    isMoveOperation = true
    if (showStorageSelectionDialog) {
      showStorageSelectDialog()
    } else {
      moveZimFileToPublicAppDirectory()
    }
  }

  private fun copyZimFileToPublicAppDirectory() {
    lifecycleScope?.launch {
      val destinationFile = getDestinationFile()
      try {
        val sourceUri = selectedFileUri ?: throw FileNotFoundException("Selected file not found")
        showProgressDialog()
        copyFile(sourceUri, destinationFile)
        withContext(Dispatchers.Main) {
          notifyFileOperationSuccess(destinationFile, sourceUri)
        }
      } catch (ignore: Exception) {
        ignore.printStackTrace()
        handleFileOperationError(
          activity.getString(R.string.copy_file_error_message, ignore.message),
          destinationFile
        )
      }
    }
  }

  @Suppress("UnsafeCallOnNullableType")
  private fun moveZimFileToPublicAppDirectory() {
    lifecycleScope?.launch {
      val destinationFile = getDestinationFile()
      try {
        val sourceUri = selectedFileUri ?: throw FileNotFoundException("Selected file not found")
        showProgressDialog()
        val moveSuccess = selectedFile?.parentFile?.uri?.let { parentUri ->
          tryMoveWithDocumentContract(
            sourceUri,
            parentUri,
            DocumentFile.fromFile(File(sharedPreferenceUtil.prefStorage)).uri
          )
        } ?: run {
          copyFile(sourceUri, destinationFile)
          true
        }
        withContext(Dispatchers.Main) {
          if (moveSuccess) {
            notifyFileOperationSuccess(destinationFile, sourceUri)
          } else {
            handleFileOperationError(
              activity.getString(R.string.move_file_error_message, "File move failed"),
              destinationFile
            )
          }
        }
      } catch (ignore: Exception) {
        ignore.printStackTrace()
        handleFileOperationError(
          activity.getString(R.string.move_file_error_message, ignore.message),
          destinationFile
        )
      }
    }
  }

  fun tryMoveWithDocumentContract(
    selectedUri: Uri,
    sourceParentFolderUri: Uri,
    destinationFolderUri: Uri
  ): Boolean {
    return try {
      val contentResolver = activity.contentResolver
      if (documentCanMove(selectedUri, contentResolver)) {
        DocumentsContract.moveDocument(
          contentResolver,
          selectedUri,
          sourceParentFolderUri,
          destinationFolderUri
        )
        true
      } else {
        false
      }
    } catch (ignore: Exception) {
      ignore.printStackTrace()
      false
    }
  }

  private fun documentCanMove(uri: Uri, contentResolver: ContentResolver): Boolean {
    if (!DocumentsContract.isDocumentUri(activity, uri)) return false

    val flags =
      contentResolver.query(
        uri,
        arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
        null,
        null,
        null
      )
        ?.use { cursor ->
          if (cursor.moveToFirst()) cursor.getInt(0) else 0
        } ?: 0

    return flags and DocumentsContract.Document.FLAG_SUPPORTS_MOVE != 0
  }

  fun handleFileOperationError(
    errorMessage: String?,
    destinationFile: File
  ) {
    dismissProgressDialog()
    fileCopyMoveCallback?.onError("$errorMessage").also {
      // Clean up the destination file if an error occurs
      lifecycleScope?.launch {
        destinationFile.deleteFile()
      }
    }
  }

  suspend fun notifyFileOperationSuccess(destinationFile: File, sourceUri: Uri) {
    if (shouldValidateZimFile && !isValidZimFile(destinationFile)) {
      handleInvalidZimFile(destinationFile, sourceUri)
      return
    }
    dismissProgressDialog()
    if (isMoveOperation) {
      deleteSourceFile(sourceUri)
      fileCopyMoveCallback?.onFileMoved(destinationFile)
    } else {
      fileCopyMoveCallback?.onFileCopied(destinationFile)
    }
  }

  fun handleInvalidZimFile(destinationFile: File, sourceUri: Uri) {
    val errorMessage = activity.getString(R.string.error_file_invalid)
    if (isMoveOperation) {
      val moveSuccessful = tryMoveWithDocumentContract(
        destinationFile.toUri(),
        destinationFile.parentFile.toUri(),
        sourceUri
      )

      if (moveSuccessful) {
        // If files is moved back using the documentContract then show the error message to user
        dismissProgressDialog()
        fileCopyMoveCallback?.onError(errorMessage)
      } else {
        // Show error message and delete the moved file if move failed.
        handleFileOperationError(errorMessage, destinationFile)
      }
    } else {
      // For copy operation, show error message and delete the copied file.
      handleFileOperationError(errorMessage, destinationFile)
    }
  }

  suspend fun isValidZimFile(destinationFile: File): Boolean {
    var archive: Archive? = null
    return try {
      // create archive object, and check if it has the mainEntry or not to validate the ZIM file.
      archive = ZimReaderSource(destinationFile).createArchive()
      archive?.hasMainEntry() == true
    } catch (ignore: Exception) {
      // if it is a invalid ZIM file
      false
    } finally {
      archive?.dispose()
    }
  }

  fun deleteSourceFile(uri: Uri) {
    try {
      DocumentsContract.deleteDocument(activity.applicationContext.contentResolver, uri)
    } catch (ignore: Exception) {
      selectedFile?.delete()
      ignore.printStackTrace()
    }
  }

  @Suppress("MagicNumber")
  private suspend fun copyFile(sourceUri: Uri, destinationFile: File) =
    withContext(Dispatchers.IO) {
      val contentResolver = activity.contentResolver

      val parcelFileDescriptor = contentResolver.openFileDescriptor(sourceUri, "r")
      val fileSize =
        parcelFileDescriptor?.fileDescriptor?.let { FileInputStream(it).channel.size() } ?: 0L
      var totalBytesTransferred = 0L

      parcelFileDescriptor?.use { pfd ->
        val sourceFd = pfd.fileDescriptor
        FileInputStream(sourceFd).channel.use { sourceChannel ->
          FileOutputStream(destinationFile).channel.use { destinationChannel ->
            var bytesTransferred: Long
            val bufferSize = 1024 * 1024
            while (totalBytesTransferred < fileSize) {
              // Transfer data from source to destination in chunks
              bytesTransferred = sourceChannel.transferTo(
                totalBytesTransferred,
                bufferSize.toLong(),
                destinationChannel
              )
              totalBytesTransferred += bytesTransferred
              val progress = (totalBytesTransferred * 100 / fileSize).toInt()
              withContext(Dispatchers.Main) {
                updateProgress(progress)
              }
            }
          }
        }
      } ?: throw FileNotFoundException("The selected file could not be opened")
    }

  fun getDestinationFile(): File {
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

  @SuppressLint("InflateParams") fun showPreparingCopyMoveDialog() {
    if (copyMovePreparingDialog == null) {
      val dialogView: View =
        activity.layoutInflater.inflate(layout.item_custom_spinner, null)
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

  fun dismissProgressDialog() {
    if (progressBarDialog?.isShowing == true) {
      progressBarDialog?.dismiss()
    }
  }

  fun dispose() {
    fileSystemDisposable?.dispose()
    setFileCopyMoveCallback(null)
    setLifeCycleScope(null)
  }

  interface FileCopyMoveCallback {
    fun onFileCopied(file: File)
    fun onFileMoved(file: File)
    fun insufficientSpaceInStorage(availableSpace: Long)
    fun filesystemDoesNotSupportedCopyMoveFilesOver4GB()
    fun onError(errorMessage: String)
  }
}
