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
package org.kiwix.kiwixmobile.main

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import com.schibsted.spain.barista.interaction.BaristaMenuClickInteractions.clickMenu
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.utils.StandardActions

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest : BaseActivityTest<KiwixMainActivity>() {

  @Rule
  override var activityRule: ActivityTestRule<KiwixMainActivity> =
    ActivityTestRule(KiwixMainActivity::class.java)

  @Before
  fun setup() {
    clickOn(R.string.reader)
  }

  @Test
  fun navigateHelp() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    StandardActions.openDrawer()
    clickOn(R.string.menu_help)
  }

  @Test
  fun navigateSettings() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    StandardActions.openDrawer()
    StandardActions.enterSettings()
  }

  @Test
  fun navigateBookmarks() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    StandardActions.openDrawer()
    clickMenu(TestUtils.getResourceString(R.string.bookmarks))
  }

  @Test
  fun navigateDeviceContent() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    clickOn(R.string.library)
  }

  @Test
  fun navigateOnlineContent() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    clickOn(R.string.download)
  }

  @Test
  fun navigateZimHostActivity() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    StandardActions.openDrawer()
    clickMenu(TestUtils.getResourceString(R.string.menu_host_books))
  }

  @Test
  fun navigateLocalFileTransfer() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    clickOn(R.string.library)
    clickMenu(
      TestUtils.getResourceString(R.string.get_content_from_nearby_device)
    )
  }

  @Test
  fun navigateLanguage() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    clickOn(R.string.download)
    clickMenu(TestUtils.getResourceString(R.string.pref_language_chooser))
  }

  @Test
  fun navigateSupport() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    StandardActions.openDrawer()
    clickMenu(TestUtils.getResourceString(R.string.menu_support_kiwix))
  }
}
