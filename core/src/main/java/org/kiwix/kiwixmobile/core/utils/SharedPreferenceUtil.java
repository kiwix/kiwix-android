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

package org.kiwix.kiwixmobile.core.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kiwix.kiwixmobile.core.CoreApp;
import org.kiwix.kiwixmobile.core.NightModeConfig;

/**
 * Manager for the Default Shared Preferences of the application.
 */

@Singleton
public class SharedPreferenceUtil {
  // Prefs
  public static final String PREF_LANG = "pref_language_chooser";
  public static final String PREF_STORAGE = "pref_select_folder";
  public static final String PREF_WIFI_ONLY = "pref_wifi_only";
  public static final String PREF_KIWIX_MOBILE = "kiwix-mobile";
  public static final String PREF_ZOOM = "pref_zoom_slider";
  public static final String PREF_ZOOM_ENABLED = "pref_zoom_enabled";
  public static final String PREF_SHOW_INTRO = "showIntro";
  private static final String PREF_BACK_TO_TOP = "pref_backtotop";
  private static final String PREF_HIDE_TOOLBAR = "pref_hidetoolbar";
  private static final String PREF_FULLSCREEN = "pref_fullscreen";
  private static final String PREF_NEW_TAB_BACKGROUND = "pref_newtab_background";
  private static final String PREF_STORAGE_TITLE = "pref_selected_title";
  private static final String PREF_EXTERNAL_LINK_POPUP = "pref_external_link_popup";
  private static final String PREF_IS_FIRST_RUN = "isFirstRun";
  private static final String PREF_SHOW_BOOKMARKS_CURRENT_BOOK = "show_bookmarks_current_book";
  private static final String PREF_SHOW_HISTORY_CURRENT_BOOK = "show_history_current_book";
  private static final String PREF_HOSTED_BOOKS = "hosted_books";
  public static final String PREF_NIGHT_MODE = "pref_night_mode";
  private SharedPreferences sharedPreferences;
  private final PublishProcessor<String> prefStorages = PublishProcessor.create();
  private final PublishProcessor<NightModeConfig.Mode> nightModes = PublishProcessor.create();

