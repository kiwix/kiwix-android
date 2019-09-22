package org.kiwix.kiwixmobile.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import java.util.Calendar;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kiwix.kiwixmobile.KiwixApplication;

/**
 * Manager for the Default Shared Preferences of the application.
 */

@Singleton
public class SharedPreferenceUtil {
  // Prefs
  public static final String PREF_LANG = "pref_language_chooser";
  public static final String PREF_STORAGE = "pref_select_folder";
  public static final String PREF_AUTONIGHTMODE = "pref_auto_nightmode";
  public static final String PREF_NIGHTMODE = "pref_nightmode";
  public static final String PREF_WIFI_ONLY = "pref_wifi_only";
  public static final String PREF_KIWIX_MOBILE = "kiwix-mobile";
  public static final String PREF_BACK_TO_TOP = "pref_backtotop";
  public static final String PREF_HIDE_TOOLBAR = "pref_hidetoolbar";
  public static final String PREF_ZOOM = "pref_zoom_slider";
  public static final String PREF_ZOOM_ENABLED = "pref_zoom_enabled";
  public static final String PREF_FULLSCREEN = "pref_fullscreen";
  public static final String PREF_NEW_TAB_BACKGROUND = "pref_newtab_background";
  public static final String PREF_FULL_TEXT_SEARCH = "pref_full_text_search";
  public static final String PREF_STORAGE_TITLE = "pref_selected_title";
  public static final String PREF_EXTERNAL_LINK_POPUP = "pref_external_link_popup";
  public static final String PREF_IS_FIRST_RUN = "isFirstRun";
  public static final String PREF_SHOW_INTRO = "showIntro";
  private static final String PREF_SHOW_BOOKMARKS_CURRENT_BOOK = "show_bookmarks_current_book";
  private static final String PREF_SHOW_HISTORY_CURRENT_BOOK = "show_history_current_book";
  private SharedPreferences sharedPreferences;
  private final PublishProcessor<String> prefStorages = PublishProcessor.create();

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
      storage =
        ContextCompat.getExternalFilesDirs(KiwixApplication.getInstance(), null)[0].getPath();
      putPrefStorage(storage);
    }
    return storage;
  }

  private boolean getPrefNightMode() {
    return sharedPreferences.getBoolean(PREF_NIGHTMODE, false);
  }

  public boolean getPrefAutoNightMode() {
    return sharedPreferences.getBoolean(PREF_AUTONIGHTMODE, false);
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

  public boolean nightMode() {
    boolean autoNightMode = getPrefAutoNightMode();
    if (autoNightMode) {
      Calendar cal = Calendar.getInstance();
      int hour = cal.get(Calendar.HOUR_OF_DAY);
      return hour < 6 || hour > 18;
    } else {
      return getPrefNightMode();
    }
  }

  public boolean getShowBookmarksCurrentBook() {
    return sharedPreferences.getBoolean(PREF_SHOW_BOOKMARKS_CURRENT_BOOK, true);
  }

  public void setShowBookmarksCurrentBook(boolean prefShowBookmarksFromCurrentBook) {
    sharedPreferences.edit()
      .putBoolean(PREF_SHOW_BOOKMARKS_CURRENT_BOOK, prefShowBookmarksFromCurrentBook)
      .apply();
  }
}
