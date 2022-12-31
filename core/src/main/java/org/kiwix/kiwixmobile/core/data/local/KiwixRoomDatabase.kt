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
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import org.kiwix.kiwixmobile.core.dao.LanguageRoomDao
import org.kiwix.kiwixmobile.core.dao.NewRecentSearchRoomDao
import org.kiwix.kiwixmobile.core.dao.NotesRoomDao
import org.kiwix.kiwixmobile.core.dao.StringToLocalConverterDao
import org.kiwix.kiwixmobile.core.dao.entities.LanguageRoomEntity
import org.kiwix.kiwixmobile.core.dao.entities.NotesRoomEntity
import org.kiwix.kiwixmobile.core.dao.entities.RecentSearchRoomEntity

@Suppress("UnnecessaryAbstractClass")
@Database(
  entities = [RecentSearchRoomEntity::class, NotesRoomEntity::class, LanguageRoomEntity::class],
  version = 2
)
@TypeConverters(StringToLocalConverterDao::class)
abstract class KiwixRoomDatabase : RoomDatabase() {
  abstract fun newRecentSearchRoomDao(): NewRecentSearchRoomDao
  abstract fun noteRoomDao(): NotesRoomDao
  abstract fun languageRoomDao(): LanguageRoomDao

  companion object {
    private var db: KiwixRoomDatabase? = null
    private lateinit var boxStore: BoxStore
    fun getInstance(context: Context, boxStore: BoxStore): KiwixRoomDatabase {
      this.boxStore = boxStore
      return db ?: synchronized(KiwixRoomDatabase::class) {
        return@getInstance db
          ?: Room.databaseBuilder(context, KiwixRoomDatabase::class.java, "KiwixRoom.db")
            // We have already database name called kiwix.db in order to avoid complexity we named as
            // kiwixRoom.db
            .build().also {
              it.migrateRecentSearch(boxStore)
              it.migrateNote(boxStore)
            }
      }
    }

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(database: SupportSQLiteDatabase) {
        Log.d("gouri", "migration helper started and ${db == null} and ${boxStore == null}")
        // database.db?.migrateNote(boxStore)
      }
    }

    fun destroyInstance() {
      db = null
    }
  }

  fun migrateRecentSearch(boxStore: BoxStore) {
    newRecentSearchRoomDao().migrationToRoomInsert(boxStore.boxFor())
  }

  fun migrateNote(boxStore: BoxStore) {
    noteRoomDao().migrationToRoomInsert(boxStore.boxFor())
  }
}
