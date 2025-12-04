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
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.DEFAULT_ZOOM
import javax.inject.Inject
import javax.inject.Singleton

private const val KIWIX_DATASTORE_NAME = "kiwix_datastore_preferences"

val Context.kiwixDataStore by preferencesDataStore(
  name = KIWIX_DATASTORE_NAME,
  produceMigrations = { context ->
    SharedPreferenceToDatastoreMigrator(context).createMigration()
  }
)

@Singleton
class KiwixDataStore @Inject constructor(val context: Context) {
  val textZoom: Flow<Int> = context.kiwixDataStore.data.map { prefs ->
    prefs[PreferencesKeys.TEXT_ZOOM] ?: DEFAULT_ZOOM
  }

  suspend fun setTextZoom(value: Int) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.TEXT_ZOOM] = value
    }
  }

  val currentZimFile: Flow<String?> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.TAG_CURRENT_FILE]
    }

  suspend fun setCurrentZimFile(currentZimFilePath: String) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.TAG_CURRENT_FILE] = currentZimFilePath
    }
  }

  val currentTab: Flow<Int?> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.TAG_CURRENT_TAB]
    }

  suspend fun setCurrentTab(currentTab: Int) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.TAG_CURRENT_TAB] = currentTab
    }
  }

  val backToTop: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_BACK_TO_TOP] ?: false
    }

  suspend fun setPrefBackToTop(backToTop: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_BACK_TO_TOP] = backToTop
    }
  }

  val openNewTabInBackground: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_NEW_TAB_BACKGROUND] ?: false
    }

  suspend fun setOpenNewInBackground(openInNewTab: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_NEW_TAB_BACKGROUND] = openInNewTab
    }
  }

  val externalLinkPopup: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_EXTERNAL_LINK_POPUP] ?: true
    }

  suspend fun setExternalLinkPopup(externalLinkPopup: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_EXTERNAL_LINK_POPUP] = externalLinkPopup
    }
  }

  val wifiOnly: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_WIFI_ONLY] ?: true
    }

  suspend fun setWifiOnly(wifiOnly: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_WIFI_ONLY] = wifiOnly
    }
  }
}
