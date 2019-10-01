package org.kiwix.kiwixmobile.core.history;

import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.core.di.ActivityScope;

@Module
public class HistoryModule {
  @ActivityScope
  @Provides
  HistoryContract.Presenter provideHistoryPresenter(HistoryPresenter presenter) {
    return presenter;
  }
}
