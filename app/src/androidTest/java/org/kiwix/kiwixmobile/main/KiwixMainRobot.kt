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
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.interaction.BaristaClickInteractions.clickOn
import com.schibsted.spain.barista.interaction.BaristaMenuClickInteractions.clickMenu
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.testutils.TestUtils.getResourceString

fun kiwixMainRobo(func: KiwixMainRobot.() -> Unit) =
  KiwixMainRobot().applyWithViewHierarchyPrinting(func)

class KiwixMainRobot : BaseRobot() {

  init {
    isVisible(ViewId(R.id.navigation_container))
  }

  fun clickReaderOnBottomNav() {
    clickOn(R.id.readerFragment)
  }

  fun assertReaderScreenDisplayed() {
    assertDisplayed(R.string.reader)
  }

  fun clickLibraryOnBottomNav() {
    clickOn(R.id.libraryFragment)
  }

  fun assertLibraryScreenDisplayed() {
    assertDisplayed(R.string.library)
  }

  fun clickFileTransferIcon() {
    clickMenu(getResourceString(R.string.get_content_from_nearby_device))
  }

  fun assertFileTransferScreenDisplayed() {
    assertDisplayed("Receive Files")
  }

  fun clickDownloadOnBottomNav() {
    clickOn(R.id.downloadsFragment)
  }

  fun assertDownloadScreenDisplayed() {
    assertDisplayed(R.string.download)
  }

  fun clickOnGlobeIcon() {
    clickMenu(getResourceString(R.string.pref_language_chooser))
  }

  fun assertLanguagesScreenDisplayed() {
    assertDisplayed(R.string.select_languages)
  }

  fun clickBookmarksOnNavDrawer() {
    clickMenu(getResourceString(R.string.bookmarks))
  }

  fun assertBookmarksScreenDisplayed() {
    assertDisplayed(R.string.bookmarks_from_current_book)
  }

  fun clickHistoryOnSideNav() {
    clickMenu(getResourceString(R.string.history))
  }

  fun assertHistoryScreenDisplayed() {
    assertDisplayed(R.string.history_from_current_book)
  }

  fun clickHostBooksOnSideNav() {
    clickMenu(getResourceString(R.string.menu_host_books))
  }

  fun assertHostBooksScreenDisplayed() {
    assertDisplayed(R.string.menu_host_books)
  }

  fun clickSettingsOnSideNav() {
    clickMenu(getResourceString(R.string.menu_settings))
  }

  fun assertSettingsScreenDisplayed() {
    assertDisplayed(R.string.menu_settings)
  }

  fun clickHelpOnSideNav() {
    clickMenu(getResourceString(R.string.menu_help))
  }

  fun assertHelpScreenDisplayed() {
    assertDisplayed(R.string.menu_help)
  }

  fun clickSupportKiwixOnSideNav() {
    clickMenu(getResourceString(R.string.menu_support_kiwix))
  }

  fun assertExternalLinkDialogDisplayed() {
    assertDisplayed(R.string.external_link_popup_dialog_title)
  }
}
