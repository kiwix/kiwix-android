/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.di.modules

import dagger.Module
import dagger.Provides
import org.kiwix.kiwixmobile.di.KiwixScope
import org.kiwix.kiwixmobile.zimManager.OnlineLibraryManager
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import javax.inject.Named

@Module
class JNIModule {
  @Provides
  @Named(ONLINE_BOOKS_LIBRARY)
  @KiwixScope
  fun provideOnlineBooksLibrary(): Library = Library()

  @Provides
  @Named(ONLINE_BOOKS_MANAGER)
  @KiwixScope
  fun providesOnlineBooksManager(
    @Named(ONLINE_BOOKS_LIBRARY) library: Library
  ): Manager = Manager(library)

  @Provides
  @KiwixScope
  fun provideOnlineLibraryManager(
    @Named(ONLINE_BOOKS_LIBRARY) library: Library,
    @Named(ONLINE_BOOKS_MANAGER) manager: Manager,
  ) = OnlineLibraryManager(library, manager)
}

const val ONLINE_BOOKS_LIBRARY = "onlineBookLibrary"
const val ONLINE_BOOKS_MANAGER = "onlineBookManager"
