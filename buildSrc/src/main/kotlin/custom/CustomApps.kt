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

package custom

import com.android.build.gradle.internal.dsl.ProductFlavor
import custom.CustomApps.CustomApp
import org.gradle.api.NamedDomainObjectContainer
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CustomApps {
  private val example = CustomApp(
    name = "customexample",
    url = "http://download.kiwix.org/zim/wikipedia_fr_test.zim",
    enforcedLanguage = "en",
    displayName = "Test Custom App"
  )
  private val phet = CustomApp(
    name = "phet",
    url = "http://download.kiwix.org/zim/phet/phet_mul_2018-09.zim",
    enforcedLanguage = "en",
    displayName = "PhET"
  )
  private val tunisie = CustomApp(
    name = "tunisie",
    url = "http://download.kiwix.org/zim/wikipedia_fr_tunisie_novid.zim",
    enforcedLanguage = "fr",
    versionName = "2018-07",
    displayName = "Encyclopédie de la Tunisie"
  )
  private val venezuela = CustomApp(
    name = "venezuela",
    url = "http://download.kiwix.org/zim/wikipedia/wikipedia_es_venezuela_2018-07.zim",
    enforcedLanguage = "es",
    displayName = "Enciclopedia de Venezuela"
  )
  private val wikimed = CustomApp(
    name = "wikimed",
    url = "http://download.kiwix.org/zim/wikipedia_en_medicine_novid.zim",
    enforcedLanguage = "en",
    versionName = "2018-08",
    displayName = "Medical Wikipedia"
  )
  private val wikimedar = CustomApp(
    name = "wikimedar",
    url = "http://download.kiwix.org/zim/wikipedia/wikipedia_ar_medicine_novid_2018-08.zim",
    enforcedLanguage = "ar",
    displayName = "وِيكيبيديا الطبية (بلا اتصال بالانترنت)"
  )
  private val wikimedde = CustomApp(
    name = "wikimedde",
    url = "http://download.kiwix.org/zim/wikipedia/wikipedia_de_medicine_novid_2018-10.zim",
    enforcedLanguage = "de",
    displayName = "Wikipedia Medizin (Offline)"
  )
  private val wikimedes = CustomApp(
    name = "wikimedes",
    url = "http://download.kiwix.org/zim/wikipedia/wikipedia_es_medicine_novid_2018-10.zim",
    enforcedLanguage = "es",
    displayName = "Wikipedia Médica (Offline)"
  )
  private val wikimedfa = CustomApp(
    name = "wikimedfa",
    url = "http://download.kiwix.org/zim/wikipedia/wikipedia_fa_medicine_novid_2018-07.zim",
    enforcedLanguage = "fa",
    displayName = "ویکی‌پدیای پزشکی (آفلاین)"
  )
  private val wikimedfr = CustomApp(
    name = "wikimedfr",
    url = "http://download.kiwix.org/zim/wikipedia/wikipedia_fr_medicine_novid_2018-07.zim",
    enforcedLanguage = "fr",
    displayName = "Wikipédia médicale"
  )
  private val wikimedja = CustomApp(
    name = "wikimedja",
    url = "http://download.kiwix.org/zim/wikipedia/wikipedia_ja_medicine_novid_2018-07.zim",
    enforcedLanguage = "ja",
    displayName = "医療ウィキペディア(オフライン)"
  )
  private val wikimedmini = CustomApp(
    name = "wikimedmini",
    url = "http://download.kiwix.org/zim/wikipedia/wikipedia_en_medicine_nodet_2018-07.zim",
    enforcedLanguage = "en",
    displayName = "Offline WikiMed mini"
  )
  private val wikimedor = CustomApp(
    name = "wikimedor",
    url = "http://download.kiwix.org/zim/wikipedia/wikipedia_or_medicine_novid_2018-07.zim",
    enforcedLanguage = "or",
    displayName = "ମେଡିକାଲ ଉଇକିପିଡିଆ (ଅଫଲାଇନ)"
  )
  private val wikimedpt = CustomApp(
    name = "wikimedpt",
    url = "http://download.kiwix.org/zim/wikipedia/wikipedia_pt_medicine_2018-10.zim",
    enforcedLanguage = "pt",
    displayName = "Wikipédia Médica (Offline)"
  )
  private val wikimedzh = CustomApp(
    name = "wikimedzh",
    url = "http://download.kiwix.org/zim/wikipedia/wikipedia_zh_medicine_novid_2018-07.zim",
    enforcedLanguage = "zh",
    displayName = "醫學維基百科(離線版)"
  )
  private val wikispecies = CustomApp(
    name = "wikispecies",
    url = "http://download.kiwix.org/zim/wikispecies/wikispecies_en_all_novid_2018-08.zim",
    enforcedLanguage = "en",
    displayName = "WikiSpecies"
  )
  private val wikivoyage = CustomApp(
    name = "wikivoyage",
    url = "http://download.kiwix.org/zim/wikivoyage/wikivoyage_en_all_novid_2018-10.zim",
    enforcedLanguage = "en",
    displayName = "Wikivoyage"
  )
  private val wikivoyageeurope = CustomApp(
    name = "wikivoyageeurope",
    url = "http://download.kiwix.org/zim/wikivoyage/wikivoyage_en_europe_novid_2018-10.zim",
    enforcedLanguage = "en",
    displayName = "Wikivoyage European Travels"
  )
  private val wikivoyagede = CustomApp(
    name = "wikivoyagede",
    url = "http://download.kiwix.org/zim/wikivoyage/wikivoyage_de_all_novid_2018-10.zim",
    enforcedLanguage = "de",
    displayName = "Wikivoyage auf Deutsch"
  )
  val all = listOf(
    example,
    phet,
    tunisie,
    venezuela,
    wikimed,
    wikimedar,
    wikimedde,
    wikimedes,
    wikimedfa,
    wikimedfr,
    wikimedja,
    wikimedmini,
    wikimedor,
    wikimedpt,
    wikimedzh,
    wikispecies,
    wikivoyage,
    wikivoyageeurope,
    wikivoyagede
  )

  fun createCustomAppFromJson(
    name: String,
    url: String,
    enforcedLanguage: String,
    displayName: String,
    versionName: String?
  ) = if (versionName == null) CustomApp(name, url, enforcedLanguage, displayName)
  else CustomApp(name, url, enforcedLanguage, displayName, versionName)

  data class CustomApp(
    val name: String,
    val url: String,
    val enforcedLanguage: String,
    val displayName: String,
    val versionName: String = parseVersionNameFromUrlOrUsePattern(url, "YYYY-MM")
  ) {
    val versionCode: Int = formatDate("YYDDD0").toInt()

    fun createFlavor(namedDomainObjectContainer: NamedDomainObjectContainer<ProductFlavor>) {
      namedDomainObjectContainer.create(this)
    }
  }

  private fun parseVersionNameFromUrlOrUsePattern(url: String, pattern: String) =
    url.substringAfterLast("_")
      .substringBeforeLast(".")
      .takeIf {
        try {
          SimpleDateFormat(pattern, Locale.ROOT).parse(it) != null
        } catch (parseException: ParseException) {
          false
        }
      }
      ?: formatDate(pattern)

  private fun formatDate(pattern: String) =
    Date().let(SimpleDateFormat(pattern, Locale.ROOT)::format)
}

fun NamedDomainObjectContainer<ProductFlavor>.create(customApps: List<CustomApp>) {
  customApps.forEach(this::create)
}

private fun NamedDomainObjectContainer<ProductFlavor>.create(
  customApp: CustomApp
) {
  create(customApp.name) {
    versionName = customApp.versionName
    versionCode = customApp.versionCode
    applicationIdSuffix = ".kiwixcustom${customApp.name}"
    buildConfigField("String", "ZIM_URL", "\"${customApp.url}\"")
    buildConfigField("String", "ENFORCED_LANG", "\"${customApp.enforcedLanguage}\"")
    configureStrings(customApp.displayName)
  }
}

fun ProductFlavor.configureStrings(appName: String) {
  resValue("string", "app_name", appName)
  resValue("string", "app_search_string", "Search $appName")
}
