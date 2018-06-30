package org.kiwix.kiwixmobile.history;

import android.util.Log;

import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.data.DataSource;
import org.kiwix.kiwixmobile.data.local.entity.History;
import org.kiwix.kiwixmobile.di.PerActivity;
import org.kiwix.kiwixmobile.di.qualifiers.Computation;
import org.kiwix.kiwixmobile.di.qualifiers.MainThread;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

@PerActivity
class HistoryPresenter extends BasePresenter<HistoryContract.View> implements HistoryContract.Presenter {

  private final DataSource dataSource;
  private final Scheduler mainThread;
  private final Scheduler computation;
  private Disposable disposable;

  @Inject
  HistoryPresenter(DataSource dataSource, @MainThread Scheduler mainThread,
                   @Computation Scheduler computation) {
    this.dataSource = dataSource;
    this.mainThread = mainThread;
    this.computation = computation;
  }

  @Override
  public void loadHistory(boolean showHistoryCurrentBook) {
    dataSource.getDateCategorizedHistory(showHistoryCurrentBook)
        .subscribe(new SingleObserver<List<History>>() {
          @Override
          public void onSubscribe(Disposable d) {
            if (disposable != null && !disposable.isDisposed()) {
              disposable.dispose();
            }
            disposable = d;
            compositeDisposable.add(d);
          }

          @Override
          public void onSuccess(List<History> histories) {
            view.updateHistoryList(histories);
          }

          @Override
          public void onError(Throwable e) {
            Log.e("HistoryPresenter", e.toString());
          }
        });
  }

  @Override
  public void filterHistory(List<History> historyList, String newText) {
    Observable.just(historyList)
        .flatMapIterable(histories -> {
          List<History> historyList1 = new ArrayList<>();
          for (History history : histories) {
            if (history != null && history.getHistoryTitle().toLowerCase()
                .contains(newText.toLowerCase())) {
              historyList1.add(history);
            }
          }
          return historyList1;
        })
        .toList()
        .subscribeOn(computation)
        .observeOn(mainThread)
        .subscribe(new SingleObserver<List<History>>() {
          @Override
          public void onSubscribe(Disposable d) {
            compositeDisposable.add(d);
          }

          @Override
          public void onSuccess(List<History> languages) {
            view.notifyHistoryListFiltered(languages);
          }

          @Override
          public void onError(Throwable e) {
            Log.e("HistoryPresenter", e.toString());
          }
        });
  }

  @Override
  public void deleteHistory(List<History> deleteList) {
    dataSource.deleteHistory(deleteList)
        .subscribe(new CompletableObserver() {
          @Override
          public void onSubscribe(Disposable d) {

          }

          @Override
          public void onComplete() {

          }

          @Override
          public void onError(Throwable e) {
            Log.e("HistoryPresenter", e.toString());
          }
        });
  }
}
