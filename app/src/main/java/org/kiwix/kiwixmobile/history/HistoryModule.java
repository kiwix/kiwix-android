package org.kiwix.kiwixmobile.history;

import org.kiwix.kiwixmobile.di.PerActivity;

import dagger.Module;
import dagger.Provides;

@Module
public class HistoryModule {
  @PerActivity
  @Provides
  HistoryContract.Presenter provideHistoryPresenter(HistoryPresenter presenter) {
    return presenter;
  }
}
