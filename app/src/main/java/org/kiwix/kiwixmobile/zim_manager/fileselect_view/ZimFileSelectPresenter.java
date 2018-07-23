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
package org.kiwix.kiwixmobile.zim_manager.fileselect_view;

import android.util.Log;

import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.data.DataSource;
import org.kiwix.kiwixmobile.data.local.dao.BookDao;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;

import java.util.ArrayList;

import javax.inject.Inject;

import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;

/**
 * Created by EladKeyshawn on 06/04/2017.
 */
public class ZimFileSelectPresenter extends BasePresenter<ZimFileSelectViewCallback> {

  private static final String TAG = "ZimFileSelectPresenter";
  private final DataSource dataSource;

  @Inject
  BookDao bookDao;

  @Inject
  ZimFileSelectPresenter(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void attachView(ZimFileSelectViewCallback mvpView) {
    super.attachView(mvpView);
  }

  public void loadLocalZimFileFromDb() {
    ArrayList<LibraryNetworkEntity.Book> books = bookDao.getBooks();
    view.showFiles(books);
  }

  void saveBooks(ArrayList<LibraryNetworkEntity.Book> books) {
    dataSource.saveBooks(books)
        .subscribe(new CompletableObserver() {
          @Override
          public void onSubscribe(Disposable d) {

          }

          @Override
          public void onComplete() {

          }

          @Override
          public void onError(Throwable e) {
            Log.e(TAG, "Unable to save books", e);
          }
        });
  }

  public void deleteBook(LibraryNetworkEntity.Book book) {
    dataSource.deleteBook(book)
        .subscribe(new CompletableObserver() {
          @Override
          public void onSubscribe(Disposable d) {

          }

          @Override
          public void onComplete() {

          }

          @Override
          public void onError(Throwable e) {
            Log.e(TAG, "Unable to delete book", e);
          }
        });
  }
}
