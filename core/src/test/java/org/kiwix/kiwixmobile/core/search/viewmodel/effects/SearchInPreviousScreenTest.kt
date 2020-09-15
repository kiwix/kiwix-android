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

package org.kiwix.kiwixmobile.core.search.viewmodel.effects

import android.content.Intent
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.popNavigationBackstack
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.setNavigationResult
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.FIND_IN_PAGE_SEARCH_STRING

internal class SearchInPreviousScreenTest {

  @Test
  fun `invoke with returns positive result with string to previous screen`() {
    val searchString = "search"
    mockkConstructor(Intent::class)
    val activity = mockk<CoreMainActivity>(relaxed = true)
    SearchInPreviousScreen(searchString).invokeWith(activity)
    verify {
      activity.setNavigationResult(searchString, FIND_IN_PAGE_SEARCH_STRING)
      activity.popNavigationBackstack()
    }
  }
}
