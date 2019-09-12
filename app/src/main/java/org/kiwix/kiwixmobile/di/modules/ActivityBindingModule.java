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
import org.kiwix.kiwixmobile.webserver.ZimHostActivity;
import org.kiwix.kiwixmobile.webserver.ZimHostModule;
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

  @PerActivity
  @ContributesAndroidInjector(modules = ZimHostModule.class)
  public abstract ZimHostActivity provideZimHostActivity();
}
