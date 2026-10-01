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

package org.kiwix.kiwixmobile.core.utils

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.first
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import java.util.Locale

object LocaleHelper {
  @JvmStatic
  suspend fun getAppLocale(context: Context, kiwixDataStore: KiwixDataStore): Locale =
    if (!AppCompatDelegate.getApplicationLocales().isEmpty) {
      AppCompatDelegate.getApplicationLocales()[0] ?: getSystemLocale(context)
    } else {
      val pref = try {
        kiwixDataStore.prefLanguage.first()
      } catch (_: Exception) {
        ""
      }
      if (pref.isNotEmpty() && pref != Locale.ROOT.toString()) {
        Locale.forLanguageTag(pref)
      } else {
        getSystemLocale(context)
      }
    }

  fun getSystemLocale(context: Context): Locale {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      runCatching {
        context.getSystemService(LocaleManager::class.java)?.systemLocales?.takeIf { it.size() > 0 }
          ?.get(0)
      }.getOrNull()?.let { return@getSystemLocale it }
    }
    return firstLocaleOrNull(runCatching { Resources.getSystem()?.configuration }.getOrNull())
      ?: firstLocaleOrNull(runCatching { context.resources?.configuration }.getOrNull())
      ?: Locale.getDefault()
  }

  private fun firstLocaleOrNull(configuration: Configuration?): Locale? =
    configuration?.locales?.takeIf { it.size() > 0 }?.get(0)
}
