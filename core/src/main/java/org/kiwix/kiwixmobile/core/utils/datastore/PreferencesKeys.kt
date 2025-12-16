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
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object PreferencesKeys {
  val TEXT_ZOOM = intPreferencesKey(KiwixDataStore.TEXT_ZOOM)
  val TAG_CURRENT_FILE = stringPreferencesKey(org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_FILE)
  val TAG_CURRENT_TAB = intPreferencesKey(org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_TAB)
  val PREF_BACK_TO_TOP = booleanPreferencesKey(KiwixDataStore.PREF_BACK_TO_TOP)
  val PREF_NEW_TAB_BACKGROUND = booleanPreferencesKey(KiwixDataStore.PREF_NEW_TAB_BACKGROUND)
  val PREF_EXTERNAL_LINK_POPUP =
    booleanPreferencesKey(KiwixDataStore.PREF_EXTERNAL_LINK_POPUP)
  val PREF_WIFI_ONLY = booleanPreferencesKey(KiwixDataStore.PREF_WIFI_ONLY)
  val PREF_THEME = stringPreferencesKey(KiwixDataStore.PREF_THEME)
  val PREF_SHOW_INTRO = booleanPreferencesKey(KiwixDataStore.PREF_SHOW_INTRO)
  val PREF_SHOW_SHOWCASE = booleanPreferencesKey(KiwixDataStore.PREF_SHOW_SHOWCASE)
  val PREF_BOOKMARKS_MIGRATED = booleanPreferencesKey(KiwixDataStore.PREF_BOOKMARKS_MIGRATED)
  val PREF_RECENT_SEARCH_MIGRATED =
    booleanPreferencesKey(KiwixDataStore.PREF_RECENT_SEARCH_MIGRATED)
  val PREF_NOTES_MIGRATED = booleanPreferencesKey(KiwixDataStore.PREF_NOTES_MIGRATED)
  val PREF_HISTORY_MIGRATED = booleanPreferencesKey(KiwixDataStore.PREF_HISTORY_MIGRATED)
  val PREF_APP_DIRECTORY_TO_PUBLIC_MIGRATED =
    booleanPreferencesKey(KiwixDataStore.PREF_APP_DIRECTORY_TO_PUBLIC_MIGRATED)
  val PREF_BOOK_ON_DISK_MIGRATED =
    booleanPreferencesKey(KiwixDataStore.PREF_BOOK_ON_DISK_MIGRATED)
  val CACHED_LANGUAGE_CODES = stringPreferencesKey(KiwixDataStore.CACHED_LANGUAGE_CODES)
  val SELECTED_ONLINE_CONTENT_LANGUAGE =
    stringPreferencesKey(KiwixDataStore.SELECTED_ONLINE_CONTENT_LANGUAGE)
  val PREF_DEVICE_DEFAULT_LANG = stringPreferencesKey(KiwixDataStore.PREF_DEVICE_DEFAULT_LANG)
  val PREF_LANG = stringPreferencesKey(KiwixDataStore.PREF_LANG)
  val PREF_SHOW_HISTORY_ALL_BOOKS =
    booleanPreferencesKey(KiwixDataStore.PREF_SHOW_HISTORY_ALL_BOOKS)
  val PREF_SHOW_BOOKMARKS_ALL_BOOKS =
    booleanPreferencesKey(KiwixDataStore.PREF_SHOW_BOOKMARKS_ALL_BOOKS)
  val PREF_SHOW_NOTES_ALL_BOOKS =
    booleanPreferencesKey(KiwixDataStore.PREF_SHOW_NOTES_ALL_BOOKS)
  val PREF_HOSTED_BOOKS = stringSetPreferencesKey(KiwixDataStore.PREF_HOSTED_BOOKS)
  val PREF_LATER_CLICKED_MILLIS = longPreferencesKey(KiwixDataStore.PREF_LATER_CLICKED_MILLIS)
  val PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS =
    longPreferencesKey(KiwixDataStore.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS)
  val PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN =
    booleanPreferencesKey(KiwixDataStore.PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN)
  val PREF_IS_SCAN_FILE_SYSTEM_TEST =
    booleanPreferencesKey(KiwixDataStore.PREF_IS_SCAN_FILE_SYSTEM_TEST)
  val PREF_MANAGE_EXTERNAL_FILES =
    booleanPreferencesKey(KiwixDataStore.PREF_MANAGE_EXTERNAL_FILES)
  val PREF_SHOW_MANAGE_PERMISSION_DIALOG_ON_REFRESH =
    booleanPreferencesKey(KiwixDataStore.PREF_SHOW_MANAGE_PERMISSION_DIALOG_ON_REFRESH)
  val PREF_SHOW_STORAGE_OPTION =
    booleanPreferencesKey(KiwixDataStore.PREF_SHOW_STORAGE_OPTION)
  val PREF_SHOW_COPY_MOVE_STORAGE_SELECTION_DIALOG =
    booleanPreferencesKey(KiwixDataStore.PREF_SHOW_COPY_MOVE_STORAGE_SELECTION_DIALOG)
  val PREF_STORAGE = stringPreferencesKey(KiwixDataStore.PREF_STORAGE)
  val STORAGE_POSITION = intPreferencesKey(KiwixDataStore.STORAGE_POSITION)
  val PREF_IS_FIRST_RUN = booleanPreferencesKey(KiwixDataStore.PREF_IS_FIRST_RUN)
  val IS_PLAY_STORE_BUILD = booleanPreferencesKey(KiwixDataStore.IS_PLAY_STORE_BUILD)
  val PREF_IS_TEST = booleanPreferencesKey(KiwixDataStore.PREF_IS_TEST)
}
