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
import org.kiwix.kiwixmobile.core.data.KiwixRoomDatabase
import javax.inject.Singleton

@Module
open class DatabaseModule {
  @Singleton
  @Provides
  fun provideYourDatabase(
    context: Context
  ) =
    KiwixRoomDatabase.getInstance(
      context = context
    ) // The reason we can construct a database for the repo

  @Singleton
  @Provides
  fun provideNewRecentSearchRoomDao(db: KiwixRoomDatabase) = db.recentSearchRoomDao()

  @Provides
  @Singleton
  fun provideHistoryDao(db: KiwixRoomDatabase) = db.historyRoomDao()

  @Provides
  @Singleton
  fun provideAppUpdateDao(db: KiwixRoomDatabase) = db.appUpdateDao()

  @Provides
  @Singleton
  fun provideDownloadApkDao(db: KiwixRoomDatabase) = db.downloadApkDao()

  @Provides
  @Singleton
  fun provideWebViewHistoryRoomDao(db: KiwixRoomDatabase) = db.webViewHistoryRoomDao()

  @Singleton
  @Provides
  fun provideNoteRoomDao(db: KiwixRoomDatabase) = db.notesRoomDao()

  @Singleton
  @Provides
  fun provideDownloadRoomDao(db: KiwixRoomDatabase, libkiwixBookOnDisk: LibkiwixBookOnDisk) =
    db.downloadRoomDao().also {
      it.libkiwixBookOnDisk = libkiwixBookOnDisk
    }
}
