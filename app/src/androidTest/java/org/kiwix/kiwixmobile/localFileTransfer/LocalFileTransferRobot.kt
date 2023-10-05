/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.localFileTransfer

import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.testutils.TestUtils

/**
 * Authored by Ayush Shrivastava on 29/10/20
 */

fun localFileTransfer(func: LocalFileTransferRobot.() -> Unit) =
  LocalFileTransferRobot().applyWithViewHierarchyPrinting(func)

class LocalFileTransferRobot : BaseRobot() {

  fun assertReceiveFileTitleVisible() {
    isVisible(TextId(R.string.receive_files_title))
  }

  fun assertSearchDeviceMenuItemVisible() {
    isVisible(ViewId(R.id.menu_item_search_devices))
  }

  fun clickOnSearchDeviceMenuItem() {
    clickOn(ViewId(R.id.menu_item_search_devices))
  }

  fun assertLocalFileTransferScreenVisible() {
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_DOWNLOAD_TEST.toLong())
    assertReceiveFileTitleVisible()
  }

  fun assertLocalLibraryVisible() {
    isVisible(TextId(R.string.library))
  }
}
