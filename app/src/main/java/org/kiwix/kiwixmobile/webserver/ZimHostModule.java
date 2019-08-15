package org.kiwix.kiwixmobile.webserver;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import org.kiwix.kiwixmobile.di.PerActivity;
import org.kiwix.kiwixmobile.utils.AlertDialogShower;

@Module
public class ZimHostModule {

  @PerActivity
  @Provides
  ZimHostContract.Presenter provideZimHostPresenter(ZimHostPresenter zimHostPresenter) {
    return zimHostPresenter;
  }

  @Provides
  @Singleton AlertDialogShower provideAlertDialogShower(AlertDialogShower alertDialogShower) {
    return alertDialogShower;
  }
}

