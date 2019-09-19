package org.kiwix.kiwixmobile.main;

import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.di.PerActivity;

@Module
public class MainModule {
  @PerActivity
  @Provides
  MainContract.Presenter provideMainPresenter(MainPresenter mainPresenter) {
    return mainPresenter;
  }
}
