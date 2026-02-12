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

import android.app.Activity
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentManager
import eu.mhutti1.utils.storage.StorageDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.local.CopyMoveProgressBarController
import org.kiwix.kiwixmobile.nav.destination.library.local.FileOperationHandler
import org.kiwix.kiwixmobile.nav.destination.library.local.MultipleFilesProcessAction
import org.kiwix.kiwixmobile.storage.STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
import org.kiwix.kiwixmobile.storage.StorageSelectDialog
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.Companion.FOUR_GIGABYTES_IN_KILOBYTES
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import org.kiwix.libzim.Archive
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

const val COPY_MOVE_DIALOG_TITLE_TESTING_TAG = "copyMoveDialogTitleTestingTag"

@Suppress("LongParameterList")
class CopyMoveFileHandler @Inject constructor(
  private val activity: Activity,
  private val kiwixDataStore: KiwixDataStore,
  private val storageCalculator: StorageCalculator,
  private val fat32Checker: Fat32Checker,
  private val fileOperationHandler: FileOperationHandler,
  private val copyMoveProgressBarController: CopyMoveProgressBarController
) {
  private var fileCopyMoveCallback: FileCopyMoveCallback? = null
  private var selectedFileUri: Uri? = null
  private var selectedFile: DocumentFile? = null
  private var lifecycleScope: CoroutineScope? = null
  private var storageObservingJob: Job? = null
  var isMoveOperation = false
  var shouldValidateZimFile: Boolean = false
  private lateinit var fragmentManager: FragmentManager
  private var isSingleFileSelected = true
  private var unitTestStorage: File? = null
  private var unitTestDestinationFile: File? = null

  @VisibleForTesting
  fun setStorageFileForUnitTest(unitTestStorage: File, unitTestDestinationFile: File) {
    this.unitTestStorage = unitTestStorage
    this.unitTestDestinationFile = unitTestDestinationFile
  }

  private fun getCopyMoveTitle(): String =
    if (isMoveOperation) {
      activity.getString(R.string.moving_zim_file, requireSelectedFile().name)
    } else {
      activity.getString(R.string.copying_zim_file, requireSelectedFile().name)
    }

  fun setAlertDialogShower(alertDialogShower: AlertDialogShower) {
    copyMoveProgressBarController.setAlertDialogShower(alertDialogShower)
  }

  suspend fun showMoveFileToPublicDirectoryDialog(
    uri: Uri? = null,
    documentFile: DocumentFile? = null,
    shouldValidateZimFile: Boolean = false,
    fragmentManager: FragmentManager,
    multipleFilesProcessAction: MultipleFilesProcessAction? = null,
    isSingleFileSelected: Boolean
  ) {
    this.isSingleFileSelected = isSingleFileSelected
    this.shouldValidateZimFile = shouldValidateZimFile
    this.fragmentManager = fragmentManager
    setSelectedFileAndUri(uri, documentFile)
    if (getStorageDeviceList().isEmpty()) {
      copyMoveProgressBarController.showPreparingCopyMoveDialog()
    }
    if (kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove.first() && getStorageDeviceList().size > 1) {
      // Show dialog to select storage if more than one storage device is available, and user
      // have not configured the storage yet.
      copyMoveProgressBarController.hidePreparingCopyMoveDialog()
      showCopyMoveDialog(true)
    } else {
      if (getStorageDeviceList().size == 1) {
        // If only internal storage is currently available, set shouldShowStorageSelectionDialog
        // to true. This allows the storage configuration dialog to be shown again if the
        // user removes an external storage device (like an SD card) and then reinserts it.
        // This ensures they are prompted to configure storage settings upon SD card reinsertion.
        kiwixDataStore.setShowStorageSelectionDialogOnCopyMove(true)
      }
      copyMoveProgressBarController.hidePreparingCopyMoveDialog()
      if (validateZimFileCanCopyOrMove()) {
        when (multipleFilesProcessAction) {
          MultipleFilesProcessAction.Copy -> performCopyOperation()
          MultipleFilesProcessAction.Move -> performMoveOperation()
          null -> showCopyMoveDialog()
        }
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

  fun showStorageSelectDialog(storageDeviceList: List<StorageDevice>) =
    StorageSelectDialog()
      .apply {
        onSelectAction = ::copyMoveZIMFileInSelectedStorage
        titleSize = STORAGE_SELECT_STORAGE_TITLE_TEXTVIEW_SIZE
        setStorageDeviceList(storageDeviceList)
        setShouldShowCheckboxSelected(false)
      }
      .show(fragmentManager, activity.getString(R.string.choose_storage_to_copy_move_zim_file))

  suspend fun copyMoveZIMFileInSelectedStorage(storageDevice: StorageDevice) {
    kiwixDataStore.apply {
      setShowStorageSelectionDialogOnCopyMove(false)
      setSelectedStorage(kiwixDataStore.getPublicDirectoryPath(storageDevice.name))
      setSelectedStoragePosition(
        if (storageDevice.isInternal) {
          INTERNAL_SELECT_POSITION
        } else {
          EXTERNAL_SELECT_POSITION
        }
      )
    }
    if (validateZimFileCanCopyOrMove()) {
      performCopyMoveOperation()
    }
  }

  private suspend fun performCopyMoveOperation() {
    if (isMoveOperation) {
      performMoveOperation()
    } else {
      performCopyOperation()
    }
  }

  fun isBookLessThan4GB(): Boolean =
    requireSelectedFile().length() < FOUR_GIGABYTES_IN_KILOBYTES

  private fun hasNotSufficientStorageSpace(availableSpace: Long): Boolean =
    availableSpace < requireSelectedFile().length()

  suspend fun validateZimFileCanCopyOrMove(): Boolean {
    val storageFile = getSelectedStorageRoot()
    // hide the dialog if already showing
    copyMoveProgressBarController.hidePreparingCopyMoveDialog()
    val availableSpace = storageCalculator.availableBytes(storageFile)
    if (hasNotSufficientStorageSpace(availableSpace)) {
      fileCopyMoveCallback?.insufficientSpaceInStorage(availableSpace)
      return false
    }
    return when (fat32Checker.fileSystemStates.value) {
      DetectingFileSystem -> {
        handleDetectingFileSystemState(storageFile)
        false
      }

      CannotWrite4GbFile -> {
        handleCannotWrite4GbFileState(storageFile)
        false
      }

      else -> true
    }
  }

  suspend fun handleDetectingFileSystemState(storageFile: File) {
    if (isBookLessThan4GB()) {
      performCopyMoveOperationIfSufficientSpaceAvailable(storageFile)
    } else {
      copyMoveProgressBarController.showPreparingCopyMoveDialog()
      observeFileSystemState()
    }
  }

  suspend fun handleCannotWrite4GbFileState(storageFile: File) {
    if (isBookLessThan4GB()) {
      performCopyMoveOperationIfSufficientSpaceAvailable(storageFile)
    } else {
      // Show an error dialog indicating the file system limitation
      fileCopyMoveCallback?.filesystemDoesNotSupportedCopyMoveFilesOver4GB()
    }
  }

  fun observeFileSystemState() {
    if (storageObservingJob?.isActive == true) return
    storageObservingJob = lifecycleScope?.launch {
      fat32Checker.fileSystemStates.collect {
        copyMoveProgressBarController.hidePreparingCopyMoveDialog()
        if (validateZimFileCanCopyOrMove()) {
          performCopyMoveOperation()
        }
      }
    }
  }

  suspend fun performCopyMoveOperationIfSufficientSpaceAvailable(storageFile: File) {
    val availableSpace = storageCalculator.availableBytes(storageFile)
    if (hasNotSufficientStorageSpace(availableSpace)) {
      fileCopyMoveCallback?.insufficientSpaceInStorage(availableSpace)
    } else {
      performCopyMoveOperation()
    }
  }

  private fun showCopyMoveDialog(showStorageSelectionDialog: Boolean = false) {
    copyMoveProgressBarController.showCopyMoveDialog(
      getCopyMoveFilesToPublicDirectoryDialogMessage(),
      { onCopyClicked(showStorageSelectionDialog) },
      { onMoveClicked(showStorageSelectionDialog) }
    )
  }

  private fun onCopyClicked(showStorageSelectionDialog: Boolean) {
    lifecycleScope?.launch {
      performCopyOperation(showStorageSelectionDialog)
    }
  }

  private fun onMoveClicked(showStorageSelectionDialog: Boolean) {
    lifecycleScope?.launch {
      performMoveOperation(showStorageSelectionDialog)
    }
  }

  private fun getCopyMoveFilesToPublicDirectoryDialogMessage() = if (isSingleFileSelected) {
    activity.getString(R.string.copy_move_files_dialog_description)
  } else {
    activity.getString(R.string.copy_move_multiple_files_dialog_description)
  }

  suspend fun performCopyOperation(showStorageSelectionDialog: Boolean = false) {
    isMoveOperation = false
    fileCopyMoveCallback?.onMultipleFilesProcessSelection(MultipleFilesProcessAction.Copy)
    if (showStorageSelectionDialog) {
      showStorageSelectDialog(getStorageDeviceList())
    } else {
      copyZimFileToPublicAppDirectory()
    }
  }

  suspend fun performMoveOperation(showStorageSelectionDialog: Boolean = false) {
    isMoveOperation = true
    fileCopyMoveCallback?.onMultipleFilesProcessSelection(MultipleFilesProcessAction.Move)
    if (showStorageSelectionDialog) {
      showStorageSelectDialog(getStorageDeviceList())
    } else {
      moveZimFileToPublicAppDirectory()
    }
  }

  private suspend fun copyZimFileToPublicAppDirectory() {
    val destinationFile = getDestinationFile()
    try {
      copyMoveProgressBarController.showProgress(getCopyMoveTitle())
      fileOperationHandler.copy(
        requireSelectedFileUri(),
        destinationFile,
        copyMoveProgressBarController::updateProgress
      )
      withContext(Dispatchers.Main) {
        notifyFileOperationSuccess(destinationFile, requireSelectedFileUri())
      }
    } catch (ignore: Exception) {
      ignore.printStackTrace()
      handleFileOperationError(
        activity.getString(R.string.copy_file_error_message, ignore.message),
        destinationFile
      )
    }
  }

  private suspend fun moveZimFileToPublicAppDirectory() {
    val destinationFile = getDestinationFile()
    try {
      copyMoveProgressBarController.showProgress(getCopyMoveTitle())
      val moveSuccess = fileOperationHandler.move(
        selectedFile = requireSelectedFile(),
        sourceUri = requireSelectedFileUri(),
        destinationFolderUri = DocumentFile.fromFile(getSelectedStorageRoot()).uri,
        destinationFile = destinationFile,
        copyMoveProgressBarController::updateProgress
      )
      withContext(Dispatchers.Main) {
        if (moveSuccess) {
          notifyFileOperationSuccess(destinationFile, requireSelectedFileUri())
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

  suspend fun handleFileOperationError(
    errorMessage: String?,
    destinationFile: File
  ) {
    copyMoveProgressBarController.dismissCopyMoveProgressDialog()
    fileCopyMoveCallback?.onError("$errorMessage").also {
      // Clean up the destination file if an error occurs
      destinationFile.deleteFile()
    }
  }

  suspend fun notifyFileOperationSuccess(destinationFile: File, sourceUri: Uri) {
    if (shouldValidateZimFile && !isValidZimFile(destinationFile)) {
      handleInvalidZimFile(destinationFile, sourceUri)
      return
    }
    copyMoveProgressBarController.dismissCopyMoveProgressDialog()
    if (isMoveOperation) {
      fileOperationHandler.delete(sourceUri, requireSelectedFile())
      fileCopyMoveCallback?.onFileMoved(destinationFile)
    } else {
      fileCopyMoveCallback?.onFileCopied(destinationFile)
    }
  }

  suspend fun isValidZimFile(destinationFile: File): Boolean =
    FileUtils.isSplittedZimFile(destinationFile.name) || validateZimFileValid(destinationFile)

  suspend fun handleInvalidZimFile(destinationFile: File, sourceUri: Uri) {
    val errorMessage = activity.getString(R.string.error_file_invalid, destinationFile.path)
    if (isMoveOperation) {
      val moveSuccessful = fileOperationHandler.rollbackMove(destinationFile, sourceUri)

      if (moveSuccessful) {
        // If files is moved back using the documentContract then show the error message to user
        copyMoveProgressBarController.dismissCopyMoveProgressDialog()
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

  private suspend fun validateZimFileValid(destinationFile: File): Boolean {
    var archive: Archive? = null
    return try {
      // create archive object, and check if it has the mainEntry or not to validate the ZIM file.
      archive = ZimReaderSource(destinationFile).createArchive()
      archive?.hasMainEntry() == true
    } catch (_: Exception) {
      // if it is a invalid ZIM file
      false
    } finally {
      archive?.dispose()
    }
  }

  suspend fun getDestinationFile(): File {
    // We could not perform the file operations in unit test so we are passing the mockk file from
    // unit test cases.
    unitTestDestinationFile?.let { return it }
    val root = getSelectedStorageRoot()
    val fileName = requireSelectedFile().name.orEmpty()

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

  suspend fun getStorageDeviceList() =
    (activity as? KiwixMainActivity)?.getStorageDeviceList().orEmpty()

  private suspend fun getSelectedStorageRoot(): File =
    unitTestStorage ?: File(kiwixDataStore.selectedStorage.first())

  private fun requireSelectedFileUri(): Uri =
    selectedFileUri ?: throw FileNotFoundException("Selected file uri not found")

  private fun requireSelectedFile(): DocumentFile =
    selectedFile ?: throw FileNotFoundException("Selected file not found")

  fun dispose() {
    storageObservingJob?.cancel()
    storageObservingJob = null
    setFileCopyMoveCallback(null)
    setLifeCycleScope(null)
  }

  interface FileCopyMoveCallback {
    fun onFileCopied(file: File)
    fun onFileMoved(file: File)
    fun insufficientSpaceInStorage(availableSpace: Long)
    fun filesystemDoesNotSupportedCopyMoveFilesOver4GB()
    fun onError(errorMessage: String)
    fun onMultipleFilesProcessSelection(multipleFilesProcessAction: MultipleFilesProcessAction)
  }
}
