package org.kiwix.kiwixmobile.language;

import org.kiwix.kiwixmobile.di.PerActivity;

import dagger.Module;
import dagger.Provides;

@Module
public class LanguageModule {
  @PerActivity
  @Provides
  LanguageContract.Presenter provideLanguagePresenter(LanguagePresenter presenter) {
    return presenter;
  }
}
