package org.kiwix.kiwixmobile.main;

import org.kiwix.kiwixmobile.di.PerActivity;

import dagger.Module;
import dagger.Provides;

@Module
public class MainModule {
  @PerActivity
  @Provides
  MainContract.Presenter provideMainPresenter(MainPresenter mainPresenter) {
    return mainPresenter;
  }
}
