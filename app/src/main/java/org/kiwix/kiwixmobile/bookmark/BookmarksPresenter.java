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

package org.kiwix.kiwixmobile.bookmark;

import android.util.Log;
import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.data.DataSource;
import org.kiwix.kiwixmobile.di.PerActivity;
import org.kiwix.kiwixmobile.di.qualifiers.Computation;
import org.kiwix.kiwixmobile.di.qualifiers.MainThread;

@PerActivity
class BookmarksPresenter extends BasePresenter<BookmarksContract.View>
  implements BookmarksContract.Presenter {

  private final DataSource dataSource;
  private final Scheduler mainThread;
  private final Scheduler computation;
  private Disposable disposable;

  @Inject BookmarksPresenter(DataSource dataSource, @MainThread Scheduler mainThread,
    @Computation Scheduler computation) {
    this.dataSource = dataSource;
    this.mainThread = mainThread;
    this.computation = computation;
  }

  @Override
  public void loadBookmarks(boolean showFromCurrentBookmarks) {
    dataSource.getBookmarks(showFromCurrentBookmarks)
      .subscribe(new SingleObserver<List<BookmarkItem>>() {
        @Override
        public void onSubscribe(Disposable d) {
          if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
          }
          disposable = d;
          compositeDisposable.add(d);
        }

        @Override
        public void onSuccess(List<BookmarkItem> bookmarks) {
          view.updateBookmarksList(bookmarks);
        }

        @Override
        public void onError(Throwable e) {
          Log.e("BookmarksPresenter", e.toString());
        }
      });
  }

  @Override
  public void filterBookmarks(List<BookmarkItem> bookmarks, String newText) {
    Observable.fromIterable(bookmarks)
      .filter(
        bookmark -> bookmark.getBookmarkTitle().toLowerCase().contains(newText.toLowerCase()))
      .toList()
      .subscribeOn(computation)
      .observeOn(mainThread)
      .subscribe(new SingleObserver<List<BookmarkItem>>() {
        @Override
        public void onSubscribe(Disposable d) {
          compositeDisposable.add(d);
        }

        @Override
        public void onSuccess(List<BookmarkItem> bookmarkList) {
          view.notifyBookmarksListFiltered(bookmarkList);
        }

        @Override
        public void onError(Throwable e) {
          Log.e("BookmarksPresenter", e.toString());
        }
      });
  }

  @Override
  public void deleteBookmarks(List<BookmarkItem> deleteList) {
    dataSource.deleteBookmarks(deleteList)
      .subscribe(new CompletableObserver() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onComplete() {

        }

        @Override
        public void onError(Throwable e) {
          Log.e("BookmarksPresenter", e.toString());
        }
      });
  }
}
