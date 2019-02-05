package org.kiwix.kiwixmobile.language;

import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.di.PerActivity;

@Module
public class LanguageModule {
  @PerActivity
  @Provides
  LanguageContract.Presenter provideLanguagePresenter(LanguagePresenter presenter) {
    return presenter;
  }
}
