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
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.rules.ActivityScenarioRule
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

@LargeTest
@RunWith(AndroidJUnit4::class)
class LanguageFragmentTest {

  @Rule
  @JvmField
  var retryRule = RetryRule()

  @get:Rule
  var activityScenarioRule = ActivityScenarioRule(KiwixMainActivity::class.java)

  @get:Rule
  var readPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)

  @get:Rule
  var writePermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

  private val instrumentation: Instrumentation by lazy {
    InstrumentationRegistry.getInstrumentation()
  }

  @Before
  fun setUp() {
    UiDevice.getInstance(instrumentation).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(instrumentation.targetContext.applicationContext)
      }
      waitForIdle()
    }
    PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext.applicationContext)
      .edit {
        putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
        putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      }
  }

  @Test
  fun testLanguageFragment() {
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
