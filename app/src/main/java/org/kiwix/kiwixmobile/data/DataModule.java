package org.kiwix.kiwixmobile.data;

import org.kiwix.kiwixmobile.di.qualifiers.Computation;
import org.kiwix.kiwixmobile.di.qualifiers.IO;
import org.kiwix.kiwixmobile.di.qualifiers.MainThread;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@Module
public class DataModule {
  @Singleton
  @Provides
  public DataSource provideDataSource(Repository repository) {
    return repository;
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
}
