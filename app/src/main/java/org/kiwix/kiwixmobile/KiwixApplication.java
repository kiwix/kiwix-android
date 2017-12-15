package org.kiwix.kiwixmobile;

import android.content.Context;
import android.os.Environment;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import org.kiwix.kiwixmobile.di.components.ApplicationComponent;
import org.kiwix.kiwixmobile.di.components.DaggerApplicationComponent;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class KiwixApplication extends MultiDexApplication {

  private static KiwixApplication application;
  private ApplicationComponent applicationComponent;
  private File logFile;

  @Override
  public void onCreate() {
    super.onCreate();

    if (isExternalStorageWritable()) {
      File appDirectory = new File(Environment.getExternalStorageDirectory() + "/Kiwix");
      logFile = new File(appDirectory, "logcat.txt");
      Log.d("KIWIX","Writing all logs into [" + logFile.getPath() + "]");

      // create app folder
      if (!appDirectory.exists()) {
        appDirectory.mkdir();
      }

      // create log folder
      if (!appDirectory.exists()) {
        appDirectory.mkdir();
      }

      if (logFile.exists() && logFile.isFile()) {
        logFile.delete();
      }

      // clear the previous logcat and then write the new one to the file
      try {
        logFile.createNewFile();
        Process process = Runtime.getRuntime().exec("logcat -c");
        process = Runtime.getRuntime().exec("logcat -f " + logFile.getPath() + " -s kiwix");
      } catch (IOException e) {
        Log.e("KIWIX", "Error while writing logcat.txt", e);
      }
    }

    Log.d("KIWIX", "Started KiwixApplication");
  }

  /* Checks if external storage is available for read and write */
  public boolean isExternalStorageWritable() {
    String state = Environment.getExternalStorageState();
    return Environment.MEDIA_MOUNTED.equals(state);
  }

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
