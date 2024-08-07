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
package org.kiwix.kiwixmobile.core.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.libkiwix.JNIKiwix
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import javax.inject.Singleton

@Module
class JNIModule {
  @Provides @Singleton
  fun providesJNIKiwix(context: Context): JNIKiwix = JNIKiwix(context)

  @Provides
  @Singleton
  fun provideLibrary(): Library = Library()

  @Provides
  @Singleton
  fun providesManager(library: Library): Manager = Manager(library)

  @Provides
  @Singleton
  fun providesLibkiwixBookmarks(
    library: Library,
    manager: Manager,
    sharedPreferenceUtil: SharedPreferenceUtil,
    bookDao: NewBookDao,
    zimReaderContainer: ZimReaderContainer
  ): LibkiwixBookmarks =
    LibkiwixBookmarks(library, manager, sharedPreferenceUtil, bookDao, zimReaderContainer)
}
