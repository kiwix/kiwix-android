package org.kiwix.kiwixmobile.main;

import android.util.Log;

import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.data.DataSource;
import org.kiwix.kiwixmobile.data.local.entity.Bookmark;
import org.kiwix.kiwixmobile.data.local.entity.History;
import org.kiwix.kiwixmobile.di.PerActivity;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

/**
 * Presenter for {@link MainActivity}.
 */

@PerActivity
class MainPresenter extends BasePresenter<MainContract.View> implements MainContract.Presenter {

  private final DataSource dataSource;

  @Inject
  MainPresenter(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void showHome() {
    dataSource.getLanguageCategorizedBooks()
        .subscribe(new SingleObserver<List<LibraryNetworkEntity.Book>>() {
          @Override
          public void onSubscribe(Disposable d) {
            compositeDisposable.add(d);
          }

          @Override
          public void onSuccess(List<LibraryNetworkEntity.Book> books) {
            view.addBooks(books);
          }

          @Override
          public void onError(Throwable e) {
            Log.d("MainPresenter", e.toString());
          }
        });
  }

  @Override
  public void saveBooks(List<LibraryNetworkEntity.Book> book) {
    dataSource.saveBooks(book);
    showHome();
  }

  @Override
  public void saveHistory(History history) {
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
            Log.e("MainPresenter", e.toString());
          }
        });
  }

  @Override
  public void loadCurrentZimBookmarksUrl() {
    compositeDisposable.add(dataSource.getCurrentZimBookmarksUrl()
        .subscribe(view::refreshBookmarksUrl, e -> Log.e("MainPresenter", e.toString())));
  }

  @Override
  public void saveBookmark(Bookmark bookmark) {
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
            Log.e("MainPresenter", e.toString());
          }
        });
  }

  @Override
  public void deleteBookmark(Bookmark bookmark) {
    dataSource.deleteBookmark(bookmark)
        .subscribe(new CompletableObserver() {
          @Override
          public void onSubscribe(Disposable d) {

          }

          @Override
          public void onComplete() {

          }

          @Override
          public void onError(Throwable e) {
            Log.e("MainPresenter", e.toString());
          }
        });
  }
}
