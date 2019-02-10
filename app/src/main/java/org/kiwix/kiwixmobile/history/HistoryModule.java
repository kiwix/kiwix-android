package org.kiwix.kiwixmobile.history;

import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.di.PerActivity;

@Module
public class HistoryModule {
  @PerActivity
  @Provides
  HistoryContract.Presenter provideHistoryPresenter(HistoryPresenter presenter) {
    return presenter;
  }
}
