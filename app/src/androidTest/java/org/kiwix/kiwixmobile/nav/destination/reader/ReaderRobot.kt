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

package org.kiwix.kiwixmobile.nav.destination.reader

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.core.main.reader.READER_SCREEN_TESTING_TAG
import org.kiwix.kiwixmobile.testutils.TestUtils.waitUntilTimeout

fun reader(func: ReaderRobot.() -> Unit) = ReaderRobot().applyWithViewHierarchyPrinting(func)

class ReaderRobot : BaseRobot() {
  fun assertReaderScreenDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.apply {
      waitForIdle()
      waitUntilTimeout()
      onNodeWithTag(READER_SCREEN_TESTING_TAG).assertExists()
    }
  }
}
