package org.kiwix.kiwixmobile;

import android.app.Application;

import android.content.Context;
import android.util.Log;
import org.kiwix.kiwixmobile.di.components.ApplicationComponent;
import org.kiwix.kiwixmobile.di.components.DaggerApplicationComponent;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;

public class KiwixApplication extends Application {

  private static KiwixApplication application;
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

  @Override
  public void onCreate() {
    super.onCreate();

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
