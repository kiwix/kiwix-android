package org.kiwix.kiwixmobile.webserver;

import android.app.Activity;
import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.di.PerActivity;

@Module
public class ZimHostModule {

  @PerActivity
  @Provides
  ZimHostContract.Presenter provideZimHostPresenter(ZimHostPresenter zimHostPresenter) {
    return zimHostPresenter;
  }

  @PerActivity
  @Provides Activity providesActivity(ZimHostActivity zimActivity) {
    return (Activity) zimActivity;
  }
}

