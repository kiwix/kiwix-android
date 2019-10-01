package org.kiwix.kiwixmobile.core.di.modules;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import org.kiwix.kiwixmobile.core.bookmark.BookmarksActivity;
import org.kiwix.kiwixmobile.core.bookmark.BookmarksModule;
import org.kiwix.kiwixmobile.core.di.ActivityScope;
import org.kiwix.kiwixmobile.core.error.ErrorActivity;
import org.kiwix.kiwixmobile.core.help.HelpActivity;
import org.kiwix.kiwixmobile.core.history.HistoryActivity;
import org.kiwix.kiwixmobile.core.history.HistoryModule;
import org.kiwix.kiwixmobile.core.intro.IntroActivity;
import org.kiwix.kiwixmobile.core.intro.IntroModule;
import org.kiwix.kiwixmobile.core.language.LanguageActivity;
import org.kiwix.kiwixmobile.core.main.MainActivity;
import org.kiwix.kiwixmobile.core.main.MainModule;
import org.kiwix.kiwixmobile.core.search.SearchActivity;
import org.kiwix.kiwixmobile.core.settings.KiwixSettingsActivity;
import org.kiwix.kiwixmobile.core.splash.SplashActivity;
import org.kiwix.kiwixmobile.core.webserver.ZimHostActivity;
import org.kiwix.kiwixmobile.core.webserver.ZimHostModule;
import org.kiwix.kiwixmobile.core.zim_manager.ZimManageActivity;

/**
 * Dagger.Android annotation processor will create the sub-components. We also specify the modules
 * to be used by each sub-components and make Dagger.Android aware of a scope annotation
 * {@link ActivityScope}.
 */

@Module
public abstract class ActivityBindingModule {
  @ActivityScope
  @ContributesAndroidInjector(modules = MainModule.class)
  public abstract MainActivity provideMainActivity();

  @ActivityScope
  @ContributesAndroidInjector
  public abstract KiwixSettingsActivity provideKiwixSettingsActivity();

  @ActivityScope
  @ContributesAndroidInjector
  public abstract ZimManageActivity provideZimManageActivity();

  @ActivityScope
  @ContributesAndroidInjector
  public abstract SearchActivity provideSearchActivity();

  @ActivityScope
  @ContributesAndroidInjector(modules = BookmarksModule.class)
  public abstract BookmarksActivity provideBookmarksActivity();

  @ActivityScope
  @ContributesAndroidInjector
  public abstract ErrorActivity provideErrorActivity();

  @ActivityScope
  @ContributesAndroidInjector(modules = IntroModule.class)
  public abstract IntroActivity provideIntroActivity();

  @ActivityScope
  @ContributesAndroidInjector
  public abstract SplashActivity provideSplashActivity();

  @ActivityScope
  @ContributesAndroidInjector
  public abstract LanguageActivity provideLanguageActivity();

  @ActivityScope
  @ContributesAndroidInjector(modules = HistoryModule.class)
  public abstract HistoryActivity provideHistoryActivity();

  @ActivityScope
  @ContributesAndroidInjector
  public abstract HelpActivity provideHelpActivity();

  @ActivityScope
  @ContributesAndroidInjector(modules = ZimHostModule.class)
  public abstract ZimHostActivity provideZimHostActivity();
}
