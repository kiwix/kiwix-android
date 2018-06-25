package org.kiwix.kiwixmobile.language;

import android.util.Log;

import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.data.Computation;
import org.kiwix.kiwixmobile.data.DataSource;
import org.kiwix.kiwixmobile.data.IO;
import org.kiwix.kiwixmobile.data.MainThread;
import org.kiwix.kiwixmobile.di.PerActivity;
import org.kiwix.kiwixmobile.models.Language;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

@PerActivity
class LanguagePresenter extends BasePresenter<LanguageContract.View> implements LanguageContract.Presenter {
  private Scheduler mainThread;
  private Scheduler computation;
  private Scheduler io;
  private DataSource dataSource;

  @Inject
  LanguagePresenter(DataSource dataSource, @Computation Scheduler computation, @MainThread Scheduler mainThread,
                    @IO Scheduler io) {
    this.computation = computation;
    this.mainThread = mainThread;
    this.io = io;
    this.dataSource = dataSource;
  }

  @Override
  public void filerLanguages(List<Language> languages, String query) {
    Observable.fromIterable(languages)
        .filter(language -> language.language.toLowerCase().contains(query.toLowerCase()) ||
            language.languageLocalized.toLowerCase().contains(query.toLowerCase()))
        .toList()
        .subscribeOn(computation)
        .observeOn(mainThread)
        .subscribe(new SingleObserver<List<Language>>() {
          @Override
          public void onSubscribe(Disposable d) {
            compositeDisposable.add(d);
          }

          @Override
          public void onSuccess(List<Language> languages) {
            view.notifyLanguagesFiltered(languages);
          }

          @Override
          public void onError(Throwable e) {
            Log.e("LanguagePresenter", e.toString());
          }
        });
  }

  @Override
  public void saveLanguages(List<Language> languages) {
    dataSource.saveLanguages(languages)
        .subscribeOn(io)
        .observeOn(mainThread)
        .subscribe(new CompletableObserver() {
          @Override
          public void onSubscribe(Disposable d) {
            compositeDisposable.add(d);
          }

          @Override
          public void onComplete() {
            view.finishActivity();
          }

          @Override
          public void onError(Throwable e) {
            Log.e("LanguagePresenter", e.toString());
          }
        });
  }
}
