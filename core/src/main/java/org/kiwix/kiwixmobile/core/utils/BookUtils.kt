/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.utils

import java.util.Locale

/**
 * Created by mhutti1 on 19/04/17.
 */
class BookUtils {
  val localeMap = Locale.getISOLanguages().map(::Locale).associateBy { it.isO3Language }

  // Get the language from the language codes of the parsed xml stream
  @Suppress("MagicNumber")
  fun getLanguage(languageCode: String): String {
    return when {
      languageCode == null -> ""
      languageCode.length == 2 -> LanguageContainer(languageCode).languageName
      languageCode.length == 3 -> localeMap[languageCode]?.displayLanguage.orEmpty()
      else -> ""
    }
  }
}
