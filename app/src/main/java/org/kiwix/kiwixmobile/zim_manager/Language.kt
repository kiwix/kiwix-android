package org.kiwix.kiwixmobile.zim_manager

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