  @Inject
  public SharedPreferenceUtil(Context context) {
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public boolean getPrefWifiOnly() {
    return sharedPreferences.getBoolean(PREF_WIFI_ONLY, true);
  }

  public boolean getPrefHideToolbar() {
    return sharedPreferences.getBoolean(PREF_HIDE_TOOLBAR, true);
  }

  public boolean getPrefIsFirstRun() {
    return sharedPreferences.getBoolean(PREF_IS_FIRST_RUN, true);
  }

  public boolean getPrefFullScreen() {
    return sharedPreferences.getBoolean(PREF_FULLSCREEN, false);
  }

  public boolean getPrefBackToTop() {
    return sharedPreferences.getBoolean(PREF_BACK_TO_TOP, false);
  }

  public boolean getPrefZoomEnabled() {
    return sharedPreferences.getBoolean(PREF_ZOOM_ENABLED, false);
  }

  public boolean getPrefNewTabBackground() {
    return sharedPreferences.getBoolean(PREF_NEW_TAB_BACKGROUND, false);
  }

  public boolean getPrefExternalLinkPopup() {
    return sharedPreferences.getBoolean(PREF_EXTERNAL_LINK_POPUP, true);
  }

  public float getPrefZoom() {
    return sharedPreferences.getFloat(PREF_ZOOM, 100.0f);
  }

  public String getPrefLanguage(String defaultLanguage) {
    return sharedPreferences.getString(PREF_LANG, defaultLanguage);
  }

  public String getPrefStorage() {
    String storage = sharedPreferences.getString(PREF_STORAGE, null);
    if (storage == null) {
      final File externalFilesDir =
        ContextCompat.getExternalFilesDirs(CoreApp.getInstance(), null)[0];
      storage = externalFilesDir != null ? externalFilesDir.getPath()
        : CoreApp.getInstance().getFilesDir().getPath(); // workaround for emulators
      putPrefStorage(storage);
    }
    return storage;
  }

  public String getPrefStorageTitle(String defaultTitle) {
    return sharedPreferences.getString(PREF_STORAGE_TITLE, defaultTitle);
  }

  public boolean getPrefFullTextSearch() {
    return false; // Temporarily disable multizim for 2.4
    //return sharedPreferences.getBoolean(PREF_FULL_TEXT_SEARCH, false);
  }

  public void putPrefLanguage(String language) {
    sharedPreferences.edit().putString(PREF_LANG, language).apply();
  }

  public void putPrefIsFirstRun(boolean isFirstRun) {
    sharedPreferences.edit().putBoolean(PREF_IS_FIRST_RUN, isFirstRun).apply();
  }

  public void putPrefWifiOnly(boolean wifiOnly) {
    sharedPreferences.edit().putBoolean(PREF_WIFI_ONLY, wifiOnly).apply();
  }

  public void putPrefStorageTitle(String storageTitle) {
    sharedPreferences.edit().putString(PREF_STORAGE_TITLE, storageTitle).apply();
  }

  public void putPrefStorage(String storage) {
    sharedPreferences.edit().putString(PREF_STORAGE, storage).apply();
    prefStorages.onNext(storage);
  }

  public Flowable<String> getPrefStorages() {
    return prefStorages.startWith(getPrefStorage());
  }

  public void putPrefFullScreen(boolean fullScreen) {
    sharedPreferences.edit().putBoolean(PREF_FULLSCREEN, fullScreen).apply();
  }

  public void putPrefExternalLinkPopup(boolean externalLinkPopup) {
    sharedPreferences.edit().putBoolean(PREF_EXTERNAL_LINK_POPUP, externalLinkPopup).apply();
  }

  public boolean showIntro() {
    return sharedPreferences.getBoolean(PREF_SHOW_INTRO, true);
  }

  public void setIntroShown() {
    sharedPreferences.edit().putBoolean(PREF_SHOW_INTRO, false).apply();
  }

  public boolean getShowHistoryCurrentBook() {
    return sharedPreferences.getBoolean(PREF_SHOW_HISTORY_CURRENT_BOOK, true);
  }

  public void setShowHistoryCurrentBook(boolean prefShowHistoryCurrentBook) {
    sharedPreferences.edit()
      .putBoolean(PREF_SHOW_HISTORY_CURRENT_BOOK, prefShowHistoryCurrentBook)
      .apply();
  }

  public boolean getShowBookmarksCurrentBook() {
    return sharedPreferences.getBoolean(PREF_SHOW_BOOKMARKS_CURRENT_BOOK, true);
  }

  public void setShowBookmarksCurrentBook(boolean prefShowBookmarksFromCurrentBook) {
    sharedPreferences.edit()
      .putBoolean(PREF_SHOW_BOOKMARKS_CURRENT_BOOK, prefShowBookmarksFromCurrentBook)
      .apply();
  }

  @NonNull
  public NightModeConfig.Mode getNightMode() {
    return NightModeConfig.Mode.from(
      Integer.parseInt(
        sharedPreferences.getString(
          PREF_NIGHT_MODE,
          "" + AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
      )
    );
  }

  public Flowable<NightModeConfig.Mode> nightModes() {
    return nightModes.startWith(getNightMode());
  }

  public void updateNightMode() {
    nightModes.offer(getNightMode());
  }

  public Set<String> getHostedBooks() {
    return sharedPreferences.getStringSet(PREF_HOSTED_BOOKS, new HashSet<>());
  }

  public void setHostedBooks(Set<String> hostedBooks) {
    sharedPreferences.edit()
      .putStringSet(PREF_HOSTED_BOOKS, hostedBooks)
      .apply();
  }
}
