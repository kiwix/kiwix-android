/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchRoomEntity

@Suppress("UnnecessaryAbstractClass")
@Database(entities = [RecentSearchRoomEntity::class], version = 1)
abstract class KiwixRoomDatabase : RoomDatabase() {
  abstract fun recentSearchRoomDao(): RecentSearchRoomDao
  abstract fun foo(): KiwixRoomDatabase

  companion object {
    private var db: KiwixRoomDatabase? = null
    abstract fun foo()
    fun getInstance(context: Context, boxStore: BoxStore): KiwixRoomDatabase {
      return db ?: synchronized(KiwixRoomDatabase::class) {
        return@getInstance db
          ?: Room.databaseBuilder(context, KiwixRoomDatabase::class.java, "KiwixRoom.db")
            // We have already database name called kiwix.db in order to avoid complexity we named as
            // kiwixRoom.db
            .addCallback(object : RoomDatabase.Callback() {
              override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.d("gouri", "onCreate")
              }

              override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Log.d("gouri", "onOpen")
              }

              override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                Log.d("gouri", "onDestructiveMigration")
              }
            })
            .build().also {
              if (!BuildConfig.BUILD_TYPE.contentEquals("fdroid")) {
                it.migrateRecentSearch(boxStore)
              }
            }
      }
    }

    fun destroyInstance() {
      db = null
    }
  }

  fun migrateRecentSearch(boxStore: BoxStore) {
    recentSearchRoomDao().migrationToRoomInsert(boxStore.boxFor())
  }
}
