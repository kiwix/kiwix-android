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
import org.kiwix.kiwixmobile.core.search.SearchActivity;

/**
 * Dagger.Android annotation processor will create the sub-components. We also specify the modules
 * to be used by each sub-components and make Dagger.Android aware of a scope annotation
 * {@link ActivityScope}.
 */

@Module
public abstract class ActivityBindingModule {

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
  @ContributesAndroidInjector(modules = HistoryModule.class)
  public abstract HistoryActivity provideHistoryActivity();

  @ActivityScope
  @ContributesAndroidInjector
  public abstract HelpActivity provideHelpActivity();
}
