package org.kiwix.kiwixmobile;

import android.app.Application;
import org.kiwix.kiwixmobile.di.components.ApplicationComponent;
import org.kiwix.kiwixmobile.di.components.DaggerApplicationComponent;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;

public class KiwixApplication extends Application {

  private static KiwixApplication application;
  private ApplicationComponent applicationComponent;

  public static KiwixApplication getInstance() {
    return application;
  }

  @Override public void onCreate() {
    super.onCreate();
    application = this;
    initializeInjector();
  }

  private void initializeInjector() {
    this.applicationComponent = DaggerApplicationComponent.builder()
        .applicationModule(new ApplicationModule(this))
        .build();
  }

  public ApplicationComponent getApplicationComponent() {
    return this.applicationComponent;
  }
}
