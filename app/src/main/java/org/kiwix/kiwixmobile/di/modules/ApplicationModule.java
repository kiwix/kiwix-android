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
package org.kiwix.kiwixmobile.di.modules;

import android.app.Application;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.Context;
import android.location.LocationManager;
import android.os.storage.StorageManager;
import dagger.Module;
import dagger.Provides;
import dagger.android.AndroidInjectionModule;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Singleton;
import org.kiwix.kiwixmobile.di.qualifiers.Computation;
import org.kiwix.kiwixmobile.di.qualifiers.IO;
import org.kiwix.kiwixmobile.di.qualifiers.MainThread;
import org.kiwix.kiwixmobile.downloader.DownloadMonitor;
import org.kiwix.kiwixmobile.downloader.fetch.FetchDownloadMonitor;
import org.kiwix.kiwixmobile.utils.BookUtils;

@Module(includes = {
  ActivityBindingModule.class,
  AndroidInjectionModule.class,
  DownloaderModule.class,
  ViewModelModule.class,
  DatabaseModule.class
})
public class ApplicationModule {

  @Provides @Singleton Application provideApplication(Context context) {
    return (Application) context;
  }

  @Provides
  @Singleton
  NotificationManager provideNotificationManager(Context context) {
    return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  @Provides @Singleton DownloadManager provideDownloadManager(Context context) {
    return (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
  }

  @Provides
  @Singleton
  BookUtils provideBookUtils() {
    return new BookUtils();
  }

  @IO
  @Provides
  public Scheduler provideIoThread() {
    return Schedulers.io();
  }

  @MainThread
  @Provides
  public Scheduler provideMainThread() {
    return AndroidSchedulers.mainThread();
  }

  @Computation
  @Provides
  public Scheduler provideComputationThread() {
    return Schedulers.computation();
  }

  @Provides @Singleton
  LocationManager provideLocationManager(Context context) {
    return (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
  }

  @Provides @Singleton
  DownloadMonitor provideDownloadMonitor(FetchDownloadMonitor fetchDownloadMonitor) {
    return fetchDownloadMonitor;
  }

  @Provides @Singleton
  StorageManager provideStorageManager(Context context) {
    return (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
  }
}
