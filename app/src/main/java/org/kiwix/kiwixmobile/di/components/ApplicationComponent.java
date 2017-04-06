package org.kiwix.kiwixmobile.di.components;

import dagger.Component;
import javax.inject.Singleton;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.di.modules.ApplicationModule;
import org.kiwix.kiwixmobile.di.modules.NetworkModule;
import org.kiwix.kiwixmobile.downloader.DownloadService;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment;

@Singleton
@Component(modules = {
    ApplicationModule.class,
    NetworkModule.class,
})
public interface ApplicationComponent {
  void inject(KiwixMobileActivity activity);

  void inject(DownloadService service);

  void inject(LibraryFragment libraryFragment);

}
