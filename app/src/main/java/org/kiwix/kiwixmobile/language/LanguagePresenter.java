package org.kiwix.kiwixmobile.language;

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
import org.kiwix.kiwixmobile.models.Language;

@PerActivity
class LanguagePresenter extends BasePresenter<LanguageContract.View>
    implements LanguageContract.Presenter {
  private final Scheduler mainThread;
  private final Scheduler computation;
  private final DataSource dataSource;

  @Inject LanguagePresenter(DataSource dataSource, @Computation Scheduler computation,
      @MainThread Scheduler mainThread) {
    this.computation = computation;
    this.mainThread = mainThread;
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
        .subscribe(new CompletableObserver() {
          @Override
          public void onSubscribe(Disposable d) {

          }

          @Override
          public void onComplete() {

          }

          @Override
          public void onError(Throwable e) {
            Log.e("LanguagePresenter", e.toString());
          }
        });
  }
}
