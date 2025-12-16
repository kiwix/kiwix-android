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
import androidx.preference.PreferenceManager

class SharedPreferenceToDatastoreMigrator(private val context: Context) {
  fun createMigration(): List<SharedPreferencesMigration<Preferences>> {
    val kiwixMobileMigration =
      SharedPreferencesMigration(
        context = context,
        sharedPreferencesName = KiwixDataStore.PREF_KIWIX_MOBILE
      )
    val kiwixDefaultMigration = SharedPreferencesMigration(
      produceSharedPreferences = { PreferenceManager.getDefaultSharedPreferences(context) },
      keysToMigrate = setOf(
        KiwixDataStore.TEXT_ZOOM,
        KiwixDataStore.PREF_BACK_TO_TOP,
        KiwixDataStore.PREF_NEW_TAB_BACKGROUND,
        KiwixDataStore.PREF_EXTERNAL_LINK_POPUP,
        KiwixDataStore.PREF_WIFI_ONLY,
        KiwixDataStore.PREF_THEME,
        KiwixDataStore.PREF_SHOW_INTRO,
        KiwixDataStore.PREF_SHOW_SHOWCASE,
        KiwixDataStore.PREF_BOOKMARKS_MIGRATED,
        KiwixDataStore.PREF_RECENT_SEARCH_MIGRATED,
        KiwixDataStore.PREF_NOTES_MIGRATED,
        KiwixDataStore.PREF_HISTORY_MIGRATED,
        KiwixDataStore.PREF_APP_DIRECTORY_TO_PUBLIC_MIGRATED,
        KiwixDataStore.PREF_BOOK_ON_DISK_MIGRATED,
        KiwixDataStore.CACHED_LANGUAGE_CODES,
        KiwixDataStore.SELECTED_ONLINE_CONTENT_LANGUAGE,
        KiwixDataStore.PREF_DEVICE_DEFAULT_LANG,
        KiwixDataStore.PREF_LANG,
        KiwixDataStore.PREF_SHOW_HISTORY_ALL_BOOKS,
        KiwixDataStore.PREF_SHOW_BOOKMARKS_ALL_BOOKS,
        KiwixDataStore.PREF_SHOW_NOTES_ALL_BOOKS,
        KiwixDataStore.PREF_HOSTED_BOOKS,
        KiwixDataStore.PREF_LATER_CLICKED_MILLIS,
        KiwixDataStore.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS,
        KiwixDataStore.PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN,
        KiwixDataStore.PREF_MANAGE_EXTERNAL_FILES,
        KiwixDataStore.PREF_SHOW_STORAGE_OPTION,
        KiwixDataStore.PREF_SHOW_COPY_MOVE_STORAGE_SELECTION_DIALOG,
        KiwixDataStore.PREF_STORAGE,
        KiwixDataStore.STORAGE_POSITION,
        KiwixDataStore.PREF_IS_FIRST_RUN,
        KiwixDataStore.IS_PLAY_STORE_BUILD,
        KiwixDataStore.PREF_IS_TEST,
      )
    )
    return listOf(kiwixMobileMigration, kiwixDefaultMigration)
  }
}
