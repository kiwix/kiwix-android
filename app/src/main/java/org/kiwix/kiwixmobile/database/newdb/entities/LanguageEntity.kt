package org.kiwix.kiwixmobile.database.newdb.entities

import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.converter.PropertyConverter
import org.kiwix.kiwixmobile.zim_manager.Language
import java.util.Locale

@Entity
data class LanguageEntity(
  @Id var id: Long = 0,
  @Convert(converter = StringToLocaleConverter::class, dbType = String::class)
  val locale: Locale = Locale.ENGLISH,
  val active: Boolean = false,
  val occurencesOfLanguage: Int = 0
) {

  constructor(language: Language) : this(
    0,
    Locale(language.languageCode),
    language.active,
    language.occurencesOfLanguage
  )

  fun toLanguageModel() =
    Language(locale, active, occurencesOfLanguage, id)
}

class StringToLocaleConverter : PropertyConverter<Locale, String> {
  override fun convertToDatabaseValue(entityProperty: Locale?): String =
    entityProperty?.isO3Language ?: Locale.ENGLISH.isO3Language

  override fun convertToEntityProperty(databaseValue: String?): Locale =
    databaseValue?.let(::Locale) ?: Locale.ENGLISH
}
