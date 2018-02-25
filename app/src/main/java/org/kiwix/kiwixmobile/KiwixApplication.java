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
