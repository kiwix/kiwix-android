/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.data;

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
