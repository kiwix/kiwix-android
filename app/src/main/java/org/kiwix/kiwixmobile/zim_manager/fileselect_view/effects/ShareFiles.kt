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

package org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk

data class ShareFiles(private val selectedBooks: List<BookOnDisk>) :
  SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    val selectedFileShareIntent = Intent()
    selectedFileShareIntent.action = Intent.ACTION_SEND_MULTIPLE
    selectedFileShareIntent.type = "application/octet-stream"
    val selectedFileContentURIs = selectedBooks.mapNotNull {
      if (Build.VERSION.SDK_INT >= 24) {
        FileProvider.getUriForFile(
          activity,
          activity.packageName + ".fileprovider",
          it.file
        )
      } else {
        Uri.fromFile(it.file)
      }
    }
    selectedFileShareIntent.putParcelableArrayListExtra(
      Intent.EXTRA_STREAM,
      ArrayList(selectedFileContentURIs)
    )
    selectedFileShareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    val shareChooserIntent = Intent.createChooser(
      selectedFileShareIntent,
      activity.getString(R.string.selected_file_cab_app_chooser_title)
    )
    if (shareChooserIntent.resolveActivity(activity.packageManager) != null) {
      activity.startActivity(shareChooserIntent) // Open the app chooser dialog
    }
  }
}
