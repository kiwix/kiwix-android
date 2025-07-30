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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
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
import org.kiwix.kiwixmobile.utils.StandardActions.openDrawer
import org.kiwix.kiwixmobile.webserver.ZimHostRobot
import org.kiwix.kiwixmobile.webserver.zimHost

fun topLevel(func: TopLevelDestinationRobot.() -> Unit) =
  TopLevelDestinationRobot().applyWithViewHierarchyPrinting(func)

class TopLevelDestinationRobot : BaseRobot() {
  fun clickReaderOnBottomNav(func: ReaderRobot.() -> Unit) {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    testFlakyView({ onView(withId(R.id.readerFragment)).perform(click()) })
    reader(func)
  }

  fun clickLibraryOnBottomNav(func: LibraryRobot.() -> Unit) {
    clickOn(ViewId(R.id.libraryFragment))
    library(func)
    pressBack()
  }

  fun clickDownloadOnBottomNav(func: OnlineLibraryRobot.() -> Unit) {
    clickOn(ViewId(R.id.downloadsFragment))
    onlineLibrary(func)
  }

  private fun inNavDrawer(coreMainActivity: CoreMainActivity, navDrawerAction: () -> Unit) {
    openDrawer(coreMainActivity)
    navDrawerAction.invoke()
    pressBack()
  }

  fun clickBookmarksOnNavDrawer(
    coreMainActivity: CoreMainActivity,
    func: BookmarksRobot.() -> Unit
  ) {
    inNavDrawer(coreMainActivity = coreMainActivity) {
      testFlakyView({ onView(withText(string.bookmarks)).perform(click()) })
      bookmarks(func)
      pressBack()
    }
  }

  fun clickHistoryOnSideNav(coreMainActivity: CoreMainActivity, func: HistoryRobot.() -> Unit) {
    inNavDrawer(coreMainActivity) {
      clickOn(TextId(string.history))
      history(func)
      pressBack()
    }
  }

  fun clickHostBooksOnSideNav(coreMainActivity: CoreMainActivity, func: ZimHostRobot.() -> Unit) {
    inNavDrawer(coreMainActivity) {
      clickOn(TextId(string.menu_wifi_hotspot))
      zimHost(func)
    }
  }

  fun clickSettingsOnSideNav(coreMainActivity: CoreMainActivity, func: SettingsRobot.() -> Unit) {
    inNavDrawer(coreMainActivity) {
      clickOn(TextId(string.menu_settings))
      settingsRobo(func)
    }
  }

  fun clickHelpOnSideNav(coreMainActivity: CoreMainActivity, func: HelpRobot.() -> Unit) {
    inNavDrawer(coreMainActivity) {
      clickOn(TextId(string.menu_help))
      help(func)
    }
  }

  fun clickSupportKiwixOnSideNav(coreMainActivity: CoreMainActivity) {
    inNavDrawer(coreMainActivity) {
      clickOn(TextId(string.menu_support_kiwix))
    }
  }
}
