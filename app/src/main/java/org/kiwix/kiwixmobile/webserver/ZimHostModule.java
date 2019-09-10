/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

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
  @Provides Activity providesActivity(ZimHostActivity zimHostActivity) {
    return zimHostActivity;
  }

  @PerActivity
  @Provides LocationServicesHelper providesLocationServicesHelper(ZimHostActivity activity,
    LocationCallbacks locationCallbacks) {
    return new LocationServicesHelper(activity, locationCallbacks);
  }

  @PerActivity
  @Provides LocationCallbacks providesLocationCallbacks(ZimHostActivity activity) {
    return activity;
  }
}

