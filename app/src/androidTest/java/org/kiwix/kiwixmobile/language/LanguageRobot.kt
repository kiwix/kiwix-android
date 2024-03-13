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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import junit.framework.AssertionFailedError
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.utils.RecyclerViewMatcher

fun language(func: LanguageRobot.() -> Unit) = LanguageRobot().applyWithViewHierarchyPrinting(func)

class LanguageRobot : BaseRobot() {

  private var retryCountForDataToLoad = 10

  fun clickDownloadOnBottomNav() {
    clickOn(ViewId(R.id.downloadsFragment))
  }

  fun waitForDataToLoad() {
    try {
      isVisible(Findable.Text("Off the Grid"))
    } catch (e: RuntimeException) {
      if (retryCountForDataToLoad > 0) {
        retryCountForDataToLoad--
        waitForDataToLoad()
      }
    }
  }

  fun clickOnLanguageIcon() {
    // Wait for a few seconds to properly saved selected language.
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    clickOn(ViewId(R.id.select_language))
  }

  fun clickOnLanguageSearchIcon() {
    clickOn(ViewId(R.id.menu_language_search))
  }

  fun searchLanguage(searchLanguage: String) {
    isVisible(ViewId(androidx.appcompat.R.id.search_src_text)).text = searchLanguage
  }

  fun selectLanguage(matchLanguage: String) {
    clickOn(Text(matchLanguage))
  }

  fun clickOnSaveLanguageIcon() {
    clickOn(ViewId(R.id.menu_language_save))
  }

  fun checkIsLanguageSelected() {
    onView(
      RecyclerViewMatcher(R.id.language_recycler_view).atPositionOnView(
        1,
        R.id.item_language_checkbox
      )
    ).check(
      matches(isChecked())
    )
  }

  fun deSelectLanguageIfAlreadySelected() {
    try {
      onView(
        RecyclerViewMatcher(R.id.language_recycler_view).atPositionOnView(
          1,
          R.id.item_language_checkbox
        )
      ).check(matches(isNotChecked()))
    } catch (assertionError: AssertionFailedError) {
      onView(
        RecyclerViewMatcher(R.id.language_recycler_view).atPositionOnView(
          1,
          R.id.item_language_checkbox
        )
      ).perform(click())
    }
  }
}
