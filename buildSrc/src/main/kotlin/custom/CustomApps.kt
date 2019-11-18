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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CustomApps {
  val all = listOf(
    CustomApp(
      name = "customexample",
      versionName = "2017-07",
      url = "http://download.kiwix.org/zim/wikipedia_fr_test.zim",
      enforcedLanguage = "en",
      displayName = "Test Custom App"
    ),
    CustomApp(
      name = "wikimed",
      versionName = "2018-08",
      url = "http://download.kiwix.org/zim/wikipedia_en_medicine_novid.zim",
      enforcedLanguage = "en",
      displayName = "Medical Wikipedia"
    )
  )

  data class CustomApp(
    val name: String,
    val versionName: String,
    val versionCode: Int = Date().let {
      SimpleDateFormat("YYDDD0", Locale.ROOT).format(it).toInt()
    },
    val url: String,
    val enforcedLanguage: String,
    val displayName: String
  )
}
fun NamedDomainObjectContainer<ProductFlavor>.create(customApps: List<CustomApp>) {
  customApps.forEach { customFlavor ->
    create(customFlavor.name) {
      versionName = customFlavor.versionName
      versionCode = customFlavor.versionCode
      applicationIdSuffix = ".kiwixcustom${customFlavor.name}"
      buildConfigField("String", "ZIM_URL", "\"${customFlavor.url}\"")
      buildConfigField("String", "ENFORCED_LANG", "\"${customFlavor.enforcedLanguage}\"")
      configureStrings(customFlavor.displayName)
    }
  }
}

fun ProductFlavor.configureStrings(appName: String) {
  resValue("string", "app_name", appName)
  resValue("string", "app_search_string", "Search $appName")
}
