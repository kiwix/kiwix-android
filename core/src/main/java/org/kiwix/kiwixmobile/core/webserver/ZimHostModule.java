package org.kiwix.kiwixmobile.core.webserver;

import android.app.Activity;
import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.core.di.ActivityScope;

@Module
public class ZimHostModule {

  @ActivityScope
  @Provides
  ZimHostContract.Presenter provideZimHostPresenter(ZimHostPresenter zimHostPresenter) {
    return zimHostPresenter;
  }

  @ActivityScope
  @Provides Activity providesActivity(ZimHostActivity zimHostActivity) {
    return zimHostActivity;
  }
}

