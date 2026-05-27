/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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
import org.kiwix.kiwixmobile.data.remote.OnlineLibraryManager
import org.kiwix.kiwixmobile.data.remote.opds.KiwixOpdsServiceFactory
import org.kiwix.kiwixmobile.data.remote.opds.KiwixOpdsServiceFactoryImpl
import org.kiwix.kiwixmobile.language.repository.LanguageRepository
import org.kiwix.kiwixmobile.language.repository.LanguageRepositoryImpl
import org.kiwix.kiwixmobile.nav.destination.library.online.repository.CategoryRepository
import org.kiwix.kiwixmobile.nav.destination.library.online.repository.CategoryRepositoryImpl
import org.kiwix.kiwixmobile.nav.destination.library.online.repository.OnlineLibraryRepository
import org.kiwix.kiwixmobile.nav.destination.library.online.repository.OnlineLibraryRepositoryImpl

@Module
class DataModule {
  @Provides
  fun provideOnlineLibraryRepository(
    onlineLibraryRepositoryImpl: OnlineLibraryRepositoryImpl
  ): OnlineLibraryRepository = onlineLibraryRepositoryImpl

  @Provides
  fun provideKiwixOpdsServiceFactory(
    onlineLibraryManager: OnlineLibraryManager
  ): KiwixOpdsServiceFactory = KiwixOpdsServiceFactoryImpl(onlineLibraryManager)

  @Provides
  fun provideLanguageRepository(
    languageRepositoryImpl: LanguageRepositoryImpl
  ): LanguageRepository = languageRepositoryImpl

  @Provides
  fun provideCategoryRepository(
    categoryRepositoryImpl: CategoryRepositoryImpl
  ): CategoryRepository = categoryRepositoryImpl
}
