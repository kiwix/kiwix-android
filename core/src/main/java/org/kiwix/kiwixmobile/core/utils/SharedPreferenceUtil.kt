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
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.kiwix.kiwixmobile.core.ThemeConfig
import org.kiwix.kiwixmobile.core.ThemeConfig.Theme.Companion.from
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.core.zim_manager.Language
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for the Default Shared Preferences of the application.
 */

@Singleton
class SharedPreferenceUtil @Inject constructor(val context: Context) {
  private val sharedPreferences: SharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(context)
  private val _prefStorages = MutableStateFlow("")
  val prefStorages
    get() = _prefStorages.asStateFlow().onStart { emit(prefStorage) }
  private val _textZooms = MutableStateFlow(textZoom)
  val textZooms get() = _textZooms.asStateFlow()
  private val appThemes = MutableStateFlow(ThemeConfig.Theme.SYSTEM)
  private val _prefWifiOnlys = MutableStateFlow(true)
  val prefWifiOnlys
    get() = _prefWifiOnlys.onStart { emit(prefWifiOnly) }

  val prefWifiOnly: Boolean
    get() = sharedPreferences.getBoolean(PREF_WIFI_ONLY, true)

  private val _onlineContentLanguage = MutableStateFlow("")
  val onlineContentLanguage = _onlineContentLanguage.asStateFlow()

  private val _onlineContentCategory = MutableStateFlow("")
  val onlineContentCategory = _onlineContentCategory.asStateFlow()

  val prefIsFirstRun: Boolean
    get() = sharedPreferences.getBoolean(PREF_IS_FIRST_RUN, true)

  var prefIsTest: Boolean
    get() = sharedPreferences.getBoolean(PREF_IS_TEST, false)
    set(prefIsTest) {
      sharedPreferences.edit { putBoolean(PREF_IS_TEST, prefIsTest) }
    }

  var prefIsScanFileSystemTest: Boolean
    get() = sharedPreferences.getBoolean(PREF_IS_SCAN_FILE_SYSTEM_TEST, false)
    set(prefIsScanFileSystemTest) {
      sharedPreferences.edit { putBoolean(PREF_IS_SCAN_FILE_SYSTEM_TEST, prefIsScanFileSystemTest) }
    }

  val prefShowShowCaseToUser: Boolean
    get() = sharedPreferences.getBoolean(PREF_SHOW_SHOWCASE, true)

  var prefBackToTop: Boolean
    get() = sharedPreferences.getBoolean(PREF_BACK_TO_TOP, false)
    set(backToTop) {
      sharedPreferences.edit { putBoolean(PREF_BACK_TO_TOP, backToTop) }
    }

  var prefNewTabBackground: Boolean
    get() = sharedPreferences.getBoolean(PREF_NEW_TAB_BACKGROUND, false)
    set(newTabInBackground) {
      sharedPreferences.edit { putBoolean(PREF_NEW_TAB_BACKGROUND, newTabInBackground) }
    }

  val prefExternalLinkPopup: Boolean
    get() = sharedPreferences.getBoolean(PREF_EXTERNAL_LINK_POPUP, true)

  val isPlayStoreBuild: Boolean
    get() = sharedPreferences.getBoolean(IS_PLAY_STORE_BUILD, false)

  val prefLanguage: String
    get() = sharedPreferences.getString(PREF_LANG, "") ?: Locale.ROOT.toString()

  val prefDeviceDefaultLanguage: String
    get() = sharedPreferences.getString(PREF_DEVICE_DEFAULT_LANG, "").orEmpty()

  val prefIsBookmarksMigrated: Boolean
    get() = sharedPreferences.getBoolean(PREF_BOOKMARKS_MIGRATED, false)

  val prefIsRecentSearchMigrated: Boolean
    get() = sharedPreferences.getBoolean(PREF_RECENT_SEARCH_MIGRATED, false)

  val prefIsNotesMigrated: Boolean
    get() = sharedPreferences.getBoolean(PREF_NOTES_MIGRATED, false)

  val prefIsHistoryMigrated: Boolean
    get() = sharedPreferences.getBoolean(PREF_HISTORY_MIGRATED, false)

  val prefIsAppDirectoryMigrated: Boolean
    get() = sharedPreferences.getBoolean(PREF_APP_DIRECTORY_TO_PUBLIC_MIGRATED, false)

  val prefIsBookOnDiskMigrated: Boolean
    get() = sharedPreferences.getBoolean(PREF_BOOK_ON_DISK_MIGRATED, false)

  var prefIsScanFileSystemDialogShown: Boolean
    get() = sharedPreferences.getBoolean(PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN, false)
    set(isFileScanFileSystemDialogShown) {
      sharedPreferences.edit {
        putBoolean(PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN, isFileScanFileSystemDialogShown)
      }
    }

