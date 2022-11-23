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
import android.os.Build
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity

@LargeTest
@RunWith(AndroidJUnit4::class)
class LanguageFragmentTest {

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
    UiDevice.getInstance(instrumentation).waitForIdle()
    PreferenceManager.getDefaultSharedPreferences(instrumentation.targetContext.applicationContext)
      .edit {
        putBoolean(SharedPreferenceUtil.PREF_SHOW_INTRO, false)
        putBoolean(SharedPreferenceUtil.PREF_WIFI_ONLY, false)
      }
  }

  @Test
  fun testLanguageFragment() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      UiThreadStatement.runOnUiThread {
        activityScenarioRule.scenario.onActivity {
          it.navigate(R.id.downloadsFragment)
        }
      }

      language {
        waitForDataToLoad()
        clickOnLanguageIcon()
        searchAndSaveLanguage("german", "German")
        clickOnLanguageIcon()
        searchAndSaveLanguage("italiano", "Italian")
      }
    }
  }
}
