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
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.ContentDesc
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.R

fun history(func: HistoryRobot.() -> Unit) = HistoryRobot().applyWithViewHierarchyPrinting(func)

class HistoryRobot : BaseRobot() {
  init {
    assertDisplayed(R.string.history_from_current_book)
  }

  fun clickOnTrashIcon() {
    clickOn(ContentDesc(R.string.pref_clear_all_bookmarks_title))
  }

  fun assertDeleteHistoryDialogDisplayed() {
    isVisible(TextId(R.string.delete_history))
  }

  override fun waitTillLoad() {
    TODO("Not yet implemented")
  }
}
