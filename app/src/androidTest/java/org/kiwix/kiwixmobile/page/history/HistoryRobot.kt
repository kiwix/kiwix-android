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

package org.kiwix.kiwixmobile.page.history

import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.testutils.TestUtils

/**
 * Authored by Ayush Shrivastava on 29/10/20
 */

fun history(func: HistoryRobot.() -> Unit) = HistoryRobot().applyWithViewHierarchyPrinting(func)

class HistoryRobot : BaseRobot() {
  /** Pushed back robot rules due to lack of info to assert the correct screen */
  fun clickOnTrashIcon() {
    clickOn(Findable.StringId.ContentDesc(R.string.pref_clear_all_bookmarks_title))
  }

  fun assertDeleteHistoryDialogDisplayed() {
    isVisible(Findable.Text(TestUtils.getResourceString(R.string.delete_history)))
  }
}
