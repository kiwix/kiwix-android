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
package org.kiwix.kiwixmobile.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import org.kiwix.kiwixmobile.database.newdb.dao.FetchDownloadDao
import org.kiwix.kiwixmobile.database.newdb.dao.HistoryDao
import org.kiwix.kiwixmobile.database.newdb.dao.NewBookDao
import org.kiwix.kiwixmobile.database.newdb.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.database.newdb.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.database.newdb.dao.NewRecentSearchDao
import org.kiwix.kiwixmobile.database.newdb.entities.MyObjectBox
import javax.inject.Singleton

@Module
open class DatabaseModule {
  companion object {
    var boxStore: BoxStore? = null
  }

  // NOT RECOMMENDED TODO use custom runner to load TestApplication
  @Provides @Singleton fun providesBoxStore(context: Context): BoxStore {
    if (boxStore == null) {
      boxStore = MyObjectBox.builder().androidContext(context).build()
    }
    return boxStore!!
  }

  @Provides @Singleton fun providesNewBookDao(boxStore: BoxStore): NewBookDao =
    NewBookDao(boxStore.boxFor())

  @Provides @Singleton fun providesNewLanguagesDao(boxStore: BoxStore): NewLanguagesDao =
    NewLanguagesDao(boxStore.boxFor())

  @Provides @Singleton fun providesNewHistoryDao(boxStore: BoxStore): HistoryDao =
    HistoryDao(boxStore.boxFor())

  @Provides @Singleton fun providesNewBookmarksDao(boxStore: BoxStore): NewBookmarksDao =
    NewBookmarksDao(boxStore.boxFor())

  @Provides @Singleton fun providesNewRecentSearchDao(boxStore: BoxStore): NewRecentSearchDao =
    NewRecentSearchDao(boxStore.boxFor())

  @Provides @Singleton fun providesFetchDownloadDao(
    boxStore: BoxStore,
    newBookDao: NewBookDao
  ): FetchDownloadDao =
    FetchDownloadDao(boxStore.boxFor(), newBookDao)
}
