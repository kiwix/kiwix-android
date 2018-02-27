package org.kiwix.kiwixmobile.di.components;

import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.bookmarks_view.BookmarksActivity;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;
import org.kiwix.kiwixmobile.di.modules.JNIModule;
import org.kiwix.kiwixmobile.di.modules.NetworkModule;
import org.kiwix.kiwixmobile.downloader.DownloadFragment;
import org.kiwix.kiwixmobile.downloader.DownloadService;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.search.SearchActivity;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity;
import org.kiwix.kiwixmobile.views.web.KiwixWebView;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.ZimFileSelectFragment;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment;

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

  void inject(KiwixWebView kiwixWebView);

  void inject(KiwixSettingsActivity kiwixSettingsActivity);

  void inject(KiwixSettingsActivity.PrefsFragment prefsFragment);

  void inject(DownloadFragment downloadFragment);
}
