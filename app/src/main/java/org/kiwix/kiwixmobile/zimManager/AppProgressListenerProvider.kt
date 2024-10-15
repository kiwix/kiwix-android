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

package org.kiwix.kiwixmobile.zimManager

import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.data.remote.OnlineLibraryProgressListener
import org.kiwix.kiwixmobile.core.downloader.downloadManager.DEFAULT_INT_VALUE
import org.kiwix.kiwixmobile.core.downloader.downloadManager.HUNDERED
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO

class AppProgressListenerProvider(
  private val zimManageViewModel: ZimManageViewModel
) : OnlineLibraryProgressListener {
  @Suppress("MagicNumber")
  override fun onProgress(bytesRead: Long, contentLength: Long) {
    val progress =
      if (contentLength == DEFAULT_INT_VALUE.toLong()) {
        ZERO
      } else {
        (bytesRead * 3 * HUNDERED / contentLength).coerceAtMost(HUNDERED.toLong())
      }
    zimManageViewModel.downloadProgress.postValue(
      zimManageViewModel.context.getString(
        R.string.downloading_library,
        zimManageViewModel.context.getString(R.string.percentage, progress.toInt())
      )
    )
  }
}
