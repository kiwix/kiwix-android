package org.kiwix.kiwixmobile.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import io.reactivex.processors.BehaviorProcessor;
import javax.inject.Inject;
import javax.inject.Singleton;

import static org.kiwix.kiwixmobile.utils.Constants.PREF_AUTONIGHTMODE;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_BACK_TO_TOP;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_BOTTOM_TOOLBAR;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_EXTERNAL_LINK_POPUP;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_FULLSCREEN;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_HIDE_TOOLBAR;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_IS_FIRST_RUN;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_LANG;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_NEW_TAB_BACKGROUND;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_NIGHTMODE;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_STORAGE;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_STORAGE_TITLE;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_WIFI_ONLY;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_ZOOM;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_ZOOM_ENABLED;

/**
 * Manager for the Default Shared Preferences of the application.
 */

@Singleton
public class SharedPreferenceUtil {
  private SharedPreferences sharedPreferences;
  private SharedPreferences.Editor editor;
  public final BehaviorProcessor<String> prefStorages;

  @Inject
  public SharedPreferenceUtil(Context context) {
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    editor = sharedPreferences.edit();
    prefStorages = BehaviorProcessor.createDefault(getPrefStorage());
  }

  public void remove(String key) {
    editor.remove(key).apply();
  }

  //Getters

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

  public boolean getPrefBottomToolbar() {
    return sharedPreferences.getBoolean(PREF_BOTTOM_TOOLBAR, false);
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
    return sharedPreferences.getString(PREF_STORAGE, Environment.getExternalStorageDirectory().getPath());
  }

  public boolean getPrefNightMode() {
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

  // Setters

  public void putPrefLanguage(String language) {
    editor.putString(PREF_LANG, language).apply();
  }

  public void putPrefIsFirstRun(boolean isFirstRun) {
    editor.putBoolean(PREF_IS_FIRST_RUN, isFirstRun).apply();
  }

  public void putPrefWifiOnly(boolean wifiOnly) {
    editor.putBoolean(PREF_WIFI_ONLY, wifiOnly).apply();
  }

  public void putPrefStorageTitle(String storageTitle) {
    editor.putString(PREF_STORAGE_TITLE, storageTitle).apply();
  }

  public void putPrefStorage(String storage) {
    editor.putString(PREF_STORAGE, storage).apply();
    prefStorages.onNext(storage);
  }

  public void putPrefFullScreen(boolean fullScreen) {
    editor.putBoolean(PREF_FULLSCREEN, fullScreen).apply();
  }

  public void putPrefExternalLinkPopup(boolean externalLinkPopup) {
    editor.putBoolean(PREF_EXTERNAL_LINK_POPUP, externalLinkPopup).apply();
  }
}
