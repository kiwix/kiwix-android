package org.kiwix.kiwixmobile.main;

import android.util.Log;

import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.data.DataSource;
import org.kiwix.kiwixmobile.data.IO;
import org.kiwix.kiwixmobile.di.PerActivity;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Scheduler;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

/**
 * Presenter for {@link MainActivity}.
 */

@PerActivity
class MainPresenter extends BasePresenter<MainContract.View> implements MainContract.Presenter {

  private DataSource dataSource;
  private Scheduler io;

  @Inject
  MainPresenter(DataSource dataSource, @IO Scheduler io) {
    this.dataSource = dataSource;
    this.io = io;
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
  public void saveHistory(String file, String favicon, String url, String title, long timeStamp) {
    Completable.fromAction(() -> dataSource.saveHistory(file, favicon, url, title, timeStamp))
        .subscribeOn(io)
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
