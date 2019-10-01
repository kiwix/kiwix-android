package org.kiwix.kiwixmobile.core.data;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class DataModule {
  @Singleton
  @Provides
  public DataSource provideDataSource(Repository repository) {
    return repository;
  }
}
