/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.kiwix.kiwixmobile;

import android.app.Activity;
import android.content.Context;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;

import com.squareup.leakcanary.LeakCanary;

import org.kiwix.kiwixmobile.di.components.ApplicationComponent;
import org.kiwix.kiwixmobile.di.components.DaggerApplicationComponent;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;

public class KiwixApplication extends MultiDexApplication implements HasActivityInjector {

  private static KiwixApplication application;
  private static ApplicationComponent applicationComponent;

  static {
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
  }

  @Inject
  DispatchingAndroidInjector<Activity> activityInjector;

  public static KiwixApplication getInstance() {
    return application;
  }

  public static ApplicationComponent getApplicationComponent() {
    return applicationComponent;
  }

  public static void setApplicationComponent(ApplicationComponent applicationComponent) {
    KiwixApplication.applicationComponent = applicationComponent;
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    application = this;
    setApplicationComponent(DaggerApplicationComponent.builder()
        .applicationModule(new ApplicationModule(this))
        .build());
  }

  @Override
  public void onCreate() {
    super.onCreate();
    applicationComponent.inject(this);
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return;
    }
    LeakCanary.install(this);
  }

  @Override
  public AndroidInjector<Activity> activityInjector() {
    return activityInjector;
  }
}
