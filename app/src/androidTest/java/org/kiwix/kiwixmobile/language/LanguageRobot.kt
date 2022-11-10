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

package org.kiwix.kiwixmobile.language

import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R

fun language(func: LanguageRobot.() -> Unit) = LanguageRobot().applyWithViewHierarchyPrinting(func)

class LanguageRobot : BaseRobot() {

  private var retryCountForDataToLoad = 5

  fun waitForDataToLoad() {
    try {
      isVisible(Text("Off the Grid"))
    } catch (e: RuntimeException) {
      if (retryCountForDataToLoad > 0) {
        retryCountForDataToLoad--
        waitForDataToLoad()
      }
    }
  }

  fun clickOnLanguageIcon() {
    clickOn(ViewId(R.id.select_language))
  }

  fun searchAndSaveLanguage(searchLanguage: String, matchLanguage: String) {
    clickOn(ViewId(R.id.menu_language_search))
    isVisible(ViewId(androidx.appcompat.R.id.search_src_text)).text = searchLanguage
    clickOn(Text(matchLanguage))
    clickOn(ViewId(R.id.menu_language_save))
  }
}
