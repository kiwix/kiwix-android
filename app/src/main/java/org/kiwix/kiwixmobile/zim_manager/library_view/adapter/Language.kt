package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import java.util.Locale

data class Language constructor(
  var active: Boolean,
  var occurencesOfLanguage: Int,
  var language: String,
  var languageLocalized: String,
  var languageCode: String,
  var languageCodeISO2: String
) {
  constructor(
    locale: Locale,
    active: Boolean,
    occurrencesOfLanguage: Int
  ) : this(
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

  override fun equals(other: Any?): Boolean {
    return (other as Language).language == language && other.active == active
  }
}