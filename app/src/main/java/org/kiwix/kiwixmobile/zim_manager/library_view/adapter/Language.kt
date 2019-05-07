package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import java.util.Locale

class Language constructor(
  locale: Locale,
  var active: Boolean?,
  var language: String = locale.displayLanguage,
  var languageLocalized: String = locale.getDisplayLanguage(locale),
  var languageCode: String = locale.isO3Language,
  var languageCodeISO2: String = locale.language
)
{

  constructor(
    languageCode: String,
    active: Boolean?
  ) : this(Locale(languageCode), active) {
  }

  override fun equals(obj: Any?): Boolean {
    return (obj as Language).language == language && obj.active == active
  }
}