package org.kiwix.kiwixmobile.bookmark;

import org.kiwix.kiwixmobile.di.PerActivity;

import dagger.Module;
import dagger.Provides;

@Module
public class BookmarksModule {
  @PerActivity
  @Provides
  BookmarksContract.Presenter provideBookmarksPresenter(BookmarksPresenter presenter) {
    return presenter;
  }
}
