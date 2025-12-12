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
import android.content.ContextWrapper
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ThemeConfig
import org.kiwix.kiwixmobile.core.ThemeConfig.Theme.Companion.from
import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.DEFAULT_ZOOM
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.KEY_LANGUAGE_ACTIVE
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.KEY_LANGUAGE_CODE
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.KEY_LANGUAGE_ID
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.KEY_OCCURRENCES_OF_LANGUAGE
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.Companion.STORAGE_POSITION
import org.kiwix.kiwixmobile.core.zim_manager.Language
import java.io.File
import java.util.Locale
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

  val appTheme: Flow<ThemeConfig.Theme> =
    context.kiwixDataStore.data.map { prefs ->
      from(
        prefs[PreferencesKeys.PREF_THEME]?.toInt()
          ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
      )
    }

  suspend fun updateAppTheme(selectedTheme: String) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_THEME] = selectedTheme
    }
  }

  val showIntro: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_SHOW_INTRO] ?: true
    }

  /**
   * Marks the intro as shown. The parameter is mainly used for test cases.
   * By default, `false` is stored, indicating that the intro has already been shown.
   */
  suspend fun setIntroShown(isShown: Boolean = false) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_SHOW_INTRO] = isShown
    }
  }

  val showShowCaseToUser: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_SHOW_SHOWCASE] ?: true
    }

  /**
   * Marks the showCaseView as shown. The parameter is mainly used for test cases.
   * By default, `false` is stored, indicating that the showCaseView has already been shown.
   */
  suspend fun setShowCaseViewForFileTransferShown(isShown: Boolean = false) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_SHOW_SHOWCASE] = isShown
    }
  }

  val isBookmarksMigrated: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_BOOKMARKS_MIGRATED] ?: false
    }

  suspend fun setBookMarkMigrated(isMigrated: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_BOOKMARKS_MIGRATED] = isMigrated
    }
  }

  val isRecentSearchMigrated: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_RECENT_SEARCH_MIGRATED] ?: false
    }

  suspend fun setRecentSearchMigrated(isMigrated: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_RECENT_SEARCH_MIGRATED] = isMigrated
    }
  }

  val isNotesMigrated: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_NOTES_MIGRATED] ?: false
    }

  suspend fun setNotesMigrated(isMigrated: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_NOTES_MIGRATED] = isMigrated
    }
  }

  val isHistoryMigrated: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_HISTORY_MIGRATED] ?: false
    }

  suspend fun setHistoryMigrated(isMigrated: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_HISTORY_MIGRATED] = isMigrated
    }
  }

  val isAppDirectoryMigrated: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_APP_DIRECTORY_TO_PUBLIC_MIGRATED] ?: false
    }

  suspend fun setAppDirectoryMigrated(isMigrated: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_APP_DIRECTORY_TO_PUBLIC_MIGRATED] = isMigrated
    }
  }

  val isBookOnDiskMigrated: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_BOOK_ON_DISK_MIGRATED] ?: false
    }

  suspend fun setBookOnDiskMigrated(isMigrated: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_BOOK_ON_DISK_MIGRATED] = isMigrated
    }
  }

  val selectedOnlineContentLanguage: Flow<String> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.SELECTED_ONLINE_CONTENT_LANGUAGE].orEmpty()
    }

  suspend fun setSelectedOnlineContentLanguage(selectedOnlineContentLanguage: String) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.SELECTED_ONLINE_CONTENT_LANGUAGE] = selectedOnlineContentLanguage
    }
  }

  val cachedLanguageList: Flow<List<Language>?> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.CACHED_LANGUAGE_CODES]?.let { jsonString ->
        val jsonArray = JSONArray(jsonString)
        List(jsonArray.length()) { i ->
          val obj = jsonArray.getJSONObject(i)
          Language(
            languageCode = obj.getString(KEY_LANGUAGE_CODE),
            occurrencesOfLanguage = obj.getInt(KEY_OCCURRENCES_OF_LANGUAGE),
            active = selectedOnlineContentLanguage.first() == obj.getString(KEY_LANGUAGE_CODE),
            id = obj.getLong(KEY_LANGUAGE_ID)
          )
        }
      }
    }

  suspend fun saveLanguageList(languages: List<Language>) {
    val jsonArray = JSONArray().apply {
      languages.forEach { lang ->
        put(
          JSONObject().apply {
            put(KEY_LANGUAGE_CODE, lang.languageCode)
            put(KEY_OCCURRENCES_OF_LANGUAGE, lang.occurencesOfLanguage)
            put(KEY_LANGUAGE_ACTIVE, lang.active)
            put(KEY_LANGUAGE_ID, lang.id)
          }
        )
      }
    }

    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.CACHED_LANGUAGE_CODES] = jsonArray.toString()
    }
  }

  val deviceDefaultLanguage: Flow<String> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_DEVICE_DEFAULT_LANG].orEmpty()
    }

  suspend fun setDeviceDefaultLanguage(language: String) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_DEVICE_DEFAULT_LANG] = language
    }
  }

  val prefLanguage: Flow<String> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_LANG] ?: Locale.ROOT.toString()
    }

  suspend fun setPrefLanguage(language: String) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_LANG] = language
    }
  }

  val showHistoryOfAllBooks: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_SHOW_HISTORY_ALL_BOOKS] ?: true
    }

  suspend fun setShowHistoryOfAllBooks(showHistoryOfAllBooks: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_SHOW_HISTORY_ALL_BOOKS] = showHistoryOfAllBooks
    }
  }

  val showBookmarksOfAllBooks: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_SHOW_BOOKMARKS_ALL_BOOKS] ?: true
    }

  suspend fun setShowBookmarksOfAllBooks(showBookmarksAllBooks: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_SHOW_BOOKMARKS_ALL_BOOKS] = showBookmarksAllBooks
    }
  }

  val showNotesOfAllBooks: Flow<Boolean> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_SHOW_NOTES_ALL_BOOKS] ?: true
    }

  suspend fun setShowNotesOfAllBooks(showNotesAllBooks: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_SHOW_NOTES_ALL_BOOKS] = showNotesAllBooks
    }
  }

  val hostedBooks: Flow<Set<String>> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_HOSTED_BOOKS] ?: HashSet()
    }

  suspend fun setHostedBooks(hostedBooks: Set<String>) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_HOSTED_BOOKS] = hostedBooks
    }
  }

  val laterClickedMilliSeconds: Flow<Long> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_LATER_CLICKED_MILLIS] ?: ZERO.toLong()
    }

  suspend fun setLaterClickedMilliSeconds(laterClickedMilliSeconds: Long) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_LATER_CLICKED_MILLIS] = laterClickedMilliSeconds
    }
  }

  val lastDonationPopupShownInMilliSeconds: Flow<Long> =
    context.kiwixDataStore.data.map { prefs ->
      prefs[PreferencesKeys.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS] ?: ZERO.toLong()
    }

  suspend fun setLastDonationPopupShownInMilliSeconds(lastDonationPopupShownInMilliSeconds: Long) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS] =
        lastDonationPopupShownInMilliSeconds
    }
  }

  val isScanFileSystemDialogShown: Flow<Boolean> =
    context.kiwixDataStore.data.map { pref ->
      pref[PreferencesKeys.PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN] ?: false
    }

  suspend fun setIsScanFileSystemDialogShown(isFileScanFileSystemDialogShown: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN] = isFileScanFileSystemDialogShown
    }
  }

  val isScanFileSystemTest: Flow<Boolean> =
    context.kiwixDataStore.data.map { pref ->
      pref[PreferencesKeys.PREF_IS_SCAN_FILE_SYSTEM_TEST] ?: false
    }

  @VisibleForTesting
  suspend fun setIsScanFileSystemTest(isScanFileSystemTest: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_IS_SCAN_FILE_SYSTEM_TEST] = isScanFileSystemTest
    }
  }

  val showManageExternalFilesPermissionDialog: Flow<Boolean> =
    context.kiwixDataStore.data.map { pref ->
      pref[PreferencesKeys.PREF_MANAGE_EXTERNAL_FILES] ?: true
    }

  suspend fun setShowManageExternalFilesPermissionDialog(isScanFileSystemTest: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_MANAGE_EXTERNAL_FILES] = isScanFileSystemTest
    }
  }

  val showManageExternalFilesPermissionDialogOnRefresh: Flow<Boolean> =
    context.kiwixDataStore.data.map { pref ->
      pref[PreferencesKeys.PREF_SHOW_MANAGE_PERMISSION_DIALOG_ON_REFRESH] ?: true
    }

  @VisibleForTesting
  suspend fun setManageExternalFilesPermissionDialogOnRefresh(showOnRefresh: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_SHOW_MANAGE_PERMISSION_DIALOG_ON_REFRESH] = showOnRefresh
    }
  }

  val showStorageOption: Flow<Boolean> =
    context.kiwixDataStore.data.map { pref ->
      pref[PreferencesKeys.PREF_SHOW_STORAGE_OPTION] ?: true
    }

  suspend fun setShowStorageOption(showStorageOption: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_SHOW_STORAGE_OPTION] = showStorageOption
    }
  }

  val shouldShowStorageSelectionDialogOnCopyMove: Flow<Boolean> =
    context.kiwixDataStore.data.map { pref ->
      pref[PreferencesKeys.PREF_SHOW_COPY_MOVE_STORAGE_SELECTION_DIALOG] ?: true
    }

  suspend fun setShowStorageSelectionDialogOnCopyMove(showStorageOption: Boolean) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_SHOW_COPY_MOVE_STORAGE_SELECTION_DIALOG] = showStorageOption
    }
  }

  val selectedStorage: Flow<String> =
    context.kiwixDataStore.data.map { prefs ->
      val storage = prefs[PreferencesKeys.PREF_STORAGE]
      return@map when {
        storage == null ->
          getPublicDirectoryPath(defaultPublicStorage()).also {
            setSelectedStorage(it)
            setSelectedStoragePosition(0)
          }

        !File(storage).isFileExist() ->
          getPublicDirectoryPath(defaultPublicStorage()).also {
            setSelectedStoragePosition(0)
          }

        else -> storage
      }
    }

  suspend fun setSelectedStorage(selectedStorage: String) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.PREF_STORAGE] = selectedStorage
    }
  }

  suspend fun getPublicDirectoryPath(path: String): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      path
    } else {
      path.substringBefore(context.getString(R.string.android_directory_seperator))
    }

  suspend fun defaultStorage(): String =
    context.getExternalFilesDirs(null)[0]?.path
      ?: context.filesDir.path // a workaround for emulators

  private suspend fun defaultPublicStorage(): String =
    ContextWrapper(context).externalMediaDirs[0]?.path
      ?: context.filesDir.path // a workaround for emulators

  val selectedStoragePosition: Flow<Int> =
    context.kiwixDataStore.data.map { pref ->
      pref[PreferencesKeys.STORAGE_POSITION] ?: 0
    }

  suspend fun setSelectedStoragePosition(pos: Int) {
    context.kiwixDataStore.edit { prefs ->
      prefs[PreferencesKeys.STORAGE_POSITION] = pos
    }
  }
}
