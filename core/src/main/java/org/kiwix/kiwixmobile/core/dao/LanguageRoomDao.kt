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

package org.kiwix.kiwixmobile.core.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverter
import io.reactivex.Flowable
import org.kiwix.kiwixmobile.core.dao.entities.LanguageRoomEntity
import org.kiwix.kiwixmobile.core.zim_manager.Language
import java.util.Locale

@Dao
abstract class LanguageRoomDao {
  @Query("SELECT * FROM LanguageRoomEntity")
  abstract fun languageEntityList(): Flowable<List<LanguageRoomEntity>>

  fun languages(): Flowable<List<Language>> = languageEntityList().map {
    Log.d("gouri", "language entity list $it and size ${it.size}")
    it.map(LanguageRoomEntity::toLanguageModel)
  }

  @Query("DELETE FROM LanguageRoomEntity")
  abstract fun deleteLanguages()

  @Insert
  abstract fun insert(languageRoomEntity: LanguageRoomEntity)

  @Transaction
  open fun insert(languages: List<Language>) {
    deleteLanguages()
    languages.map {
      insert(LanguageRoomEntity(it))
    }
  }
}

class StringToLocalConverterDao {
  @TypeConverter
  fun convertToDatabaseValue(entityProperty: Locale?): String {
    val foo = entityProperty?.isO3Language ?: Locale.ENGLISH.isO3Language
    return foo.also {
      Log.d("gouri", "convertToDatabaseValue success")

    }
  }

  @TypeConverter
  fun convertToEntityProperty(databaseValue: String?): Locale {

    val foo = databaseValue?.let(::Locale) ?: Locale.ENGLISH
    return foo.also {
      Log.d("gouri", "convertToEntityProperty success")

    }
  }
}



