package org.kiwix.kiwixmobile.data;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class DataModule {
  @Singleton
  @Provides
  public DataSource provideDataSource(Repository repository) {
    return repository;
  }
}
