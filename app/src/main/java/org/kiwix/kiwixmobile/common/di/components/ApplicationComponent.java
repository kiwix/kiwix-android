package org.kiwix.kiwixmobile.common.di.components;

import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.common.data.content_provider.ZimContentProvider;
import org.kiwix.kiwixmobile.modules.bookmarks_view.BookmarksActivity;
import org.kiwix.kiwixmobile.common.di.modules.ApplicationModule;
import org.kiwix.kiwixmobile.common.di.modules.JNIModule;
import org.kiwix.kiwixmobile.common.di.modules.NetworkModule;
import org.kiwix.kiwixmobile.modules.downloader.DownloadService;
import org.kiwix.kiwixmobile.modules.library.LibraryAdapter;
import org.kiwix.kiwixmobile.modules.search.SearchActivity;
import org.kiwix.kiwixmobile.modules.zim_manager.ZimManageActivity;
import org.kiwix.kiwixmobile.modules.zim_manager.fileselect_view.ZimFileSelectFragment;
import org.kiwix.kiwixmobile.modules.zim_manager.library_view.LibraryFragment;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
    ApplicationModule.class,
    NetworkModule.class,
    JNIModule.class,
})
public interface ApplicationComponent {
  void inject(KiwixMobileActivity activity);

  void inject(DownloadService service);

  void inject(LibraryFragment libraryFragment);

  void inject(BookmarksActivity bookmarksActivity);

  void inject(ZimFileSelectFragment zimFileSelectFragment);

  void inject(ZimContentProvider zimContentProvider);

  void inject(LibraryAdapter libraryAdapter);

  void inject(SearchActivity searchActivity);

  void inject(ZimManageActivity zimManageActivity);
}
