/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.help

import android.content.Intent
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.rule.ActivityTestRule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest

class HelpActivityTest : BaseActivityTest<HelpActivity>() {

  override var activityRule: ActivityTestRule<HelpActivity> = activityTestRule()

  @Test
  fun verifyHelpActivity() {
    Intents.init()
    help {
      clickOnWhatDoesKiwixDo()
      assertWhatDoesKiwixDoIsExpanded()
      clickOnWhereIsContent()
      assertWhereIsContentIsExpanded()
      clickOnLargeZimFiles()
      assertLargeZimsIsExpanded()
      clickOnSendFeedback()
    }
    intended(IntentMatchers.hasAction(Intent.ACTION_CHOOSER))
    Intents.release()
  }

}
