/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.zim_manager.library_view

import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import org.kiwix.kiwixmobile.base.BasePresenter
import org.kiwix.kiwixmobile.data.local.dao.BookDao
import org.kiwix.kiwixmobile.data.remote.KiwixService
import org.kiwix.kiwixmobile.downloader.DownloadFragment
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity
import javax.inject.Inject

/**
 * Created by EladKeyshawn on 06/04/2017.
 */

class LibraryPresenter @Inject constructor(
  var kiwixService: KiwixService,
  var bookDao: BookDao
) : BasePresenter<LibraryViewCallback>() {

  fun loadBooks() {
    view.displayScanningContent()
    compositeDisposable.add(
        kiwixService.library
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { library -> view.showBooks(library.books) },
                { error ->
                  Log.w("kiwixLibrary", "Error loading books:" + (error.localizedMessage
                      ?: "(null)"))
                  view.displayNoItemsFound()
                }
            )
    )
  }

  fun loadRunningDownloadsFromDb() {
    bookDao.downloadingBooks
        .filter(this::isNotInDownloads)
        .forEach(this::download)
  }

  private fun download(book: LibraryNetworkEntity.Book) {
    book.url = book.remoteUrl
    view.downloadFile(book)
  }

  private fun isNotInDownloads(book: LibraryNetworkEntity.Book?) =
      !DownloadFragment.downloads.containsValue(book)
}
