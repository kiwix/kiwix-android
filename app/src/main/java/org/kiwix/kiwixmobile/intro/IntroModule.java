package org.kiwix.kiwixmobile.intro;

import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.di.ActivityScope;

@Module
public class IntroModule {
  @ActivityScope
  @Provides
  IntroContract.Presenter provideIntroPresenter(IntroPresenter presenter) {
    return presenter;
  }
}
