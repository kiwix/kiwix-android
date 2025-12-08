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

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

object PreferencesKeys {
  val TEXT_ZOOM = intPreferencesKey(SharedPreferenceUtil.TEXT_ZOOM)
  val TAG_CURRENT_FILE = stringPreferencesKey(org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_FILE)
  val TAG_CURRENT_TAB = intPreferencesKey(org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_TAB)
  val PREF_BACK_TO_TOP = booleanPreferencesKey(SharedPreferenceUtil.PREF_BACK_TO_TOP)
  val PREF_NEW_TAB_BACKGROUND = booleanPreferencesKey(SharedPreferenceUtil.PREF_NEW_TAB_BACKGROUND)
  val PREF_EXTERNAL_LINK_POPUP =
    booleanPreferencesKey(SharedPreferenceUtil.PREF_EXTERNAL_LINK_POPUP)
  val PREF_WIFI_ONLY = booleanPreferencesKey(SharedPreferenceUtil.PREF_WIFI_ONLY)
  val PREF_THEME = stringPreferencesKey(SharedPreferenceUtil.PREF_THEME)
  val PREF_SHOW_INTRO = booleanPreferencesKey(SharedPreferenceUtil.PREF_SHOW_INTRO)
  val PREF_SHOW_SHOWCASE = booleanPreferencesKey(SharedPreferenceUtil.PREF_SHOW_SHOWCASE)
  val PREF_BOOKMARKS_MIGRATED = booleanPreferencesKey(SharedPreferenceUtil.PREF_BOOKMARKS_MIGRATED)
  val PREF_RECENT_SEARCH_MIGRATED =
    booleanPreferencesKey(SharedPreferenceUtil.PREF_RECENT_SEARCH_MIGRATED)
  val PREF_NOTES_MIGRATED = booleanPreferencesKey(SharedPreferenceUtil.PREF_NOTES_MIGRATED)
  val PREF_HISTORY_MIGRATED = booleanPreferencesKey(SharedPreferenceUtil.PREF_HISTORY_MIGRATED)
  val PREF_APP_DIRECTORY_TO_PUBLIC_MIGRATED =
    booleanPreferencesKey(SharedPreferenceUtil.PREF_APP_DIRECTORY_TO_PUBLIC_MIGRATED)
  val PREF_BOOK_ON_DISK_MIGRATED =
    booleanPreferencesKey(SharedPreferenceUtil.PREF_BOOK_ON_DISK_MIGRATED)
  val CACHED_LANGUAGE_CODES = stringPreferencesKey(SharedPreferenceUtil.CACHED_LANGUAGE_CODES)
  val SELECTED_ONLINE_CONTENT_LANGUAGE =
    stringPreferencesKey(SharedPreferenceUtil.SELECTED_ONLINE_CONTENT_LANGUAGE)
  val PREF_DEVICE_DEFAULT_LANG = stringPreferencesKey(SharedPreferenceUtil.PREF_DEVICE_DEFAULT_LANG)
  val PREF_LANG = stringPreferencesKey(SharedPreferenceUtil.PREF_LANG)
}
