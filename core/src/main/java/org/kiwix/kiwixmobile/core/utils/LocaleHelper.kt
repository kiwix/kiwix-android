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

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import java.util.Locale

object LocaleHelper {
  @JvmStatic
  fun getAppLocale(context: Context, kiwixDataStore: KiwixDataStore): Locale =
    if (!AppCompatDelegate.getApplicationLocales().isEmpty) {
      AppCompatDelegate.getApplicationLocales()[0] ?: getSystemLocale(context)
    } else {
      val pref = try {
        runBlocking { kiwixDataStore.prefLanguage.first() }
      } catch (_: Exception) {
        ""
      }
      if (pref.isNotEmpty() && pref != Locale.ROOT.toString()) {
        Locale.forLanguageTag(pref)
      } else {
        getSystemLocale(context)
      }
    }

  private fun getSystemLocale(context: Context): Locale = try {
    context.resources.configuration.locales.get(0)
  } catch (_: Throwable) {
    Locale.getDefault()
  }

  @JvmStatic
  fun getLocalizedString(context: Context, kiwixDataStore: KiwixDataStore, resId: Int, vararg args: Any): String = try {
    val config = android.content.res.Configuration(context.resources.configuration)
    config.setLocale(getAppLocale(context, kiwixDataStore))
    val localizedContext = context.createConfigurationContext(config)
    if (args.isEmpty()) localizedContext.getString(resId) else localizedContext.getString(resId, *args)
  } catch (_: Throwable) {
    if (args.isEmpty()) context.getString(resId) else context.getString(resId, *args)
  }
}
