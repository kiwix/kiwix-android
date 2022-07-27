/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.zimManager.libraryView

import eu.mhutti1.utils.storage.Bytes
import eu.mhutti1.utils.storage.Kb
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.zimManager.libraryView.adapter.LibraryListItem
import javax.inject.Inject

class AvailableSpaceCalculator @Inject constructor(
  private val downloadDao: FetchDownloadDao,
  private val storageCalculator: StorageCalculator
) {
  fun hasAvailableSpaceFor(
    bookItem: LibraryListItem.BookItem,
    successAction: (LibraryListItem.BookItem) -> Unit,
    failureAction: (String) -> Unit
  ) {
    downloadDao.allDownloads()
      .map { it.map(DownloadModel::bytesRemaining).sum() }
      .map { bytesToBeDownloaded -> storageCalculator.availableBytes() - bytesToBeDownloaded }
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { trueAvailableBytes ->
        if (bookItem.book.size.toLong() * Kb < trueAvailableBytes) {
          successAction.invoke(bookItem)
        } else {
          failureAction.invoke(Bytes(trueAvailableBytes).humanReadable)
        }
      }
  }
}