  val prefStorage: String
    get() {
      val storage = sharedPreferences.getString(PREF_STORAGE, null)
      return when {
        storage == null ->
          getPublicDirectoryPath(defaultPublicStorage()).also {
            putPrefStorage(it)
            putStoragePosition(0)
          }

        runBlocking { !File(storage).isFileExist() } ->
          getPublicDirectoryPath(defaultPublicStorage()).also {
            putStoragePosition(0)
          }

        else -> storage
      }
    }

  val storagePosition: Int
    get() = sharedPreferences.getInt(STORAGE_POSITION, 0)

  fun defaultStorage(): String =
    context.getExternalFilesDirs(null)[0]?.path
      ?: context.filesDir.path // a workaround for emulators

  fun defaultPublicStorage(): String =
    ContextWrapper(context).externalMediaDirs[0]?.path
      ?: context.filesDir.path // a workaround for emulators

  fun showCaseViewForFileTransferShown() {
    sharedPreferences.edit { putBoolean(PREF_SHOW_SHOWCASE, false) }
  }

  fun putPrefBookMarkMigrated(isMigrated: Boolean) =
    sharedPreferences.edit { putBoolean(PREF_BOOKMARKS_MIGRATED, isMigrated) }

  fun putPrefRecentSearchMigrated(isMigrated: Boolean) =
    sharedPreferences.edit { putBoolean(PREF_RECENT_SEARCH_MIGRATED, isMigrated) }

  fun putPrefHistoryMigrated(isMigrated: Boolean) =
    sharedPreferences.edit { putBoolean(PREF_HISTORY_MIGRATED, isMigrated) }

  fun putPrefNotesMigrated(isMigrated: Boolean) =
    sharedPreferences.edit { putBoolean(PREF_NOTES_MIGRATED, isMigrated) }

  fun putPrefAppDirectoryMigrated(isMigrated: Boolean) =
    sharedPreferences.edit { putBoolean(PREF_APP_DIRECTORY_TO_PUBLIC_MIGRATED, isMigrated) }

  fun putPrefBookOnDiskMigrated(isMigrated: Boolean) =
    sharedPreferences.edit { putBoolean(PREF_BOOK_ON_DISK_MIGRATED, isMigrated) }

  fun putPrefLanguage(language: String) =
    sharedPreferences.edit { putString(PREF_LANG, language) }

  fun putPrefDeviceDefaultLanguage(language: String) =
    sharedPreferences.edit { putString(PREF_DEVICE_DEFAULT_LANG, language) }

  fun putPrefIsFirstRun(isFirstRun: Boolean) =
    sharedPreferences.edit { putBoolean(PREF_IS_FIRST_RUN, isFirstRun) }

  fun putPrefWifiOnly(wifiOnly: Boolean) {
    sharedPreferences.edit { putBoolean(PREF_WIFI_ONLY, wifiOnly) }
    _prefWifiOnlys.tryEmit(wifiOnly)
  }

  fun putPrefStorage(storage: String) {
    sharedPreferences.edit { putString(PREF_STORAGE, storage) }
    _prefStorages.tryEmit(storage)
  }

  fun putStoragePosition(pos: Int) {
    sharedPreferences.edit { putInt(STORAGE_POSITION, pos) }
  }

  fun setIsPlayStoreBuildType(isPlayStoreBuildType: Boolean) {
    sharedPreferences.edit { putBoolean(IS_PLAY_STORE_BUILD, isPlayStoreBuildType) }
  }

  fun putPrefExternalLinkPopup(externalLinkPopup: Boolean) =
    sharedPreferences.edit { putBoolean(PREF_EXTERNAL_LINK_POPUP, externalLinkPopup) }

  fun saveLanguageList(languages: List<Language>) {
    runCatching {
      val jsonArray = JSONArray()
      languages.forEach { lang ->
        val obj = JSONObject().apply {
          put(KEY_LANGUAGE_CODE, lang.languageCode)
          put(KEY_OCCURRENCES_OF_LANGUAGE, lang.occurencesOfLanguage)
          put(KEY_LANGUAGE_ACTIVE, lang.active)
          put(KEY_LANGUAGE_ID, lang.id)
        }
        jsonArray.put(obj)
      }
      sharedPreferences.edit {
        putString(CACHED_LANGUAGE_CODES, jsonArray.toString())
      }
    }.onFailure { it.printStackTrace() }
  }

