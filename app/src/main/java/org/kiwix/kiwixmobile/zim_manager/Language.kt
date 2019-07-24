package org.kiwix.kiwixmobile.zim_manager

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Locale

@Parcelize
data class Language constructor(
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

  override fun equals(other: Any?): Boolean =
    (other as Language).language == language && other.active == active

  override fun hashCode(): Int {
    var result = active.hashCode()
    result = 31 * result + language.hashCode()
    return result
  }
}
