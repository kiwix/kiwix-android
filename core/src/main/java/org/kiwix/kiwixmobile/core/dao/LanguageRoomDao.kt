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

package org.kiwix.kiwixmobile.core.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.kiwix.kiwixmobile.core.dao.entities.LanguageRoomEntity
import org.kiwix.kiwixmobile.core.zim_manager.Language

@Dao
abstract class LanguageRoomDao {
  @Query("SELECT * FROM LanguageRoomEntity")
  abstract fun languageAsEntity(): Flow<List<LanguageRoomEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun saveLanguages(languageRoomEntityList: List<LanguageRoomEntity>)

  @Query("DELETE FROM LanguageRoomEntity")
  abstract fun deleteAllLanguages()

  fun languages(): Flow<List<Language>> =
    languageAsEntity().map { it.map(LanguageRoomEntity::toLanguageModel) }

  @Transaction
  open fun insert(languages: List<Language>) {
    deleteAllLanguages()
    saveLanguages(languages.map(::LanguageRoomEntity))
  }
}
