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
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import applyWithViewHierarchyPrinting
import com.adevinta.android.barista.interaction.BaristaSleepInteractions
import com.adevinta.android.barista.interaction.BaristaSwipeRefreshInteractions.refresh
import junit.framework.AssertionFailedError
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.testFlakyView
import org.kiwix.kiwixmobile.utils.RecyclerViewMatcher

fun language(func: LanguageRobot.() -> Unit) = LanguageRobot().applyWithViewHierarchyPrinting(func)

class LanguageRobot : BaseRobot() {

  fun clickDownloadOnBottomNav() {
    clickOn(ViewId(R.id.downloadsFragment))
  }

  fun waitForDataToLoad(retryCountForDataToLoad: Int = 10) {
    try {
      isVisible(TextId(string.your_languages))
    } catch (e: RuntimeException) {
      if (retryCountForDataToLoad > 0) {
        // refresh the data if there is "Swipe Down for Library" visible on the screen.
        refreshOnlineListIfSwipeDownForLibraryTextVisible()
        waitForDataToLoad(retryCountForDataToLoad - 1)
        return
      }
      // throw the exception when there is no more retry left.
      throw RuntimeException("Couldn't load the online library list.\n Original exception = $e")
    }
  }

  private fun refreshOnlineListIfSwipeDownForLibraryTextVisible() {
    try {
      onView(ViewMatchers.withText(string.swipe_down_for_library)).check(matches(isDisplayed()))
      refreshOnlineList()
    } catch (e: RuntimeException) {
      try {
        // do nothing as currently downloading the online library.
        onView(withId(R.id.onlineLibraryProgressLayout)).check(matches(isDisplayed()))
      } catch (e: RuntimeException) {
        // if not visible try to get the online library.
        refreshOnlineList()
      }
    }
  }

  private fun refreshOnlineList() {
    refresh(R.id.librarySwipeRefresh)
  }

  fun clickOnLanguageIcon() {
    // Wait for a few seconds to properly saved selected language.
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
    clickOn(ViewId(R.id.select_language))
  }

  fun clickOnLanguageSearchIcon() {
    testFlakyView({ onView(withId(R.id.menu_language_search)).perform(click()) })
  }

  fun searchLanguage(searchLanguage: String) {
    isVisible(ViewId(androidx.appcompat.R.id.search_src_text)).text = searchLanguage
  }

  fun selectLanguage(matchLanguage: String) {
    testFlakyView({ clickOn(Text(matchLanguage)) })
  }

  fun clickOnSaveLanguageIcon() {
    clickOn(ViewId(R.id.menu_language_save))
  }

  fun checkIsLanguageSelected() {
    // Wait for a second to properly visible the searched language on top.
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
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
    // Wait for a second to properly visible the searched language on top.
    BaristaSleepInteractions.sleep(TestUtils.TEST_PAUSE_MS.toLong())
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
