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
package org.kiwix.kiwixmobile.zim_manager.library;

import android.util.Log;

import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.data.local.dao.BookDao;
import org.kiwix.kiwixmobile.data.remote.KiwixService;
import org.kiwix.kiwixmobile.models.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.zim_manager.download.DownloadFragment;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Created by EladKeyshawn on 06/04/2017.
 */

public class LibraryPresenter extends BasePresenter<LibraryViewCallback> {

  @Inject
  KiwixService kiwixService;

  @Inject
  BookDao bookDao;

  @Inject
  LibraryPresenter() {
  }

  void loadBooks() {
    view.displayScanningContent();
    compositeDisposable.add(kiwixService.getLibrary()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(library -> view.showBooks(library.getBooks()), error -> {
          String msg = error.getLocalizedMessage();
          Log.w("kiwixLibrary", "Error loading books:" + (msg != null ? msg : "(null)"));
          view.displayNoItemsFound();
        }));
  }

  void loadRunningDownloadsFromDb() {
    for (LibraryNetworkEntity.Book book : bookDao.getDownloadingBooks()) {
      if (!DownloadFragment.downloads.containsValue(book)) {
        book.url = book.remoteUrl;
        view.downloadFile(book);
      }
    }
  }
}
