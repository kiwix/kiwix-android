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
package org.kiwix.kiwixmobile.help

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import leakcanary.LeakAssertions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.kiwixmobile.testutils.TestUtils.closeSystemDialogs
import org.kiwix.kiwixmobile.testutils.TestUtils.isSystemUINotRespondingDialogVisible
import org.kiwix.kiwixmobile.utils.KiwixIdlingResource

class HelpFragmentTest : BaseActivityTest() {

  private lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).apply {
      if (isSystemUINotRespondingDialogVisible(this)) {
        closeSystemDialogs(context, this)
      }
      waitForIdle()
    }
    activityScenario = ActivityScenario.launch(KiwixMainActivity::class.java).apply {
      moveToState(Lifecycle.State.RESUMED)
      onActivity {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
      }
    }
  }

  @Rule
  @JvmField
  var retryRule = RetryRule()

  init {
    AccessibilityChecks.enable().setRunChecksFromRootView(true)
  }

  @Test
  fun verifyHelpActivity() {
    setShowCopyMoveToPublicDirectory(false)
    activityScenario.onActivity {
      it.navigate(R.id.helpFragment)
    }
    help {
      clickOnWhatDoesKiwixDo()
      assertWhatDoesKiwixDoIsExpanded()
      clickOnWhatDoesKiwixDo()
      clickOnWhereIsContent()
      assertWhereIsContentIsExpanded()
      clickOnWhereIsContent()
      clickOnHowToUpdateContent()
      assertHowToUpdateContentIsExpanded()
      clickOnHowToUpdateContent()
      assertWhyCopyMoveFilesToAppPublicDirectoryIsNotVisible()
    }
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.TIRAMISU) {
      // Temporarily disabling leak checks on Android 13,
      // as it is incorrectly detecting leaks in Android's internal classes.
      LeakAssertions.assertNoLeaks()
    }
  }

  @Test
  fun verifyHelpActivityWithPlayStoreRestriction() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      setShowCopyMoveToPublicDirectory(true)
      activityScenario.onActivity {
        it.navigate(R.id.helpFragment)
      }
      help {
        clickOnWhatDoesKiwixDo()
        assertWhatDoesKiwixDoIsExpanded()
        clickOnWhatDoesKiwixDo()
        clickOnWhereIsContent()
        assertWhereIsContentIsExpanded()
        clickOnWhereIsContent()
        clickOnHowToUpdateContent()
        assertHowToUpdateContentIsExpanded()
        clickOnHowToUpdateContent()
        clickWhyCopyMoveFilesToAppPublicDirectory()
        assertWhyCopyMoveFilesToAppPublicDirectoryIsExpanded()
        clickWhyCopyMoveFilesToAppPublicDirectory()
      }
      LeakAssertions.assertNoLeaks()
    }
  }

  private fun setShowCopyMoveToPublicDirectory(showRestriction: Boolean) {
    context.let {
      sharedPreferenceUtil = SharedPreferenceUtil(it).apply {
        setIntroShown()
        putPrefWifiOnly(false)
        setIsPlayStoreBuildType(showRestriction)
        prefIsTest = true
        putPrefLanguage("en")
      }
    }
  }

  @After
  fun finish() {
    IdlingRegistry.getInstance().unregister(KiwixIdlingResource.getInstance())
  }
}
