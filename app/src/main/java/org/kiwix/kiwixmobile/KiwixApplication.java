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

import android.content.Context;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AppCompatDelegate;

import org.kiwix.kiwixmobile.di.components.ApplicationComponent;
import org.kiwix.kiwixmobile.di.components.DaggerApplicationComponent;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;

public class KiwixApplication extends MultiDexApplication {

  private static KiwixApplication application;

  static {
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
  }

  private ApplicationComponent applicationComponent;

  public static KiwixApplication getInstance() {
    return application;
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    application = this;
    initializeInjector();
  }

  private void initializeInjector() {
    setApplicationComponent(DaggerApplicationComponent.builder()
        .applicationModule(new ApplicationModule(this))
        .build());
  }

  public ApplicationComponent getApplicationComponent() {
    return this.applicationComponent;
  }

  public void setApplicationComponent(ApplicationComponent applicationComponent) {
    this.applicationComponent = applicationComponent;
  }
}
