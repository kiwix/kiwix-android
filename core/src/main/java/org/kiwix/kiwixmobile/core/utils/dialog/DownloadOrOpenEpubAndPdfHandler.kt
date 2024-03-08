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

package org.kiwix.kiwixmobile.core.utils.dialog

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.downloadFileFromUrl
import java.io.File
import javax.inject.Inject

class DownloadOrOpenEpubAndPdfHandler @Inject constructor(
  private val activity: Activity,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val alertDialogShower: AlertDialogShower,
  private val zimReaderContainer: ZimReaderContainer
) {

  fun showDownloadOrOpenEpubAndPdfDialog(url: String, documentType: String?) {
    alertDialogShower.show(
      KiwixDialog.DownloadOrOpenEpubAndPdf,
      { openOrDownloadFile(url, documentType, true) },
      { openOrDownloadFile(url, documentType, false) },
      { }
    )
  }

  private fun openOrDownloadFile(url: String, documentType: String?, openFile: Boolean) {
    downloadFileFromUrl(
      url,
      null,
      zimReaderContainer,
      sharedPreferenceUtil
    )?.let { savedFile ->
      if (openFile) {
        openFile(savedFile, documentType)
      } else {
        activity.toast(activity.getString(R.string.save_media_saved, savedFile.name)).also {
          Log.e("DownloadOrOpenEpubAndPdf", "File downloaded at = ${savedFile.path}")
        }
      }
    } ?: run {
      activity.toast(R.string.save_media_error)
    }
  }

  private fun openFile(savedFile: File, documentType: String?) {
    if (savedFile.exists()) {
      val uri = FileProvider.getUriForFile(
        activity,
        "${activity.packageName}.fileprovider",
        savedFile
      )
      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, documentType)
        flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      if (intent.resolveActivity(activity.packageManager) != null) {
        activity.startActivity(intent)
      } else {
        activity.toast(R.string.no_reader_application_installed)
      }
    }
  }
}
