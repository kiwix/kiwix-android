package org.kiwix.kiwixmobile.bookmark;

import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.di.ActivityScope;

@Module
public class BookmarksModule {
  @ActivityScope
  @Provides
  BookmarksContract.Presenter provideBookmarksPresenter(BookmarksPresenter presenter) {
    return presenter;
  }
}
