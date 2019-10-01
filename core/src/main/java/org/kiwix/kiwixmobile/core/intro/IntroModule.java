package org.kiwix.kiwixmobile.core.intro;

import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.core.di.ActivityScope;

@Module
public class IntroModule {
  @ActivityScope
  @Provides
  IntroContract.Presenter provideIntroPresenter(IntroPresenter presenter) {
    return presenter;
  }
}
