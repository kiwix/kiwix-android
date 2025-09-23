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
import android.util.Log
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import java.io.File
import javax.inject.Inject

/**
 * Handles the process of validating and opening selected ZIM files
 * for the Standalone (non-Play Store) variant of the app.
 */
class ProcessSelectedZimFilesForStandalone @Inject constructor(
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val activity: Activity
) {
  private var selectedZimFileCallback: SelectedZimFileCallback? = null

  fun setSelectedZimFileCallback(selectedZimFileCallback: SelectedZimFileCallback) {
    this.selectedZimFileCallback = selectedZimFileCallback
  }

  /**
   * Returns whether this handler can process URIs.
   * Standalone builds (non-Play Store) handle them directly.
   */
  fun canHandleUris(): Boolean = !sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove()

  /**
   * Entry point for processing selected files.
   *
   * - If a single file is selected, validate and open it.
   * - If multiple files are selected, validate and add them to the library one by one.
   */
  suspend fun processSelectedFiles(uris: List<Uri>, isAfterRetry: Boolean = false) {
    val singleFile = uris.singleOrNull()
    if (singleFile != null && !isAfterRetry) {
      processSingleFile(uris.first())
    } else {
      processMultipleFiles(uris)
    }
  }

  /**
   * Processes a single ZIM file:
   * - If valid → navigate to reader.
   * - If invalid → show toast with error message.
   */
  private suspend fun processSingleFile(uri: Uri) {
    val (file, errorMessage) = getZimFileFromUri(uri)
    if (file == null) {
      activity.toast(errorMessage)
    } else {
      selectedZimFileCallback?.navigateToReaderFragment(file)
    }
  }

  /**
   * Processes multiple ZIM files sequentially.
   * - Valid files are added to the library.
   * - Invalid files show error dialog and allow retrying with remaining files.
   */
  private suspend fun processMultipleFiles(uris: List<Uri>) {
    uris.forEachIndexed { index, uri ->
      val (file, errorMessage) = getZimFileFromUri(uri)

      if (file == null) {
        selectedZimFileCallback?.showFileCopyMoveErrorDialog(errorMessage) {
          // Continue processing from the next file
          processSelectedFiles(uris.drop(index + ONE), true)
        }
        return
      }

      selectedZimFileCallback?.addBookToLibkiwixBookOnDisk(file)
      // Notify user after all files are processed
      if (index == uris.lastIndex) {
        activity.toast(activity.getString(string.your_selected_files_added_to_library))
      }
    }
  }

  /**
   * Resolves a [File] from a given [Uri] and validates it as a ZIM file.
   *
   * @return [Pair]:
   *   - First → valid [File] if found, otherwise `null`.
   *   - Second → error message if invalid/not found, otherwise empty string.
   */
  private suspend fun getZimFileFromUri(
    uri: Uri
  ): Pair<File?, String> {
    val filePath =
      FileUtils.getLocalFilePathByUri(
        activity.applicationContext,
        uri
      )
    if (filePath == null || !File(filePath).isFileExist()) {
      Log.e(
        TAG_KIWIX,
        "ZIM file not found in storage. File Uri = $uri\nRetrieved Path = $filePath"
      )
      return null to activity.getString(string.error_file_not_found, "$uri")
    }
    val file = File(filePath)
    return if (!FileUtils.isValidZimFile(file.path)) {
      Log.e(TAG_KIWIX, "Invalid ZIM file. Path = ${file.path}")
      null to activity.getString(string.error_file_invalid, file.path)
    } else {
      file to ""
    }
  }
}

interface SelectedZimFileCallback {
  fun navigateToReaderFragment(file: File)
  fun addBookToLibkiwixBookOnDisk(file: File)
  fun showFileCopyMoveErrorDialog(errorMessage: String, callBack: suspend () -> Unit)
}
