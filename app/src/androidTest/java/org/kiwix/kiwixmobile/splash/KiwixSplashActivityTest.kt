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
package org.kiwix.kiwixmobile.splash

import android.Manifest
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import leakcanary.LeakAssertions
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible

@LargeTest
@RunWith(AndroidJUnit4::class)
class KiwixSplashActivityTest {

  @Rule
  @JvmField
  var retryRule = RetryRule()

  private val activityScenario: ActivityScenario<KiwixMainActivity> =
    ActivityScenario.launch(KiwixMainActivity::class.java)

  @Rule
  @JvmField
  var readPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)

  @Rule
  @JvmField
  var writePermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)
  private var context: Context? = null

  @Before
  fun setUp() {
    Intents.init()
    context = InstrumentationRegistry.getInstrumentation().targetContext
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(context)
      }
      waitForIdle()
    }
  }

  @Test
  fun testFirstRun() {
    shouldShowIntro(true)
    activityScenario.recreate()
    activityScenario.onActivity {
    }
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    Espresso.onView(ViewMatchers.withId(R.id.get_started))
      .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    // Verify that the value of the "intro shown" boolean inside
    // the SharedPreferences Database is not changed until
    // the "Get started" button is pressed
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    Assert.assertEquals(
      true,
      preferences.getBoolean(
        SharedPreferenceUtil.PREF_SHOW_INTRO,
        true
      )
    )
    LeakAssertions.assertNoLeaks()
  }

  @Test
  fun testNormalRun() {
    shouldShowIntro(false)
    activityScenario.recreate()
    activityScenario.onActivity {
    }
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    Intents.intended(
      IntentMatchers.hasComponent(
        KiwixMainActivity::class.java.canonicalName
      )
    )
    LeakAssertions.assertNoLeaks()
  }

  @After
  fun endTest() {
    Intents.release()
  }

  private fun shouldShowIntro(value: Boolean) {
    val preferencesEditor = PreferenceManager.getDefaultSharedPreferences(
      context
    ).edit()
    preferencesEditor.putBoolean(
      SharedPreferenceUtil.PREF_SHOW_INTRO,
      value
    ).commit()
  }
}
