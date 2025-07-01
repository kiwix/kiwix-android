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

package org.kiwix.kiwixmobile.core.dao.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.convertToLocal
import org.kiwix.kiwixmobile.core.zim_manager.Language
import java.util.Locale

@Entity
data class LanguageRoomEntity(
  @PrimaryKey(autoGenerate = true) var id: Long = 0,
  @TypeConverters(StringToLocalRoomConverter::class)
  var locale: Locale = Locale.ENGLISH,
  var active: Boolean = false,
  var occurencesOfLanguage: Int = 0
) {
  constructor(language: Language) : this(
    0,
    language.languageCode.convertToLocal(),
    language.active,
    language.occurencesOfLanguage
  )

  fun toLanguageModel() =
    Language(locale, active, occurencesOfLanguage, id)
}

class StringToLocalRoomConverter {
  @TypeConverter
  fun convertToDatabaseValue(entityProperty: Locale?): String =
    entityProperty?.isO3Language ?: Locale.ENGLISH.isO3Language

  @TypeConverter
  fun convertToEntityProperty(databaseValue: String?): Locale =
    databaseValue?.convertToLocal() ?: Locale.ENGLISH
}
