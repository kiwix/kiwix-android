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
import org.gradle.api.NamedDomainObjectContainer
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File

typealias ProductFlavors = NamedDomainObjectContainer<ProductFlavor>

object CustomApps {

  fun createDynamically(srcFolder: File, productFlavors: ProductFlavors) {
    productFlavors.create(customApps(srcFolder))
  }

  private fun customApps(srcFolder: File) = srcFolder.walk()
    .filter { it.name == "info.json" }
    .map { CustomApp(it.parentFile.name, JSONParser().parse(it.readText()) as JSONObject) }
    .toList()
}

fun ProductFlavors.create(customApps: List<CustomApp>) {
  customApps.forEach { customApp ->
    this.create(customApp.name) {
      versionName = customApp.versionName
      versionCode = customApp.versionCode
      applicationIdSuffix = ".kiwixcustom${customApp.name}"
      buildConfigField("String", "ZIM_URL", "\"${customApp.url}\"")
      buildConfigField("String", "ENFORCED_LANG", "\"${customApp.enforcedLanguage}\"")
      buildConfigField("Boolean", "DISABLE_SIDEBAR", "${customApp.disableSideBar}")
      buildConfigField("Boolean", "DISABLE_TABS", "${customApp.disableTabs}")
      buildConfigField("Boolean", "DISABLE_READ_ALOUD", "${customApp.disableReadAloud}")
      buildConfigField("Boolean", "DISABLE_EXTERNAL_LINKS", "${customApp.disableExternalLinks}")
      configureStrings(customApp.displayName)
    }
  }
}

fun <T> JSONObject.getAndCast(columnName: String): T =
  getOrDefault(columnName, null) as T

fun ProductFlavor.configureStrings(appName: String) {
  resValue("string", "app_name", appName)
  resValue("string", "app_search_string", "Search $appName")
}
