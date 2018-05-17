package org.kiwix.kiwixmobile.common.di.modules;

import org.kiwix.kiwixmobile.error.KiwixErrorActivity;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.modules.bookmarksview.BookmarksActivity;
import org.kiwix.kiwixmobile.common.di.PerActivity;
import org.kiwix.kiwixmobile.modules.search.SearchActivity;
import org.kiwix.kiwixmobile.common.settings.KiwixSettingsActivity;
import org.kiwix.kiwixmobile.modules.zimmanager.ZimManageActivity;

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
  public abstract KiwixErrorActivity provideKiwixErrorActivity();
}
