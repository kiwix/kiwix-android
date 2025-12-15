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
package org.kiwix.kiwixmobile.core.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for the Default Shared Preferences of the application.
 */

@Singleton
class SharedPreferenceUtil @Inject constructor(val context: Context) {
  private val sharedPreferences: SharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(context)

  var prefIsTest: Boolean
    get() = sharedPreferences.getBoolean(PREF_IS_TEST, false)
    set(prefIsTest) {
      sharedPreferences.edit { putBoolean(PREF_IS_TEST, prefIsTest) }
    }

  val isPlayStoreBuild: Boolean
    get() = sharedPreferences.getBoolean(IS_PLAY_STORE_BUILD, false)

  fun setIsPlayStoreBuildType(isPlayStoreBuildType: Boolean) {
    sharedPreferences.edit { putBoolean(IS_PLAY_STORE_BUILD, isPlayStoreBuildType) }
  }

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
  fun isPlayStoreBuildWithAndroid11OrAbove(): Boolean =
    isPlayStoreBuild && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

  @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
  fun isNotPlayStoreBuildWithAndroid11OrAbove(): Boolean =
    !isPlayStoreBuild && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

  companion object {
    // Prefs
    const val PREF_LANG = "pref_language_chooser"
    const val PREF_DEVICE_DEFAULT_LANG = "pref_device_default_language"
    const val PREF_STORAGE = "pref_select_folder"
    const val STORAGE_POSITION = "storage_position"
    const val PREF_WIFI_ONLY = "pref_wifi_only"
    const val PREF_KIWIX_MOBILE = "kiwix-mobile"
    const val PREF_SHOW_INTRO = "showIntro"
    const val PREF_IS_TEST = "is_test"
    const val PREF_SHOW_SHOWCASE = "showShowCase"
    const val PREF_BACK_TO_TOP = "pref_backtotop"
    const val PREF_NEW_TAB_BACKGROUND = "pref_newtab_background"
    const val PREF_EXTERNAL_LINK_POPUP = "pref_external_link_popup"
    const val PREF_SHOW_STORAGE_OPTION = "show_storgae_option"
    const val PREF_IS_FIRST_RUN = "isFirstRun"
    const val PREF_SHOW_BOOKMARKS_ALL_BOOKS = "show_bookmarks_current_book"
    const val PREF_SHOW_HISTORY_ALL_BOOKS = "show_history_current_book"
    const val PREF_SHOW_NOTES_ALL_BOOKS = "show_notes_current_book"
    const val PREF_HOSTED_BOOKS = "hosted_books"
    const val PREF_THEME = "pref_dark_mode"
    const val TEXT_ZOOM = "true_text_zoom"
    const val DEFAULT_ZOOM = 100
    const val PREF_MANAGE_EXTERNAL_FILES = "pref_manage_external_files"
    const val PREF_SHOW_MANAGE_PERMISSION_DIALOG_ON_REFRESH = "pref_show_manage_external_files"
    const val IS_PLAY_STORE_BUILD = "is_play_store_build"
    const val PREF_BOOKMARKS_MIGRATED = "pref_bookmarks_migrated"
    const val PREF_RECENT_SEARCH_MIGRATED = "pref_recent_search_migrated"
    const val PREF_HISTORY_MIGRATED = "pref_history_migrated"
    const val PREF_NOTES_MIGRATED = "pref_notes_migrated"
    const val PREF_APP_DIRECTORY_TO_PUBLIC_MIGRATED = "pref_app_directory_to_public_migrated"
    const val PREF_BOOK_ON_DISK_MIGRATED = "pref_book_on_disk_migrated"
    const val PREF_SHOW_COPY_MOVE_STORAGE_SELECTION_DIALOG = "pref_show_copy_move_storage_dialog"
    const val PREF_LATER_CLICKED_MILLIS = "pref_later_clicked_millis"
    const val PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS =
      "pref_last_donation_shown_in_milliseconds"
    const val SELECTED_ONLINE_CONTENT_LANGUAGE = "selectedOnlineContentLanguage"
    const val CACHED_LANGUAGE_CODES = "cachedLanguageCodes"
    const val KEY_LANGUAGE_CODE = "languageCode"
    const val KEY_OCCURRENCES_OF_LANGUAGE = "occurrencesOfLanguage"
    const val KEY_LANGUAGE_ACTIVE = "languageActive"
    const val KEY_LANGUAGE_ID = "languageId"
    const val PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN = "prefScanFileSystemDialogShown"
    const val PREF_IS_SCAN_FILE_SYSTEM_TEST = "prefIsScanFileSystemTest"
  }
}
