/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package branded

import com.android.build.gradle.internal.dsl.ProductFlavor
import org.gradle.api.NamedDomainObjectContainer
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File

typealias ProductFlavors = NamedDomainObjectContainer<out ProductFlavor>

object BrandedApps {

  fun createDynamically(srcFolder: File, productFlavors: ProductFlavors) {
    productFlavors.create(brandedApps(srcFolder))
  }

  private fun brandedApps(srcFolder: File) = srcFolder.walk()
    .filter { it.name == "info.json" }
    .map { BrandedApp(it.parentFile.name, JSONParser().parse(it.readText()) as JSONObject) }
    .toList()
}

fun ProductFlavors.create(brandedApp: List<BrandedApp>) {
  brandedApp.forEach { brandedApp ->
    this.create(brandedApp.name) {
      versionName = brandedApp.versionName
      versionCode = brandedApp.versionCode
      applicationIdSuffix = ".kiwixcustom${brandedApp.name}"
      buildConfigField("String", "ZIM_URL", "\"${brandedApp.url}\"")
      buildConfigField("String", "ENFORCED_LANG", "\"${brandedApp.enforcedLanguage}\"")
      buildConfigField("String", "ABOUT_APP_URL", "\"${brandedApp.aboutAppUrl}\"")
      buildConfigField("String", "SUPPORT_URL", "\"${brandedApp.supportUrl}\"")
      buildConfigField(
        "Boolean",
        "SHOW_SEARCH_SUGGESTIONS_SPELLCHECKED", "${brandedApp.showSearchSuggestionsSpellChecked}"
      )
      buildConfigField("Boolean", "DISABLE_SIDEBAR", "${brandedApp.disableSideBar}")
      buildConfigField("Boolean", "DISABLE_TABS", "${brandedApp.disableTabs}")
      buildConfigField("Boolean", "DISABLE_READ_ALOUD", "${brandedApp.disableReadAloud}")
      buildConfigField("Boolean", "DISABLE_TITLE", "${brandedApp.disableTitle}")
      buildConfigField("Boolean", "DISABLE_EXTERNAL_LINK", "${brandedApp.disableExternalLinks}")
      buildConfigField("Boolean", "DISABLE_HELP_MENU", "${brandedApp.disableHelpMenu}")
      configureStrings(brandedApp.displayName)
    }
  }
}

fun <T> JSONObject.getAndCast(columnName: String): T =
  getOrDefault(columnName, null) as T

fun ProductFlavor.configureStrings(appName: String) {
  resValue("string", "app_name", appName)
  resValue("string", "app_search_string", "Search $appName")
}
