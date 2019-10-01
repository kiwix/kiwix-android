package org.kiwix.kiwixmobile.core.bookmark;

import dagger.Module;
import dagger.Provides;
import org.kiwix.kiwixmobile.core.di.ActivityScope;

@Module
public class BookmarksModule {
  @ActivityScope
  @Provides
  BookmarksContract.Presenter provideBookmarksPresenter(BookmarksPresenter presenter) {
    return presenter;
  }
}
