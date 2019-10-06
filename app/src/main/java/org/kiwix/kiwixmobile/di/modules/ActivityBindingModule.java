/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.di.modules;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import org.kiwix.kiwixmobile.bookmark.BookmarksActivity;
import org.kiwix.kiwixmobile.bookmark.BookmarksModule;
import org.kiwix.kiwixmobile.di.ActivityScope;
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
import org.kiwix.kiwixmobile.webserver.ZimHostActivity;
import org.kiwix.kiwixmobile.webserver.ZimHostModule;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;

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
