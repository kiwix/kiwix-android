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
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;
import com.jakewharton.threetenabp.AndroidThreeTen;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.di.components.ApplicationComponent;
import org.kiwix.kiwixmobile.di.components.DaggerApplicationComponent;

public class KiwixApplication extends MultiDexApplication implements HasActivityInjector {

  private static KiwixApplication application;
  private static ApplicationComponent applicationComponent;

  static {
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
  }

  @Inject
  DispatchingAndroidInjector<Activity> activityInjector;
  private File logFile;

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
        .context(this)
        .build());
  }

  @Override
  public void onCreate() {
    super.onCreate();
    AndroidThreeTen.init(this);
    if (isExternalStorageWritable()) {
      File appDirectory = new File(Environment.getExternalStorageDirectory() + "/Kiwix");
      logFile = new File(appDirectory, "logcat.txt");
      Log.d("KIWIX", "Writing all logs into [" + logFile.getPath() + "]");

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
    applicationComponent.inject(this);
    if (BuildConfig.DEBUG) {
      StrictMode.setThreadPolicy(buildThreadPolicy(new StrictMode.ThreadPolicy.Builder()));
      StrictMode.setVmPolicy(buildVmPolicy(new StrictMode.VmPolicy.Builder()));
    }
  }

  private StrictMode.ThreadPolicy buildThreadPolicy(StrictMode.ThreadPolicy.Builder builder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      builder.detectResourceMismatches();
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder.detectUnbufferedIo();
    }
    return builder.detectCustomSlowCalls()
        .detectDiskReads()
        .detectDiskWrites()
        .detectNetwork()
        .penaltyFlashScreen()
        .penaltyLog()
        .build();
  }

  private StrictMode.VmPolicy buildVmPolicy(StrictMode.VmPolicy.Builder builder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      builder.detectCleartextNetwork();
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      builder.detectContentUriWithoutPermission();
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      builder.detectFileUriExposure();
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      builder.detectLeakedRegistrationObjects();
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      builder.detectNonSdkApiUsage();
    }
    return builder.detectActivityLeaks()
        .detectLeakedClosableObjects()
        .detectLeakedSqlLiteObjects()
        .penaltyLog()
        .build();
  }

  /* Checks if external storage is available for read and write */
  public boolean isExternalStorageWritable() {
    String state = Environment.getExternalStorageState();
    return Environment.MEDIA_MOUNTED.equals(state);
  }

  @Override
  public AndroidInjector<Activity> activityInjector() {
    return activityInjector;
  }
}
