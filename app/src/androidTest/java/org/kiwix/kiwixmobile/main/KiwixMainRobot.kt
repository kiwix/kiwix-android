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

import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.ContentDesc
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.testutils.TestUtils.getResourceString
import org.kiwix.kiwixmobile.utils.StandardActions.openDrawer

fun kiwixMainRobo(func: KiwixMainRobot.() -> Unit) =
  KiwixMainRobot().applyWithViewHierarchyPrinting(func)

class KiwixMainRobot : BaseRobot() {

  init {
    isVisible(ViewId(R.id.navigation_container))
  }

  fun clickReaderOnBottomNav() {
    clickOn(ViewId(R.id.readerFragment))
  }

  fun assertReaderScreenDisplayed() {
    isVisible(Text(getResourceString(R.string.reader)))
  }

  fun clickLibraryOnBottomNav() {
    clickOn(ViewId(R.id.libraryFragment))
  }

  fun library(func: LibraryRobot.() -> Unit) =
    LibraryRobot().applyWithViewHierarchyPrinting(func)

  inner class LibraryRobot : BaseRobot() {
    init {
      isVisible(Text(getResourceString(R.string.library)))
    }

    fun clickFileTransferIcon() {
      clickOn(ViewId(R.id.get_zim_nearby_device))
    }

    fun assertFileTransferScreenDisplayed() {
      isVisible(Text("Receive Files"))
    }
  }

  fun clickDownloadOnBottomNav() {
    clickOn(ViewId(R.id.downloadsFragment))
  }

  fun download(func: DownloadRobot.() -> Unit) =
    DownloadRobot().applyWithViewHierarchyPrinting(func)

  inner class DownloadRobot : BaseRobot() {
    init {
      isVisible(Text(getResourceString(R.string.download)))
    }

    fun clickOnGlobeIcon() {
      clickOn(ViewId(R.id.select_language))
    }

    fun assertLanguagesScreenDisplayed() {
      isVisible(Text(getResourceString(R.string.select_languages)))
    }
  }

  fun clickBookmarksOnNavDrawer() {
    clickOn(Text(getResourceString(R.string.bookmarks)))
  }

  fun bookmarks(func: BookmarksRobot.() -> Unit) =
    BookmarksRobot().applyWithViewHierarchyPrinting(func)

  inner class BookmarksRobot : BaseRobot() {
    /** Pushed back robot rules due to lack of info to assert the correct screen */
    fun clickOnTrashIcon() {
      clickOn(ContentDesc(R.string.pref_clear_all_bookmarks_title))
    }

    fun assertDeleteBookmarksDialogDisplayed() {
      isVisible(Text(getResourceString(R.string.delete_bookmarks)))
    }
  }

  fun clickHistoryOnSideNav() {
    clickOn(Text(getResourceString(R.string.history)))
  }

  fun history(func: HistoryRobot.() -> Unit) = HistoryRobot().applyWithViewHierarchyPrinting(func)
  inner class HistoryRobot : BaseRobot() {
    /** Pushed back robot rules due to lack of info to assert the correct screen */
    fun clickOnTrashIcon() {
      clickOn(ContentDesc(R.string.pref_clear_all_bookmarks_title))
    }

    fun assertDeleteHistoryDialogDisplayed() {
      isVisible(Text(getResourceString(R.string.delete_history)))
    }
  }

  fun clickHostBooksOnSideNav() {
    clickOn(Text(getResourceString(R.string.menu_host_books)))
  }

  fun assertHostBooksScreenDisplayed() {
    isVisible(Text(getResourceString(R.string.menu_host_books)))
  }

  fun clickSettingsOnSideNav() {
    clickOn(Text(getResourceString(R.string.menu_settings)))
  }

  fun assertSettingsScreenDisplayed() {
    isVisible(Text(getResourceString(R.string.menu_settings)))
  }

  fun clickHelpOnSideNav() {
    clickOn(Text(getResourceString(R.string.menu_help)))
  }

  fun assertHelpScreenDisplayed() {
    isVisible(Text(getResourceString(R.string.menu_help)))
  }

  fun clickSupportKiwixOnSideNav() {
    clickOn(Text(getResourceString(R.string.menu_support_kiwix)))
  }

  fun assertExternalLinkDialogDisplayed() {
    isVisible(Text(getResourceString(R.string.external_link_popup_dialog_title)))
  }

  fun navigate() {
    pressBack()
    openDrawer()
  }
}
