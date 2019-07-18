package org.kiwix.kiwixmobile.di.modules;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import org.kiwix.kiwixmobile.bookmark.BookmarksActivity;
import org.kiwix.kiwixmobile.bookmark.BookmarksModule;
import org.kiwix.kiwixmobile.di.PerActivity;
import org.kiwix.kiwixmobile.error.ErrorActivity;
import org.kiwix.kiwixmobile.help.HelpActivity;
import org.kiwix.kiwixmobile.history.HistoryActivity;
import org.kiwix.kiwixmobile.history.HistoryModule;
import org.kiwix.kiwixmobile.intro.IntroActivity;
import org.kiwix.kiwixmobile.intro.IntroModule;
import org.kiwix.kiwixmobile.language.LanguageActivity;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.main.MainModule;
import org.kiwix.kiwixmobile.search.SearchActivity;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity;
import org.kiwix.kiwixmobile.splash.SplashActivity;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;

/**
 * Dagger.Android annotation processor will create the sub-components. We also specify the modules
 * to be used by each sub-components and make Dagger.Android aware of a scope annotation
 * {@link PerActivity}.
 */

@Module
public abstract class ActivityBindingModule {
  @PerActivity
  @ContributesAndroidInjector(modules = MainModule.class)
  public abstract MainActivity provideMainActivity();

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
  @ContributesAndroidInjector(modules = BookmarksModule.class)
  public abstract BookmarksActivity provideBookmarksActivity();

  @PerActivity
  @ContributesAndroidInjector
  public abstract ErrorActivity provideErrorActivity();

  @PerActivity
  @ContributesAndroidInjector(modules = IntroModule.class)
  public abstract IntroActivity provideIntroActivity();

  @PerActivity
  @ContributesAndroidInjector
  public abstract SplashActivity provideSplashActivity();

  @PerActivity
  @ContributesAndroidInjector
  public abstract LanguageActivity provideLanguageActivity();

  @PerActivity
  @ContributesAndroidInjector(modules = HistoryModule.class)
  public abstract HistoryActivity provideHistoryActivity();

  @PerActivity
  @ContributesAndroidInjector
  public abstract HelpActivity provideHelpActivity();
}
