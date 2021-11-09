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
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat.getExternalFilesDirs
import androidx.core.content.edit
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.NightModeConfig
import org.kiwix.kiwixmobile.core.NightModeConfig.Mode.Companion.from
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
  private val _prefStorages = PublishProcessor.create<String>()
  val prefStorages
    get() = _prefStorages.startWith(prefStorage)
  private val _textZooms = PublishProcessor.create<Int>()
  val textZooms
    get() = _textZooms.startWith(textZoom)
  private val nightModes = PublishProcessor.create<NightModeConfig.Mode>()
  private val _prefWifiOnlys = PublishProcessor.create<Boolean>()
  val prefWifiOnlys
    get() = _prefWifiOnlys.startWith(prefWifiOnly)

  val prefWifiOnly: Boolean
    get() = sharedPreferences.getBoolean(PREF_WIFI_ONLY, true)

  val prefIsFirstRun: Boolean
    get() = sharedPreferences.getBoolean(PREF_IS_FIRST_RUN, true)

  val prefFullScreen: Boolean
    get() = sharedPreferences.getBoolean(PREF_FULLSCREEN, false)

  val prefBackToTop: Boolean
    get() = sharedPreferences.getBoolean(PREF_BACK_TO_TOP, false)

  val prefNewTabBackground: Boolean
    get() = sharedPreferences.getBoolean(PREF_NEW_TAB_BACKGROUND, false)

  val prefExternalLinkPopup: Boolean
    get() = sharedPreferences.getBoolean(PREF_EXTERNAL_LINK_POPUP, true)

  val prefLanguage: String
    get() = sharedPreferences.getString(PREF_LANG, "") ?: Locale.ROOT.toString()

  val prefStorage: String
    get() {
      val storage = sharedPreferences.getString(PREF_STORAGE, null)
      return when {
        storage == null -> defaultStorage().also(::putPrefStorage)
        !File(storage).exists() -> defaultStorage()
        else -> storage
      }
    }

  private fun defaultStorage(): String =
    getExternalFilesDirs(context, null)[0]?.path
      ?: context.filesDir.path // a workaround for emulators

  fun getPrefStorageTitle(defaultTitle: String): String =
    sharedPreferences.getString(PREF_STORAGE_TITLE, defaultTitle) ?: defaultTitle

  fun putPrefLanguage(language: String) =
    sharedPreferences.edit { putString(PREF_LANG, language) }

  fun putPrefIsFirstRun(isFirstRun: Boolean) =
    sharedPreferences.edit { putBoolean(PREF_IS_FIRST_RUN, isFirstRun) }

  fun putPrefWifiOnly(wifiOnly: Boolean) {
    sharedPreferences.edit { putBoolean(PREF_WIFI_ONLY, wifiOnly) }
    _prefWifiOnlys.onNext(wifiOnly)
  }

  fun putPrefStorageTitle(storageTitle: String) =
    sharedPreferences.edit { putString(PREF_STORAGE_TITLE, storageTitle) }

  fun putPrefStorage(storage: String) {
    sharedPreferences.edit { putString(PREF_STORAGE, storage) }
    _prefStorages.onNext(storage)
  }

  fun putPrefFullScreen(fullScreen: Boolean) =
    sharedPreferences.edit { putBoolean(PREF_FULLSCREEN, fullScreen) }

  fun putPrefExternalLinkPopup(externalLinkPopup: Boolean) =
    sharedPreferences.edit { putBoolean(PREF_EXTERNAL_LINK_POPUP, externalLinkPopup) }

  fun showIntro(): Boolean = sharedPreferences.getBoolean(PREF_SHOW_INTRO, true)

  fun setIntroShown() = sharedPreferences.edit { putBoolean(PREF_SHOW_INTRO, false) }

  var showHistoryAllBooks: Boolean
    get() = sharedPreferences.getBoolean(PREF_SHOW_HISTORY_ALL_BOOKS, true)
    set(prefShowHistoryAllBooks) {
      sharedPreferences.edit { putBoolean(PREF_SHOW_HISTORY_ALL_BOOKS, prefShowHistoryAllBooks) }
    }

  var showBookmarksAllBooks: Boolean
    get() = sharedPreferences.getBoolean(PREF_SHOW_BOOKMARKS_ALL_BOOKS, true)
    set(prefShowBookmarksFromCurrentBook) = sharedPreferences.edit {
      putBoolean(PREF_SHOW_BOOKMARKS_ALL_BOOKS, prefShowBookmarksFromCurrentBook)
    }

  val nightMode: NightModeConfig.Mode
    get() = from(
      sharedPreferences.getString(PREF_NIGHT_MODE, null)?.toInt()
        ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    )

  fun nightModes(): Flowable<NightModeConfig.Mode> = nightModes.startWith(nightMode)

  fun updateNightMode() = nightModes.offer(nightMode)

  var manageExternalFilesPermissionDialog: Boolean
    get() = sharedPreferences.getBoolean(PREF_MANAGE_EXTERNAL_FILES, true)
    set(prefManageExternalFilesPermissionDialog) =
      sharedPreferences.edit {
        putBoolean(PREF_MANAGE_EXTERNAL_FILES, prefManageExternalFilesPermissionDialog)
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
      _textZooms.offer(textZoom)
    }

  companion object {
    // Prefs
    const val PREF_LANG = "pref_language_chooser"
    const val PREF_STORAGE = "pref_select_folder"
    const val PREF_WIFI_ONLY = "pref_wifi_only"
    const val PREF_KIWIX_MOBILE = "kiwix-mobile"
    const val PREF_SHOW_INTRO = "showIntro"
    private const val PREF_BACK_TO_TOP = "pref_backtotop"
    private const val PREF_FULLSCREEN = "pref_fullscreen"
    private const val PREF_NEW_TAB_BACKGROUND = "pref_newtab_background"
    private const val PREF_STORAGE_TITLE = "pref_selected_title"
    private const val PREF_EXTERNAL_LINK_POPUP = "pref_external_link_popup"
    private const val PREF_IS_FIRST_RUN = "isFirstRun"
    private const val PREF_SHOW_BOOKMARKS_ALL_BOOKS = "show_bookmarks_current_book"
    private const val PREF_SHOW_HISTORY_ALL_BOOKS = "show_history_current_book"
    private const val PREF_HOSTED_BOOKS = "hosted_books"
    const val PREF_NIGHT_MODE = "pref_night_mode"
    private const val TEXT_ZOOM = "true_text_zoom"
    private const val DEFAULT_ZOOM = 100
    private const val PREF_MANAGE_EXTERNAL_FILES = "pref_manage_external_files"
  }
}
