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
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleCoroutineScope
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
import java.io.File
import javax.inject.Inject

class CopyMoveFileHandler @Inject constructor(
  private val activity: Activity,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val alertDialogShower: AlertDialogShower,
  private val storageCalculator: StorageCalculator
) {
  var fileCopyMoveCallback: FileCopyMoveCallback? = null
  private var selectedFileUri: Uri? = null
  private var selectedFile: File? = null
  private var progressBarDialog: AlertDialog? = null
  var lifecycleScope: LifecycleCoroutineScope? = null
  private var progressBar: ProgressBar? = null
  private var progressBarTextView: TextView? = null
  private var isCopySelected = false

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
          showCopyMoveDialog()
        }
      )
    } else {
      showCopyMoveDialog()
    }
  }

  private fun showCopyMoveDialog() {
    val availableSpace = storageCalculator.availableBytes()
    val fileSize = selectedFile?.length() ?: 0L

    if (availableSpace > fileSize) {
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
    } else {
      fileCopyMoveCallback?.insufficientSpaceInStorage(availableSpace)
    }
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
        ignore.printStackTrace()
        fileCopyMoveCallback?.onError("Unable to copy zim file ${ignore.message}")
          .also {
            // delete the temporary file if any error happens
            destinationFile.deleteFile()
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

  private fun moveZimFileToPublicAppDirectory() {
    lifecycleScope?.launch {
      val destinationFile = getDestinationFile()
      try {
        selectedFileUri?.let { uri ->
          showProgressDialog()
          val destinationUri = Uri.fromFile(destinationFile)
          copyFile(uri, destinationUri)
          withContext(Dispatchers.Main) {
            dismissProgressDialog()
            fileCopyMoveCallback?.onFileMoved(destinationFile)
          }.also {
            DocumentsContract.deleteDocument(activity.applicationContext.contentResolver, uri)
          }
        }
      } catch (ignore: Exception) {
        dismissProgressDialog()
        ignore.printStackTrace()
        fileCopyMoveCallback?.onError("Unable to copy zim file ${ignore.message}")
          .also {
            // delete the temporary file if any error happens
            destinationFile.deleteFile()
          }
      }
    }
  }

  private fun getDestinationFile(): File =
    File("${sharedPreferenceUtil.prefStorage}/${selectedFile?.name}").also {
      if (!it.isFileExist()) it.createNewFile()
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
    fun onError(errorMessage: String)
  }
}
