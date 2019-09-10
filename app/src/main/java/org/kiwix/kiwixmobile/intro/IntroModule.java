/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.intro;

import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.di.PerActivity;

@Module
public class IntroModule {
  @PerActivity
  @Provides
  IntroContract.Presenter provideIntroPresenter(IntroPresenter presenter) {
    return presenter;
  }
}
