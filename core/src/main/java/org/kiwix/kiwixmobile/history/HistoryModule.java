package org.kiwix.kiwixmobile.history;

import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.di.ActivityScope;

@Module
public class HistoryModule {
  @ActivityScope
  @Provides
  HistoryContract.Presenter provideHistoryPresenter(HistoryPresenter presenter) {
    return presenter;
  }
}
