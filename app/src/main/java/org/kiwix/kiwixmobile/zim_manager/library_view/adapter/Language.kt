package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import java.util.Locale

class Language constructor(
  locale: Locale,
  var active: Boolean,
  var occurencesOfLanguage: Int,
  var language: String = locale.displayLanguage,
  var languageLocalized: String = locale.getDisplayLanguage(locale),
  var languageCode: String = locale.isO3Language,
  var languageCodeISO2: String = locale.language
) {

  constructor(
    languageCode: String,
    active: Boolean,
    occurrencesOfLanguage: Int
  ) : this(Locale(languageCode), active, occurrencesOfLanguage) {
  }

  override fun equals(other: Any?): Boolean {
    return (other as Language).language == language && other.active == active
  }
}