/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.search

import android.os.Build
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.testutils.TestUtils

fun search(func: SearchRobot.() -> Unit) = SearchRobot().applyWithViewHierarchyPrinting(func)

class SearchRobot : BaseRobot() {

  fun clickOnSearchItemInSearchList() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      pressBack()
    }
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS_FOR_SEARCH_TEST.toLong())
    isVisible(ViewId(R.id.search_list))
    clickOn(ViewId(R.id.list_item_search_text))
  }

  fun checkZimFileSearchSuccessful(readerFragment: Int) {
    isVisible(ViewId(readerFragment))
  }

  override fun waitTillLoad() {
    TODO("Not yet implemented")
  }
}
