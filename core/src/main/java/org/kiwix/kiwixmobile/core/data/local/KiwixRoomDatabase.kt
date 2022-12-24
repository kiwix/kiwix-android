/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import org.kiwix.kiwixmobile.core.dao.NewRecentSearchRoomDao
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchRoomEntity

@Suppress("UnnecessaryAbstractClass")
@Database(entities = [RecentSearchRoomEntity::class], version = 1)
abstract class KiwixRoomDatabase : RoomDatabase() {
  abstract fun newRecentSearchRoomDao(): NewRecentSearchRoomDao

  companion object {
    private var db: KiwixRoomDatabase? = null
    fun getInstance(context: Context, boxStore: BoxStore): KiwixRoomDatabase {
      return db ?: synchronized(KiwixRoomDatabase::class) {
        return@getInstance db
          ?: Room.databaseBuilder(context, KiwixRoomDatabase::class.java, "KiwixRoom.db")
            // We have already database name called kiwix.db in order to avoid complexity we named as
            // kiwixRoom.db
            .build().also {
              it.migrateRecentSearch(boxStore)
            }
      }
    }

    fun destroyInstance() {
      db = null
    }
  }

  fun migrateRecentSearch(boxStore: BoxStore) {
    newRecentSearchRoomDao().migrationToRoomInsert(boxStore.boxFor())
  }
}
