package org.kiwix.kiwixmobile.main;

import android.util.Log;
import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.bookmark.BookmarkItem;
import org.kiwix.kiwixmobile.data.DataSource;
import org.kiwix.kiwixmobile.di.PerActivity;
import org.kiwix.kiwixmobile.history.HistoryListItem;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

/**
 * Presenter for {@link MainActivity}.
 */

@PerActivity
class MainPresenter extends BasePresenter<MainContract.View> implements MainContract.Presenter {

  private static final String TAG = "MainPresenter";
  private final DataSource dataSource;

  @Inject MainPresenter(DataSource dataSource) {
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
