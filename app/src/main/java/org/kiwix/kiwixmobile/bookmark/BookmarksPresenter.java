/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
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
import org.kiwix.kiwixmobile.data.local.entity.Bookmark;
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
