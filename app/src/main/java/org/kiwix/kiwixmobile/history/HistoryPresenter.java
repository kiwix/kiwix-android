package org.kiwix.kiwixmobile.history;

import android.util.Log;

import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.data.Computation;
import org.kiwix.kiwixmobile.data.DataSource;
import org.kiwix.kiwixmobile.data.IO;
import org.kiwix.kiwixmobile.data.MainThread;
import org.kiwix.kiwixmobile.data.local.entity.History;
import org.kiwix.kiwixmobile.di.PerActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

@PerActivity
class HistoryPresenter extends BasePresenter<HistoryContract.View> implements HistoryContract.Presenter {

  private DataSource dataSource;
  private Scheduler io;
  private Scheduler mainThread;
  private Scheduler computation;

  @Inject
  HistoryPresenter(DataSource dataSource, @IO Scheduler io, @MainThread Scheduler mainThread,
                   @Computation Scheduler computation) {
    this.dataSource = dataSource;
    this.io = io;
    this.mainThread = mainThread;
    this.computation = computation;
  }

  @Override
  public void loadHistory(boolean showHistoryCurrentBook) {
    dataSource.getDateCategorizedHistory(showHistoryCurrentBook)
        .subscribeOn(io)
        .observeOn(mainThread)
        .subscribe(new SingleObserver<List<History>>() {
          @Override
          public void onSubscribe(Disposable d) {
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
}
