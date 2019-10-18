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

package org.kiwix.kiwixmobile.core.main;

import android.util.Log;
import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.core.base.BasePresenter;
import org.kiwix.kiwixmobile.core.bookmark.BookmarkItem;
import org.kiwix.kiwixmobile.core.data.DataSource;
import org.kiwix.kiwixmobile.core.di.ActivityScope;
import org.kiwix.kiwixmobile.core.history.HistoryListItem;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

/**
 * Presenter for {@link MainActivity}.
 */

@ActivityScope
public class MainPresenter extends BasePresenter<MainContract.View>
  implements MainContract.Presenter {

  private static final String TAG = "MainPresenter";
  private final DataSource dataSource;

  @Inject public MainPresenter(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void loadBooks() {
    dataSource.getLanguageCategorizedBooks()
      .subscribe(new SingleObserver<List<BooksOnDiskListItem>>() {
        @Override
        public void onSubscribe(Disposable d) {
          compositeDisposable.add(d);
        }

        @Override
        public void onSuccess(List<BooksOnDiskListItem> books) {
          view.addBooks(books);
        }

        @Override
        public void onError(Throwable e) {
          Log.e(TAG, "Unable to load books", e);
        }
      });
  }

  @Override
  public void saveBooks(List<BooksOnDiskListItem.BookOnDisk> book) {
    dataSource.saveBooks(book)
      .subscribe(new CompletableObserver() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onComplete() {
          loadBooks();
        }

        @Override
        public void onError(Throwable e) {
          Log.e(TAG, "Unable to save books", e);
        }
      });
  }

  @Override
  public void saveHistory(HistoryListItem.HistoryItem history) {
    dataSource.saveHistory(history)
      .subscribe(new CompletableObserver() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onComplete() {

        }

        @Override
        public void onError(Throwable e) {
          Log.e(TAG, "Unable to save history", e);
        }
      });
  }

  @Override
  public void loadCurrentZimBookmarksUrl() {
    compositeDisposable.add(dataSource.getCurrentZimBookmarksUrl()
      .subscribe(view::refreshBookmarksUrl,
        e -> Log.e(TAG, "Unable to load current ZIM urls", e)));
  }

  @Override
  public void saveBookmark(BookmarkItem bookmark) {
    dataSource.saveBookmark(bookmark)
      .subscribe(new CompletableObserver() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onComplete() {

        }

        @Override
        public void onError(Throwable e) {
          Log.e(TAG, "Unable to save bookmark", e);
        }
      });
  }

  @Override
  public void deleteBookmark(String bookmarkUrl) {
    dataSource.deleteBookmark(bookmarkUrl)
      .subscribe(new CompletableObserver() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onComplete() {

        }

        @Override
        public void onError(Throwable e) {
          Log.e(TAG, "Unable to delete bookmark", e);
        }
      });
  }
}
