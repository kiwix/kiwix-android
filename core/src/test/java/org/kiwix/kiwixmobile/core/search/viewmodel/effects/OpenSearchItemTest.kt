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
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.popNavigationBackstack
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.RecentSearchListItem
import org.kiwix.kiwixmobile.core.utils.TAG_FILE_SEARCHED
import org.kiwix.kiwixmobile.core.utils.TAG_FILE_SEARCHED_NEW_TAB

internal class OpenSearchItemTest {

  @Test
  fun `invoke with returns an Ok Result with list item value`() {
    val searchListItem = RecentSearchListItem("")
    val activity: CoreMainActivity = mockk(relaxed = true)
    mockkConstructor(Intent::class)
    val intent = mockk<Intent>()
    every {
      anyConstructed<Intent>().putExtra(TAG_FILE_SEARCHED, searchListItem.value)
        .putExtra(TAG_FILE_SEARCHED_NEW_TAB, false)
    } returns intent
    OpenSearchItem(searchListItem, false).invokeWith(activity)
    verify {
      activity.popNavigationBackstack()
      activity.openSearchItem(searchListItem.value, false)
    }
  }

  @Test
  fun `invoke with returns an Ok Result with list item value for new tab`() {
    val searchListItem = RecentSearchListItem("")
    val activity: CoreMainActivity = mockk(relaxed = true)
    mockkConstructor(Intent::class)
    val intent = mockk<Intent>()
    every {
      anyConstructed<Intent>().putExtra(TAG_FILE_SEARCHED, searchListItem.value)
        .putExtra(TAG_FILE_SEARCHED_NEW_TAB, true)
    } returns intent
    OpenSearchItem(searchListItem, true).invokeWith(activity)
    verify {
      activity.popNavigationBackstack()
      activity.openSearchItem(searchListItem.value, true)
    }
  }
}
