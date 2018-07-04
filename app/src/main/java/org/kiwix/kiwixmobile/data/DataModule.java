package org.kiwix.kiwixmobile.data;

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
