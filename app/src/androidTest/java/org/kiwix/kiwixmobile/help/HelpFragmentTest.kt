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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import leakcanary.LeakAssertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.testutils.RetryRule

class HelpFragmentTest : BaseActivityTest() {

  @Before
  override fun waitForIdle() {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle()
  }

  @Rule
  @JvmField
  var retryRule = RetryRule()

  @Test
  fun verifyHelpActivity() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      activityScenarioRule.scenario.onActivity {
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
        clickOnSendFeedback()
      }
      LeakAssertions.assertNoLeaks()
    }
  }
}
