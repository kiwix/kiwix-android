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

package org.kiwix.kiwixmobile.nav.destination.library.local

import android.app.Activity
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentManager
import eu.mhutti1.utils.storage.Bytes
import eu.mhutti1.utils.storage.StorageDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.extensions.runSafelyInLifecycleScope
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.isSplittedZimFile
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.nav.destination.library.CopyMoveFileHandler
import org.kiwix.kiwixmobile.storage.StorageSelectDialog
import java.io.File
import javax.inject.Inject

/**
 * Handles the process of validating, copying/moving, and opening
 * selected ZIM files for the Play Store variant of the app.
 *
 * This class ensures:
 * - Enough storage is available before proceeding.
 * - File validity is checked before copying/moving.
 * - User-friendly error handling (snackbar, dialogs, toasts).
 * - Sequential handling of multiple ZIM files.
 */
class ProcessSelectedZimFilesForPlayStore @Inject constructor(
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val kiwixDataStore: KiwixDataStore,
  private val activity: Activity,
  private val copyMoveFileHandler: CopyMoveFileHandler,
  private val storageCalculator: StorageCalculator
) : CopyMoveFileHandler.FileCopyMoveCallback {
  private var snackBarHostState: SnackbarHostState? = null
  private var selectedZimFileCallback: SelectedZimFileCallback? = null
  private lateinit var fragmentManager: FragmentManager
  private var lifecycleScope: CoroutineScope? = null
  private var alertDialogShower: AlertDialogShower? = null
  private var isSingleFileSelected = false

  /**
   * Manages the selected action by user when processing the multiple files.
   */
  private var multipleFilesProcessAction: MultipleFilesProcessAction? = null
  private val selectedZimFileUriList: MutableList<Uri> = mutableListOf()

  /**
   * Initializes the handler with required dependencies and callbacks.
   * Must be called before using this class.
   */
  fun init(
    lifecycleScope: CoroutineScope,
    alertDialogShower: AlertDialogShower,
    snackBarHostState: SnackbarHostState,
    fragmentManager: FragmentManager,
    selectedZimFileCallback: SelectedZimFileCallback
  ) {
    this.lifecycleScope = lifecycleScope
    this.fragmentManager = fragmentManager
    this.selectedZimFileCallback = selectedZimFileCallback
    this.alertDialogShower = alertDialogShower
    copyMoveFileHandler.apply {
      setFileCopyMoveCallback(this@ProcessSelectedZimFilesForPlayStore)
      setLifeCycleScope(lifecycleScope)
      setAlertDialogShower(alertDialogShower)
    }
    this.snackBarHostState = snackBarHostState
  }

  /**
   * Returns whether this handler can process URIs in Play Store builds
   * (restricted to Android 11+ due to storage changes).
   */
  fun canHandleUris(): Boolean = sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove()

  /** Stores the selected ZIM file URIs internally for processing. */
  private fun storeSelectedFiles(uris: List<Uri>) {
    selectedZimFileUriList.clear()
    selectedZimFileUriList.addAll(uris)
  }

  /**
   * Validates available storage, and processes one or multiple files.
   * If space is insufficient, shows storage selection dialog.
   */
  suspend fun processSelectedFiles(uris: List<Uri>, isAfterRetry: Boolean = false) {
    storeSelectedFiles(uris)
    val totalSelectedFileSize = getTotalSizeOfSelectedZIMFiles(uris)
    val availableSpaceInStorage =
      storageCalculator.availableBytes(File(kiwixDataStore.selectedStorage.first()))
    if (availableSpaceInStorage < totalSelectedFileSize) {
      // Not enough storage → show storage selection dialog/snackbar
      insufficientSpaceInStorage(availableSpaceInStorage)
      return
    }

    if (uris.size == 1 && !isAfterRetry) {
      isSingleFileSelected = true
      processSingleFile(uris.first())
    } else {
      isSingleFileSelected = false
      processMultipleFiles(uris)
    }
  }

  /**
   * Processes a single file: validates it and asks user to move/copy to public directory.
   * If invalid:
   * - From multiple selection → show error dialog but continue with other files.
   * - From single selection → show toast and stop.
   */
  private suspend fun processSingleFile(
    uri: Uri,
    isFromMultipleFiles: Boolean = false
  ) {
    val documentFile = when (uri.scheme) {
      "file" -> DocumentFile.fromFile(File("$uri"))
      else -> {
        DocumentFile.fromSingleUri(activity, uri)
      }
    }

    val fileName = documentFile?.name
    if (!isValidZimFile(fileName)) {
      handleInvalidFile(uri, fileName, isFromMultipleFiles)
      return
    }

    copyMoveFileHandler.showMoveFileToPublicDirectoryDialog(
      uri,
      documentFile,
      // pass if fileName is null then we will validate it after copying/moving
      fileName == null,
      fragmentManager,
      multipleFilesProcessAction,
      isSingleFileSelected
    )
  }

  /**
   * Handles invalid ZIM file scenarios based on selection type.
   */
  private fun handleInvalidFile(
    uri: Uri,
    fileName: String?,
    isFromMultipleFiles: Boolean
  ) {
    val errorMessage =
      activity.getString(string.error_file_invalid, fileName ?: uri.toString())
    if (isFromMultipleFiles) {
      selectedZimFileCallback?.showFileCopyMoveErrorDialog(errorMessage) {
        // Continue with next file
        processSelectedFiles(selectedZimFileUriList.drop(ONE), isAfterRetry = true)
      }
    } else {
      activity.toast(errorMessage)
    }
  }

  /**
   * Processes multiple files sequentially by delegating each file
   * to [processSingleFile]. When one finishes, the next is processed.
   */
  private suspend fun processMultipleFiles(uris: List<Uri>) {
    // We process ZIM file copy/move operations one by one.
    // Since copying/moving runs on an IO thread, it is not synchronized.
    // Therefore, we observe the callbacks and continue with the next operation accordingly.
    if (uris.isEmpty()) {
      // All files processed successfully.
      multipleFilesProcessAction = null
      activity.toast(activity.getString(string.your_selected_files_added_to_library))
      return
    }
    val uri = uris.first()
    processSingleFile(uri, true)
  }

  /** Returns total size of all selected ZIM files. */
  private fun getTotalSizeOfSelectedZIMFiles(urisList: List<Uri>): Long {
    var totalFilesSize = 0L
    urisList.forEach { uri ->
      val documentFile =
        when (uri.scheme) {
          "file" -> DocumentFile.fromFile(File("$uri"))
          else -> {
            DocumentFile.fromSingleUri(sharedPreferenceUtil.context, uri)
          }
        }
      totalFilesSize = totalFilesSize.plus(documentFile?.length() ?: ZERO.toLong())
    }
    return totalFilesSize
  }

  /** Validates whether the given file is a valid ZIM or a split ZIM file. */
  private fun isValidZimFile(fileName: String?): Boolean =
    fileName?.let {
      FileUtils.isValidZimFile(it) || isSplittedZimFile(it)
    } ?: false

  /** Shows a snackbar suggesting the user to change storage. */
  @Suppress("UnsafeCallOnNullableType")
  private fun showStorageSelectionSnackBar(message: String) {
    snackBarHostState?.snack(
      message = message,
      actionLabel = activity.getString(string.change_storage),
      lifecycleScope = lifecycleScope!!,
      actionClick = {
        lifecycleScope?.launch {
          showStorageSelectDialog((activity as KiwixMainActivity).getStorageDeviceList())
        }
      }
    )
  }

  /** Shows storage selection dialog to choose another storage device. */
  private fun showStorageSelectDialog(storageDeviceList: List<StorageDevice>) =
    StorageSelectDialog()
      .apply {
        onSelectAction = ::storeDeviceInPreferences
        setStorageDeviceList(storageDeviceList)
        setShouldShowCheckboxSelected(true)
      }
      .show(fragmentManager, activity.getString(string.pref_storage))

  /**
   * Stores the newly selected storage path in preferences
   * and retries copying/moving the ZIM file.
   */
  private fun storeDeviceInPreferences(
    storageDevice: StorageDevice
  ) {
    lifecycleScope.runSafelyInLifecycleScope {
      kiwixDataStore.apply {
        setSelectedStorage(kiwixDataStore.getPublicDirectoryPath(storageDevice.name))
        setSelectedStoragePosition(
          if (storageDevice.isInternal) {
            INTERNAL_SELECT_POSITION
          } else {
            EXTERNAL_SELECT_POSITION
          }
        )
      }
      // after selecting the storage try to copy/move the zim file.
      copyMoveFileHandler.copyMoveZIMFileInSelectedStorage(storageDevice)
    }
  }

  fun dispose() {
    copyMoveFileHandler.dispose()
    lifecycleScope = null
    selectedZimFileCallback = null
    multipleFilesProcessAction = null
  }

  override fun onFileCopied(file: File) {
    validateAndOpenZimInReader(file)
  }

  override fun onFileMoved(file: File) {
    validateAndOpenZimInReader(file)
  }

  override fun insufficientSpaceInStorage(availableSpace: Long) {
    val message =
      """
      ${activity.getString(string.move_no_space)}
      ${activity.getString(string.space_available)} ${Bytes(availableSpace).humanReadable}
      """.trimIndent()

    showStorageSelectionSnackBar(message)
  }

  override fun filesystemDoesNotSupportedCopyMoveFilesOver4GB() {
    showStorageSelectionSnackBar(activity.getString(R.string.file_system_does_not_support_4gb))
  }

  override fun onError(errorMessage: String) {
    if (isSingleFileSelected) {
      multipleFilesProcessAction = null
      activity.toast(errorMessage)
    } else {
      selectedZimFileCallback?.showFileCopyMoveErrorDialog(errorMessage) {
        // Continue with remaining files after error
        processSelectedFiles(selectedZimFileUriList.drop(ONE), true)
      }
    }
  }

  override fun onMultipleFilesProcessSelection(multipleFilesProcessAction: MultipleFilesProcessAction) {
    this.multipleFilesProcessAction = multipleFilesProcessAction
  }

  /**
   * Validates if copied/moved file is usable and navigates to reader.
   * If multiple files are selected, continues with next file in queue.
   */
  private fun validateAndOpenZimInReader(file: File) {
    when {
      isSingleFileSelected -> {
        // For single file it works like before.
        if (isSplittedZimFile(file.path)) {
          showWarningDialogForSplittedZimFile()
        } else {
          selectedZimFileCallback?.navigateToReaderFragment(file = file)
        }
      }

      else -> lifecycleScope?.launch {
        selectedZimFileCallback?.addBookToLibkiwixBookOnDisk(file)
        processSelectedFiles(selectedZimFileUriList.drop(ONE), true)
      }
    }
  }

  private fun showWarningDialogForSplittedZimFile() {
    alertDialogShower?.show(KiwixDialog.ShowWarningAboutSplittedZimFile)
  }
}

sealed class MultipleFilesProcessAction {
  object Copy : MultipleFilesProcessAction()
  object Move : MultipleFilesProcessAction()
}
