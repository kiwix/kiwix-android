/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 *
 */
package org.kiwix.kiwixmobile.core;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDex;
import com.jakewharton.threetenabp.AndroidThreeTen;
import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.core.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.core.di.components.CoreComponent;
import org.kiwix.kiwixmobile.core.di.components.DaggerCoreComponent;
import org.kiwix.kiwixmobile.core.downloader.DownloadMonitor;

public abstract class CoreApp extends Application {

  private static CoreApp app;
  private static CoreComponent coreComponent;

  static {
    AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
  }

  @Inject
  DownloadMonitor downloadMonitor;
  @Inject
  NightModeConfig nightModeConfig;
  @Inject
  KiwixDatabase kiwixDatabase;

  /**
   * The init of this class does the work of initializing,
   * simply injecting it is all that there is to be done
   */
  @SuppressWarnings("unused")
  @Inject
  JNIInitialiser jniInitialiser;

  public static CoreApp getInstance() {
    return app;
  }

  public static CoreComponent getCoreComponent() {
    return coreComponent;
  }

  public static void setCoreComponent(CoreComponent appComponent) {
    CoreApp.coreComponent = appComponent;
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    if (BuildConfig.DEBUG) {
      MultiDex.install(this);
    }
    app = this;
    setCoreComponent(DaggerCoreComponent.builder()
      .context(this)
      .build());
  }

  @Override
  public void onCreate() {
    super.onCreate();
    AndroidThreeTen.init(this);
    writeLogFile();
    coreComponent.inject(this);
    kiwixDatabase.forceMigration();
    downloadMonitor.init();
    nightModeConfig.init();
    if (BuildConfig.DEBUG) {
      StrictMode.setThreadPolicy(buildThreadPolicy(new StrictMode.ThreadPolicy.Builder()));
      StrictMode.setVmPolicy(buildVmPolicy(new StrictMode.VmPolicy.Builder()));
    }
  }

  private void writeLogFile() {
    if (isExternalStorageWritable()) {
      File appDirectory = new File(Environment.getExternalStorageDirectory() + "/Kiwix");
      File logFile = new File(appDirectory, "logcat.txt");
      Log.d("KIWIX", "Writing all logs into [" + logFile.getPath() + "]");
      // create app folder
      if (!appDirectory.exists()) {
        appDirectory.mkdir();
      }
      if (logFile.exists() && logFile.isFile()) {
        logFile.delete();
      }
      // clear the previous logcat and then write the new one to the file
      try {
        logFile.createNewFile();
        Runtime.getRuntime().exec("logcat -c");
        Runtime.getRuntime().exec("logcat -f " + logFile.getPath() + " -s kiwix");
      } catch (IOException e) {
        Log.e("KIWIX", "Error while writing logcat.txt", e);
      }
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      builder.detectNonSdkApiUsage();
    }
    return builder.detectActivityLeaks()
      .detectLeakedClosableObjects()
      .detectLeakedSqlLiteObjects()
      .penaltyLog()
      .detectLeakedRegistrationObjects()
      .build();
  }

  /* Checks if external storage is available for read and write */
  public boolean isExternalStorageWritable() {
    String state = Environment.getExternalStorageState();
    return Environment.MEDIA_MOUNTED.equals(state);
  }
}
