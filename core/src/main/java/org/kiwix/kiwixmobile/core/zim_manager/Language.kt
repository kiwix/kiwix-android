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

package org.kiwix.kiwixmobile.core.zim_manager

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Locale

@Parcelize
data class Language constructor(
  val id: Long = 0,
  var active: Boolean,
  var occurencesOfLanguage: Int,
  var language: String,
  var languageLocalized: String,
  var languageCode: String,
  var languageCodeISO2: String
) : Parcelable {

  constructor(
    locale: Locale,
    active: Boolean,
    occurrencesOfLanguage: Int,
    id: Long = 0
  ) : this(
    id,
    active,
    occurrencesOfLanguage,
    locale.displayLanguage,
    locale.getDisplayLanguage(locale),
    locale.isO3Language,
    locale.language
  )

  constructor(
    languageCode: String,
    active: Boolean,
    occurrencesOfLanguage: Int
  ) : this(Locale(languageCode), active, occurrencesOfLanguage)

  override fun equals(other: Any?): Boolean =
    (other as Language).language == language && other.active == active

  override fun hashCode(): Int {
    var result = active.hashCode()
    result = 31 * result + language.hashCode()
    return result
  }

  fun matches(filter: String) =
    language.contains(filter, true) or languageLocalized.contains(filter, true)
}
