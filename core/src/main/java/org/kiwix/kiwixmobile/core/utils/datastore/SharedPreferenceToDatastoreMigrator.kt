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
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

class SharedPreferenceToDatastoreMigrator(private val context: Context) {
  fun createMigration(): List<SharedPreferencesMigration<Preferences>> {
    val kiwixMobileMigration =
      SharedPreferencesMigration(
        context = context,
        sharedPreferencesName = SharedPreferenceUtil.PREF_KIWIX_MOBILE
      )
    val kiwixDefaultMigration = SharedPreferencesMigration(
      produceSharedPreferences = { PreferenceManager.getDefaultSharedPreferences(context) },
      keysToMigrate = setOf(
        SharedPreferenceUtil.Companion.TEXT_ZOOM,
        SharedPreferenceUtil.Companion.PREF_BACK_TO_TOP,
        SharedPreferenceUtil.Companion.PREF_NEW_TAB_BACKGROUND,
        SharedPreferenceUtil.Companion.PREF_EXTERNAL_LINK_POPUP,
        SharedPreferenceUtil.Companion.PREF_WIFI_ONLY,
        SharedPreferenceUtil.Companion.PREF_THEME,
        SharedPreferenceUtil.PREF_SHOW_INTRO,
        SharedPreferenceUtil.PREF_SHOW_SHOWCASE,
        SharedPreferenceUtil.PREF_BOOKMARKS_MIGRATED,
        SharedPreferenceUtil.PREF_RECENT_SEARCH_MIGRATED,
        SharedPreferenceUtil.PREF_NOTES_MIGRATED,
        SharedPreferenceUtil.PREF_HISTORY_MIGRATED,
        SharedPreferenceUtil.PREF_APP_DIRECTORY_TO_PUBLIC_MIGRATED,
        SharedPreferenceUtil.PREF_BOOK_ON_DISK_MIGRATED,
        SharedPreferenceUtil.CACHED_LANGUAGE_CODES,
        SharedPreferenceUtil.SELECTED_ONLINE_CONTENT_LANGUAGE,
        SharedPreferenceUtil.PREF_DEVICE_DEFAULT_LANG,
        SharedPreferenceUtil.PREF_LANG,
        SharedPreferenceUtil.PREF_SHOW_HISTORY_ALL_BOOKS,
        SharedPreferenceUtil.PREF_SHOW_BOOKMARKS_ALL_BOOKS,
        SharedPreferenceUtil.PREF_SHOW_NOTES_ALL_BOOKS,
        SharedPreferenceUtil.PREF_HOSTED_BOOKS,
        SharedPreferenceUtil.PREF_LATER_CLICKED_MILLIS,
        SharedPreferenceUtil.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS,
      )
    )
    return listOf(kiwixMobileMigration, kiwixDefaultMigration)
  }
}
