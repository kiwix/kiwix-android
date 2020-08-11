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

import android.Manifest
import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.main.KiwixMainActivity

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR2)
class HelpFragmentTest {

  @get:Rule
  var activityRule: ActivityTestRule<KiwixMainActivity> =
    ActivityTestRule(KiwixMainActivity::class.java)

  @get:Rule
  var readPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE)

  @get:Rule
  var writePermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

  @Test
  fun verifyHelpActivity() {
    runOnUiThread { activityRule.activity.navigate(R.id.helpFragment) }
    help {
      clickOnWhatDoesKiwixDo()
      assertWhatDoesKiwixDoIsExpanded()
      clickOnWhatDoesKiwixDo()
      clickOnWhereIsContent()
      assertWhereIsContentIsExpanded()
      clickOnWhereIsContent()
      clickOnSendFeedback()
    }
  }
}
