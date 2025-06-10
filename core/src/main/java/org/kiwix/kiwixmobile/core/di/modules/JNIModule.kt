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
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.libkiwix.JNIKiwix
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import javax.inject.Named
import javax.inject.Singleton

@Module
class JNIModule {
  @Provides @Singleton
  fun providesJNIKiwix(context: Context): JNIKiwix = JNIKiwix(context)

  @Provides
  @Singleton
  @Named(BOOKMARK_LIBRARY)
  fun provideBookmarkLibrary(): Library = Library()

  @Provides
  @Singleton
  @Named(BOOKMARK_MANAGER)
  fun providesBookmarkManager(
    @Named(BOOKMARK_LIBRARY) library: Library
  ): Manager = Manager(library)

  @Provides
  @Singleton
  fun providesLibkiwixBookmarks(
    @Named(BOOKMARK_LIBRARY) library: Library,
    @Named(BOOKMARK_MANAGER) manager: Manager,
    sharedPreferenceUtil: SharedPreferenceUtil,
    libkiwixBookOnDisk: LibkiwixBookOnDisk,
    zimReaderContainer: ZimReaderContainer
  ): LibkiwixBookmarks =
    LibkiwixBookmarks(
      library,
      manager,
      sharedPreferenceUtil,
      libkiwixBookOnDisk,
      zimReaderContainer
    )

  @Provides
  @Singleton
  @Named(LOCAL_BOOKS_LIBRARY)
  fun provideLocalBooksLibrary(): Library = Library()

  @Provides
  @Singleton
  @Named(LOCAL_BOOKS_MANAGER)
  fun providesLocalBooksManager(
    @Named(LOCAL_BOOKS_LIBRARY) library: Library
  ): Manager = Manager(library)

  @Provides
  @Singleton
  fun providesLibkiwixBooks(
    @Named(LOCAL_BOOKS_LIBRARY) library: Library,
    @Named(LOCAL_BOOKS_MANAGER) manager: Manager,
    sharedPreferenceUtil: SharedPreferenceUtil,
  ): LibkiwixBookOnDisk = LibkiwixBookOnDisk(library, manager, sharedPreferenceUtil)
}

const val BOOKMARK_LIBRARY = "bookmarkLibrary"
const val BOOKMARK_MANAGER = "bookmarkManager"
const val LOCAL_BOOKS_LIBRARY = "localBooksLibrary"
const val LOCAL_BOOKS_MANAGER = "localBooksManager"