  fun getCachedLanguageList(): List<Language>? =
    runCatching {
      val jsonString =
        sharedPreferences.getString(CACHED_LANGUAGE_CODES, null) ?: return@runCatching null
      val jsonArray = JSONArray(jsonString)
      List(jsonArray.length()) { i ->
        val obj = jsonArray.getJSONObject(i)
        Language(
          languageCode = obj.getString(KEY_LANGUAGE_CODE),
          occurrencesOfLanguage = obj.getInt(KEY_OCCURRENCES_OF_LANGUAGE),
          active = selectedOnlineContentLanguage == obj.getString(KEY_LANGUAGE_CODE),
          id = obj.getLong(KEY_LANGUAGE_ID)
        )
      }
    }.onFailure { it.printStackTrace() }.getOrNull()

  fun saveCategoryList(categories: List<Category>) {
    runCatching {
      val jsonArray = JSONArray()
      categories.forEach { category ->
        val obj = JSONObject().apply {
          put(KEY_ONLINE_CATEGORY_NAME, category.category)
          put(KEY_ONLINE_CATEGORY_ID, category.id)
        }
        jsonArray.put(obj)
      }
      sharedPreferences.edit {
        putString(CACHED_ONLINE_CATEGORIES, jsonArray.toString())
      }
    }.onFailure { it.printStackTrace() }
  }

  fun getCachedCategoryList(): List<Category>? =
    runCatching {
      val jsonString =
        sharedPreferences.getString(CACHED_ONLINE_CATEGORIES, null) ?: return@runCatching null
      val jsonArray = JSONArray(jsonString)
      List(jsonArray.length()) { i ->
        val obj = jsonArray.getJSONObject(i)
        Category(
          category = obj.getString(KEY_ONLINE_CATEGORY_NAME),
          active = selectedOnlineContentCategory == obj.getString(KEY_ONLINE_CATEGORY_NAME),
          id = obj.getLong(KEY_ONLINE_CATEGORY_ID)
        )
      }
    }.onFailure { it.printStackTrace() }.getOrNull()

  fun showIntro(): Boolean = sharedPreferences.getBoolean(PREF_SHOW_INTRO, true)

  fun setIntroShown() = sharedPreferences.edit { putBoolean(PREF_SHOW_INTRO, false) }

  var showHistoryAllBooks: Boolean
    get() = sharedPreferences.getBoolean(PREF_SHOW_HISTORY_ALL_BOOKS, true)
    set(prefShowHistoryAllBooks) {
      sharedPreferences.edit { putBoolean(PREF_SHOW_HISTORY_ALL_BOOKS, prefShowHistoryAllBooks) }
    }

  var showBookmarksAllBooks: Boolean
    get() = sharedPreferences.getBoolean(PREF_SHOW_BOOKMARKS_ALL_BOOKS, true)
    set(prefShowBookmarksFromCurrentBook) =
      sharedPreferences.edit {
        putBoolean(PREF_SHOW_BOOKMARKS_ALL_BOOKS, prefShowBookmarksFromCurrentBook)
      }

  var showStorageOption: Boolean
    get() = sharedPreferences.getBoolean(PREF_SHOW_STORAGE_OPTION, true)
    set(prefShowStorageOption) =
      sharedPreferences.edit {
        putBoolean(PREF_SHOW_STORAGE_OPTION, prefShowStorageOption)
      }

  var showNotesAllBooks: Boolean
    get() = sharedPreferences.getBoolean(PREF_SHOW_NOTES_ALL_BOOKS, true)
    set(prefShowBookmarksFromCurrentBook) =
      sharedPreferences.edit {
        putBoolean(PREF_SHOW_NOTES_ALL_BOOKS, prefShowBookmarksFromCurrentBook)
      }

  val appTheme: ThemeConfig.Theme
    get() =
      from(
        sharedPreferences.getString(PREF_THEME, null)?.toInt()
          ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      )

  fun appThemes(): Flow<ThemeConfig.Theme> = appThemes.onStart { emit(appTheme) }

  fun updateAppTheme(selectedTheme: String) {
    sharedPreferences.edit { putString(PREF_THEME, selectedTheme) }
    appThemes.tryEmit(appTheme)
  }

  var manageExternalFilesPermissionDialog: Boolean
    get() = sharedPreferences.getBoolean(PREF_MANAGE_EXTERNAL_FILES, true)
    set(prefManageExternalFilesPermissionDialog) =
      sharedPreferences.edit {
        putBoolean(PREF_MANAGE_EXTERNAL_FILES, prefManageExternalFilesPermissionDialog)
      }

  // this is only used for test cases
  @VisibleForTesting
  var manageExternalFilesPermissionDialogOnRefresh: Boolean
    get() = sharedPreferences.getBoolean(PREF_SHOW_MANAGE_PERMISSION_DIALOG_ON_REFRESH, true)
    set(manageExternalFilesPermissionDialogOnRefresh) =
      sharedPreferences.edit {
        putBoolean(
          PREF_SHOW_MANAGE_PERMISSION_DIALOG_ON_REFRESH,
          manageExternalFilesPermissionDialogOnRefresh
        )
      }

