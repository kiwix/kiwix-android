package org.kiwix.kiwixmobile.di.modules;

import org.kiwix.kiwixmobile.bookmark.BookmarksActivity;
import org.kiwix.kiwixmobile.di.PerActivity;
import org.kiwix.kiwixmobile.error.ErrorActivity;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.search.SearchActivity;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/**
 * Dagger.Android annotation processor will create the sub-components. We also specify the modules
 * to be used by each sub-components and make Dagger.Android aware of a scope annotation
 * {@link PerActivity}.
 */

@Module
public abstract class ActivityBindingModule {
  @PerActivity
  @ContributesAndroidInjector
  public abstract MainActivity provideKiwixMobileActivity();

  @PerActivity
  @ContributesAndroidInjector
  public abstract KiwixSettingsActivity provideKiwixSettingsActivity();

  @PerActivity
  @ContributesAndroidInjector
  public abstract ZimManageActivity provideZimManageActivity();

  @PerActivity
  @ContributesAndroidInjector
  public abstract SearchActivity provideSearchActivity();

  @PerActivity
  @ContributesAndroidInjector
  public abstract BookmarksActivity provideBookmarksActivity();

  @PerActivity
  @ContributesAndroidInjector
  public abstract ErrorActivity provideErrorActivity();

  @PerActivity
  @ContributesAndroidInjector
  public abstract org.kiwix.kiwixmobile.main2.MainActivity provideMainActivity();
}
