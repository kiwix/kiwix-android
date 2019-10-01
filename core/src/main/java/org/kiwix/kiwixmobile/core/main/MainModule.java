package org.kiwix.kiwixmobile.core.main;

import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.core.di.ActivityScope;

@Module
public class MainModule {
  @ActivityScope
  @Provides
  MainContract.Presenter provideMainPresenter(MainPresenter mainPresenter) {
    return mainPresenter;
  }
}
