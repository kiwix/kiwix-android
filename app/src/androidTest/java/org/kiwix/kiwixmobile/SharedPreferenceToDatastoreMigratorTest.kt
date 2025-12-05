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

package org.kiwix.kiwixmobile

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_FILE
import org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_TAB
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.datastore.PreferencesKeys
import org.kiwix.kiwixmobile.core.utils.datastore.SharedPreferenceToDatastoreMigrator
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.ui.KiwixDestination

@RunWith(AndroidJUnit4::class)
class SharedPreferenceToDatastoreMigratorTest {
  @get:Rule
  val tmpFolder = TemporaryFolder()
  private lateinit var context: Context
  private val lifeCycleScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (TestUtils.isSystemUINotRespondingDialogVisible(this)) {
        TestUtils.closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    KiwixDataStore(context).apply {
      lifeCycleScope.launch {
        setWifiOnly(false)
        setIntroShown()
      }
    }
    PreferenceManager.getDefaultSharedPreferences(context).edit {
      putBoolean(SharedPreferenceUtil.PREF_IS_TEST, true)
      putBoolean(SharedPreferenceUtil.IS_PLAY_STORE_BUILD, true)
      putBoolean(SharedPreferenceUtil.PREF_SCAN_FILE_SYSTEM_DIALOG_SHOWN, true)
      putBoolean(SharedPreferenceUtil.PREF_IS_FIRST_RUN, false)
      putString(SharedPreferenceUtil.PREF_LANG, "en")
      putLong(
        SharedPreferenceUtil.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS,
        System.currentTimeMillis()
      )
    }
    ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        LanguageUtils.handleLocaleChange(
          it,
          "en",
          SharedPreferenceUtil(context)
        )
        it.navigate(KiwixDestination.Library.route)
      }
    }
  }

  @Test
  fun testSharedPreferencesAreMigratedToDataStore() = runTest {
    // PREF_KIWIX_MOBILE
    val mobilePrefs = context.getSharedPreferences(
      SharedPreferenceUtil.PREF_KIWIX_MOBILE,
      Context.MODE_PRIVATE
    )
    mobilePrefs.edit()
      .putString(TAG_CURRENT_FILE, "/sdcard/wiki.zim")
      .putInt(TAG_CURRENT_TAB, 3)
      .apply()

    // DEFAULT SharedPreferences (SharedPreferenceUtil)
    val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    defaultPrefs.edit()
      .putInt(SharedPreferenceUtil.TEXT_ZOOM, 120)
      .putBoolean(SharedPreferenceUtil.PREF_BACK_TO_TOP, true)
      .putBoolean(SharedPreferenceUtil.PREF_NEW_TAB_BACKGROUND, false)
      .putBoolean(SharedPreferenceUtil.PREF_EXTERNAL_LINK_POPUP, true)
      .putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      .putString(SharedPreferenceUtil.PREF_THEME, "2")
      .putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, true)
      .putBoolean(SharedPreferenceUtil.PREF_SHOW_SHOWCASE, false)
      .putBoolean(SharedPreferenceUtil.PREF_BOOKMARKS_MIGRATED, true)
      .putBoolean(SharedPreferenceUtil.PREF_RECENT_SEARCH_MIGRATED, true)
      .putBoolean(SharedPreferenceUtil.PREF_NOTES_MIGRATED, true)
      .putBoolean(SharedPreferenceUtil.PREF_HISTORY_MIGRATED, true)
      .putBoolean(SharedPreferenceUtil.PREF_APP_DIRECTORY_TO_PUBLIC_MIGRATED, false)
      .putBoolean(SharedPreferenceUtil.PREF_BOOK_ON_DISK_MIGRATED, true)
      .apply()

    val testDataStore = PreferenceDataStoreFactory.create(
      scope = this,
      migrations = SharedPreferenceToDatastoreMigrator(context).createMigration(),
      produceFile = { tmpFolder.newFile("test.preferences_pb") }
    )

    // Test the SharedPreference are successfully migrated to KiwixDataStore.
    val prefs = testDataStore.data.first()

    // From PREF_KIWIX_MOBILE
    assertEquals("/sdcard/wiki.zim", prefs[PreferencesKeys.TAG_CURRENT_FILE])
    assertEquals(3, prefs[PreferencesKeys.TAG_CURRENT_TAB])

    // From DEFAULT prefs
    assertEquals(120, prefs[PreferencesKeys.TEXT_ZOOM])
    assertEquals(true, prefs[PreferencesKeys.PREF_BACK_TO_TOP])
    assertEquals(false, prefs[PreferencesKeys.PREF_NEW_TAB_BACKGROUND])
    assertEquals(true, prefs[PreferencesKeys.PREF_EXTERNAL_LINK_POPUP])
    assertEquals(false, prefs[PreferencesKeys.PREF_WIFI_ONLY])
    assertEquals(true, prefs[PreferencesKeys.PREF_SHOW_INTRO])
    assertEquals(false, prefs[PreferencesKeys.PREF_SHOW_SHOWCASE])
    assertEquals(true, prefs[PreferencesKeys.PREF_BOOKMARKS_MIGRATED])
    assertEquals(true, prefs[PreferencesKeys.PREF_RECENT_SEARCH_MIGRATED])
    assertEquals(true, prefs[PreferencesKeys.PREF_NOTES_MIGRATED])
    assertEquals(true, prefs[PreferencesKeys.PREF_HISTORY_MIGRATED])
    assertEquals(false, prefs[PreferencesKeys.PREF_APP_DIRECTORY_TO_PUBLIC_MIGRATED])
    assertEquals(true, prefs[PreferencesKeys.PREF_BOOK_ON_DISK_MIGRATED])
    assertEquals("2", prefs[PreferencesKeys.PREF_THEME])
  }
}
