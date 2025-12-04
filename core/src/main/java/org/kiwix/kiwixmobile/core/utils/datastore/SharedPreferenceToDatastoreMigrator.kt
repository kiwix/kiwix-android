/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.utils.datastore

import android.content.Context
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

class SharedPreferenceToDatastoreMigrator(private val context: Context) {
  fun createMigration(): List<SharedPreferencesMigration<Preferences>> {
    val kiwixMobileMigration =
      SharedPreferencesMigration(
        context = context,
        sharedPreferencesName = SharedPreferenceUtil.PREF_KIWIX_MOBILE
      )
    // SharedPreferencesMigration(
    //   produceSharedPreferences = { PreferenceManager.getDefaultSharedPreferences(context) },
    //   keysToMigrate = setOf(
    //     SharedPreferenceUtil.Companion.TEXT_ZOOM,
    //   ),
    //   migrate = { prefs ->
    //     // currentPreferences.toMutablePreferences().apply {
    //     //   if (!contains(PreferencesKeys.TEXT_ZOOM)) {
    //     //     put(
    //     //       PreferencesKeys.TEXT_ZOOM,
    //     //       prefs.getInt(SharedPreferenceUtil.Companion.TEXT_ZOOM, DEFAULT_ZOOM)
    //     //     )
    //     //   }
    //     // }
    //   }
    // )
    return listOf(kiwixMobileMigration)
  }
}
