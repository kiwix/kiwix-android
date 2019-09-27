package org.kiwix.kiwixmobile.utils

import java.util.Locale

class LanguageContainer private constructor(val languageCode: String, val languageName: String) {
  constructor(languageCode: String) : this(languageCode, chooseLanguageName(languageCode))

  companion object {
    private fun chooseLanguageName(
      languageCode: String
    ): String {
      val displayLanguage = Locale(languageCode).displayLanguage
      return if (displayLanguage.length == 2 || displayLanguage.isEmpty())
        Locale.ENGLISH.displayLanguage
      else
        displayLanguage
    }
  }
}
