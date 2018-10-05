package org.kiwix.kiwixmobile.zim_manager;

import org.kiwix.kiwixmobile.di.PerFragment;
import org.kiwix.kiwixmobile.zim_manager.download.DownloadFragment;
import org.kiwix.kiwixmobile.zim_manager.fileselect.ZimFileSelectFragment;
import org.kiwix.kiwixmobile.zim_manager.library.LibraryFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class ZimManageModule {
  @PerFragment
  @ContributesAndroidInjector
  abstract ZimFileSelectFragment provideZimFileSelectFragment();

  @PerFragment
  @ContributesAndroidInjector
  abstract LibraryFragment provideLibraryFragment();

  @PerFragment
  @ContributesAndroidInjector
  abstract DownloadFragment provideDownloadFragment();
}
