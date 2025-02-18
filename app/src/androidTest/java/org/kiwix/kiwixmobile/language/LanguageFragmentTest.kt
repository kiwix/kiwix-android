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
package org.kiwix.kiwixmobile.language

import android.Manifest
import android.app.Instrumentation
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import leakcanary.LeakAssertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.utils.StandardActions

@LargeTest
@RunWith(AndroidJUnit4::class)
class LanguageFragmentTest {

  @Rule
  @JvmField
  var retryRule = RetryRule()

  private val permissions = arrayOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
  )

  @Rule
  @JvmField
  var permissionRules: GrantPermissionRule =
    GrantPermissionRule.grant(*permissions)

  private val instrumentation: Instrumentation by lazy {
    InstrumentationRegistry.getInstrumentation()
  }

  init {
    AccessibilityChecks.enable().setRunChecksFromRootView(true)
  }

  @Before
  fun setUp() {
    UiDevice.getInstance(instrumentation).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(instrumentation.targetContext.applicationContext, this)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext.applicationContext)
      .edit {
        putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
        putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
        putString(SharedPreferenceUtil.PREF_LANG, "en")
        putLong(
          SharedPreferenceUtil.PREF_LAST_DONATION_POPUP_SHOWN_IN_MILLISECONDS,
          System.currentTimeMillis()
        )
      }
    ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
      }
    }
  }

  @Test
  fun testLanguageFragment() {
    StandardActions.closeDrawer() // close the drawer if open before running the test cases.
    language {
      clickDownloadOnBottomNav()
      waitForDataToLoad()

      // search and de-select if german language already selected
      clickOnLanguageIcon()
      clickOnLanguageSearchIcon()
      searchLanguage("german")
      deSelectLanguageIfAlreadySelected()
      clickOnSaveLanguageIcon()

      // search and de-select if italian language already selected
      clickOnLanguageIcon()
      clickOnLanguageSearchIcon()
      searchLanguage("italiano")
      deSelectLanguageIfAlreadySelected()
      clickOnSaveLanguageIcon()

      // Search and save language for german
      clickOnLanguageIcon()
      clickOnLanguageSearchIcon()
      searchLanguage("german")
      selectLanguage("German")
      clickOnSaveLanguageIcon()

      // Search and save language for italian
      clickOnLanguageIcon()
      clickOnLanguageSearchIcon()
      searchLanguage("italiano")
      selectLanguage("Italian")
      clickOnSaveLanguageIcon()

      // verify is german language selected
      clickOnLanguageIcon()
      clickOnLanguageSearchIcon()
      searchLanguage("german")
      checkIsLanguageSelected()
      clickOnSaveLanguageIcon()

      // verify is italian language selected
      clickOnLanguageIcon()
      clickOnLanguageSearchIcon()
      searchLanguage("italiano")
      checkIsLanguageSelected()
      clickOnSaveLanguageIcon()
    }
    LeakAssertions.assertNoLeaks()
  }
}