  var hostedBooks: Set<String>
    get() = sharedPreferences.getStringSet(PREF_HOSTED_BOOKS, null)?.toHashSet() ?: HashSet()
    set(hostedBooks) {
      sharedPreferences.edit { putStringSet(PREF_HOSTED_BOOKS, hostedBooks) }
    }

  var textZoom: Int
    get() = sharedPreferences.getInt(TEXT_ZOOM, DEFAULT_ZOOM)
    set(textZoom) {
      sharedPreferences.edit { putInt(TEXT_ZOOM, textZoom) }
      _textZooms.tryEmit(textZoom)
    }

  var shouldShowStorageSelectionDialog: Boolean
    get() = sharedPreferences.getBoolean(PREF_SHOW_COPY_MOVE_STORAGE_SELECTION_DIALOG, true)
    set(value) {
      sharedPreferences.edit {
        putBoolean(PREF_SHOW_COPY_MOVE_STORAGE_SELECTION_DIALOG, value)
      }
    }

  var lastDonationPopupShownInMilliSeconds: Long
    get() = sharedPreferences.getLong(PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS, 0L)
    set(value) {
      sharedPreferences.edit {
        putLong(PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS, value)
      }
    }

  var laterClickedMilliSeconds: Long
    get() = sharedPreferences.getLong(PREF_LATER_CLICKED_MILLIS, 0L)
    set(value) {
      sharedPreferences.edit {
        putLong(PREF_LATER_CLICKED_MILLIS, value)
      }
    }

  var selectedOnlineContentLanguage: String
    get() = sharedPreferences.getString(SELECTED_ONLINE_CONTENT_LANGUAGE, "").orEmpty()
    set(selectedOnlineContentLanguage) {
      sharedPreferences.edit {
        putString(
          SELECTED_ONLINE_CONTENT_LANGUAGE,
          selectedOnlineContentLanguage
        )
      }
      _onlineContentLanguage.tryEmit(selectedOnlineContentLanguage)
    }

  var selectedOnlineContentCategory: String
    get() = sharedPreferences.getString(SELECTED_ONLINE_CONTENT_CATEGORY, "").orEmpty()
    set(selectedOnlineContentCategory) {
      sharedPreferences.edit {
        putString(
          SELECTED_ONLINE_CONTENT_CATEGORY,
          selectedOnlineContentCategory
        )
      }
      _onlineContentCategory.tryEmit(selectedOnlineContentCategory)
    }

  fun getPublicDirectoryPath(path: String): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      path
    } else {
      path.substringBefore(context.getString(R.string.android_directory_seperator))
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
    private const val PREF_BACK_TO_TOP = "pref_backtotop"
    private const val PREF_NEW_TAB_BACKGROUND = "pref_newtab_background"
    const val PREF_EXTERNAL_LINK_POPUP = "pref_external_link_popup"
    const val PREF_SHOW_STORAGE_OPTION = "show_storgae_option"
    const val PREF_IS_FIRST_RUN = "isFirstRun"
    private const val PREF_SHOW_BOOKMARKS_ALL_BOOKS = "show_bookmarks_current_book"
    private const val PREF_SHOW_HISTORY_ALL_BOOKS = "show_history_current_book"
    private const val PREF_SHOW_NOTES_ALL_BOOKS = "show_notes_current_book"
    private const val PREF_HOSTED_BOOKS = "hosted_books"
    const val PREF_THEME = "pref_dark_mode"
    private const val TEXT_ZOOM = "true_text_zoom"
    private const val DEFAULT_ZOOM = 100
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
    private const val PREF_LATER_CLICKED_MILLIS = "pref_later_clicked_millis"
    const val PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS =
      "pref_last_donation_shown_in_milliseconds"
    private const val SELECTED_ONLINE_CONTENT_LANGUAGE = "selectedOnlineContentLanguage"
    private const val CACHED_LANGUAGE_CODES = "cachedLanguageCodes"
    private const val KEY_LANGUAGE_CODE = "languageCode"
    private const val KEY_OCCURRENCES_OF_LANGUAGE = "occurrencesOfLanguage"
    private const val KEY_LANGUAGE_ACTIVE = "languageActive"
    private const val KEY_LANGUAGE_ID = "languageId"
    const val PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN = "prefScanFileSystemDialogShown"
    const val PREF_IS_SCAN_FILE_SYSTEM_TEST = "prefIsScanFileSystemTest"
    private const val SELECTED_ONLINE_CONTENT_CATEGORY = "selectedOnlineContentCategory"
    private const val CACHED_ONLINE_CATEGORIES = "cachedOnlineCategories"
    private const val KEY_ONLINE_CATEGORY_NAME = "onlineCategoryName"
    private const val KEY_ONLINE_CATEGORY_ID = "onlineCategoryId"
  }
}
