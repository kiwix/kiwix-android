package org.kiwix.kiwixmobile.di.components;

import dagger.Component;
import javax.inject.Singleton;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.LibraryFragment;
import org.kiwix.kiwixmobile.bookmarksView.BookmarksActivity;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;
import org.kiwix.kiwixmobile.di.modules.NetworkModule;
import org.kiwix.kiwixmobile.downloader.DownloadService;

@Singleton
@Component(modules = {
    ApplicationModule.class,
    NetworkModule.class,
})
public interface ApplicationComponent {
  void inject(KiwixMobileActivity activity);

  void inject(DownloadService service);

  void inject(LibraryFragment libraryFragment);

  void inject(BookmarksActivity bookmarksActivity);
}
