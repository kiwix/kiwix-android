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

import java.util.HashMap
import java.util.Locale

/**
 * Created by mhutti1 on 19/04/17.
 */
class BookUtils {
  val localeMap: Map<String, Locale>

  // Create a map of ISO 369-2 language codes
  init {
    val languages = Locale.getISOLanguages()
    localeMap = HashMap(languages.size)
    languages
      .asSequence()
      .map(::Locale)
      .forEach { localeMap.put(it.isO3Language, it) }
  }

  // Get the language from the language codes of the parsed xml stream
  fun getLanguage(languageCode: String?): String {
    var language = ""
    if (languageCode == null) {
      language = ""
    } else {
      if (languageCode.length == 2)
        language = LanguageContainer(languageCode).languageName
      else if (languageCode.length == LANGUAGE_CODE_LENGTH_THREE) {
        val locale = localeMap[languageCode]
        language = locale?.displayLanguage.toString()
      }
    }
    return language
  }

  companion object {
    const val LANGUAGE_CODE_LENGTH_THREE = 3
  }
}
