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

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_BOOKMARK_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_HELP_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_HISTORY_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_SETTINGS_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_SUPPORT_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.core.main.LEFT_DRAWER_ZIM_HOST_ITEM_TESTING_TAG
import org.kiwix.kiwixmobile.help.HelpRobot
import org.kiwix.kiwixmobile.help.help
import org.kiwix.kiwixmobile.nav.destination.library.LibraryRobot
import org.kiwix.kiwixmobile.nav.destination.library.OnlineLibraryRobot
import org.kiwix.kiwixmobile.nav.destination.library.library
import org.kiwix.kiwixmobile.nav.destination.library.onlineLibrary
import org.kiwix.kiwixmobile.nav.destination.reader.ReaderRobot
import org.kiwix.kiwixmobile.nav.destination.reader.reader
import org.kiwix.kiwixmobile.page.bookmarks.BookmarksRobot
import org.kiwix.kiwixmobile.page.bookmarks.bookmarks
import org.kiwix.kiwixmobile.page.history.HistoryRobot
import org.kiwix.kiwixmobile.page.history.history
import org.kiwix.kiwixmobile.settings.SettingsRobot
import org.kiwix.kiwixmobile.settings.settingsRobo
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout
import org.kiwix.kiwixmobile.utils.StandardActions.openDrawer
import org.kiwix.kiwixmobile.webserver.ZimHostRobot
import org.kiwix.kiwixmobile.webserver.zimHost

fun topLevel(func: TopLevelDestinationRobot.() -> Unit) =
  TopLevelDestinationRobot().applyWithViewHierarchyPrinting(func)

class TopLevelDestinationRobot : BaseRobot() {
  fun clickReaderOnBottomNav(
    composeTestRule: ComposeContentTestRule,
    func: ReaderRobot.() -> Unit
  ) {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    testFlakyView({
      composeTestRule.apply {
        waitUntilTimeout()
        onNodeWithTag(BOTTOM_NAV_READER_ITEM_TESTING_TAG).performClick()
      }
    })
    reader(func)
  }

  fun clickLibraryOnBottomNav(
    composeTestRule: ComposeContentTestRule,
    func: LibraryRobot.() -> Unit
  ) {
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(BOTTOM_NAV_LIBRARY_ITEM_TESTING_TAG).performClick()
    }
    library(func)
    pressBack()
  }

  fun clickDownloadOnBottomNav(
    composeTestRule: ComposeContentTestRule,
    func: OnlineLibraryRobot.() -> Unit
  ) {
    composeTestRule.apply {
      waitUntilTimeout()
      onNodeWithTag(BOTTOM_NAV_DOWNLOADS_ITEM_TESTING_TAG).performClick()
    }
    onlineLibrary(func)
  }

  private fun inNavDrawer(coreMainActivity: CoreMainActivity, navDrawerAction: () -> Unit) {
    openDrawer(coreMainActivity)
    navDrawerAction.invoke()
    pressBack()
  }

  fun clickBookmarksOnNavDrawer(
    coreMainActivity: CoreMainActivity,
    composeTestRule: ComposeContentTestRule,
    func: BookmarksRobot.() -> Unit
  ) {
    inNavDrawer(coreMainActivity = coreMainActivity) {
      testFlakyView({
        composeTestRule.onNodeWithTag(LEFT_DRAWER_BOOKMARK_ITEM_TESTING_TAG).performClick()
      })
      bookmarks(func)
      pressBack()
    }
  }

  fun clickHistoryOnSideNav(
    coreMainActivity: CoreMainActivity,
    composeTestRule: ComposeContentTestRule,
    func: HistoryRobot.() -> Unit
  ) {
    inNavDrawer(coreMainActivity) {
      testFlakyView({
        composeTestRule.onNodeWithTag(LEFT_DRAWER_HISTORY_ITEM_TESTING_TAG).performClick()
      })
      history(func)
      pressBack()
    }
  }

  fun clickHostBooksOnSideNav(
    coreMainActivity: CoreMainActivity,
    composeTestRule: ComposeContentTestRule,
    func: ZimHostRobot.() -> Unit
  ) {
    inNavDrawer(coreMainActivity) {
      testFlakyView({
        composeTestRule.onNodeWithTag(LEFT_DRAWER_ZIM_HOST_ITEM_TESTING_TAG).performClick()
      })
      zimHost(func)
    }
  }

  fun clickSettingsOnSideNav(
    coreMainActivity: CoreMainActivity,
    composeTestRule: ComposeContentTestRule,
    func: SettingsRobot.() -> Unit
  ) {
    inNavDrawer(coreMainActivity) {
      testFlakyView({
        composeTestRule.onNodeWithTag(LEFT_DRAWER_SETTINGS_ITEM_TESTING_TAG).performClick()
      })
      settingsRobo(func)
    }
  }

  fun clickHelpOnSideNav(
    coreMainActivity: CoreMainActivity,
    composeTestRule: ComposeContentTestRule,
    func: HelpRobot.() -> Unit
  ) {
    inNavDrawer(coreMainActivity) {
      testFlakyView({
        composeTestRule.onNodeWithTag(LEFT_DRAWER_HELP_ITEM_TESTING_TAG).performClick()
      })
      help(func)
    }
  }

  fun clickSupportKiwixOnSideNav(
    coreMainActivity: CoreMainActivity,
    composeTestRule: ComposeContentTestRule,
  ) {
    inNavDrawer(coreMainActivity) {
      testFlakyView({
        composeTestRule.onNodeWithTag(LEFT_DRAWER_SUPPORT_ITEM_TESTING_TAG).performClick()
      })
    }
  }
}
