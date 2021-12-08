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

package org.kiwix.kiwixmobile.zimManager.fileselect_view.effects

import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.navigate
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.localFileTransfer.URIS_KEY

data class ShareFiles(private val selectedBooks: List<BookOnDisk>) :
  SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
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
    activity.navigate(
      R.id.localFileTransferFragment,
      bundleOf(URIS_KEY to selectedFileContentURIs.toTypedArray())
    )
  }
}
