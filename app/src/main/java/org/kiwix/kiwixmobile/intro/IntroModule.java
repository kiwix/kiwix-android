package org.kiwix.kiwixmobile.intro;

import org.kiwix.kiwixmobile.di.PerActivity;

import dagger.Module;
import dagger.Provides;

@Module
public class IntroModule {
  @PerActivity
  @Provides
  IntroContract.Presenter provideIntroPresenter(IntroPresenter presenter) {
    return presenter;
  }
}
