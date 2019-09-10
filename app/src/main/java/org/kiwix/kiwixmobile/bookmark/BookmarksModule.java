/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.bookmark;

import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.di.PerActivity;

@Module
public class BookmarksModule {
  @PerActivity
  @Provides
  BookmarksContract.Presenter provideBookmarksPresenter(BookmarksPresenter presenter) {
    return presenter;
  }
}
