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

import androidx.appcompat.app.AppCompatActivity
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.RecentSearchListItem

internal class DeleteRecentSearchTest {

  @Test
  fun `invoke with deletes a search`() = runBlocking {
    val searchListItem: SearchListItem = RecentSearchListItem("", "")
    val recentSearchDao: RecentSearchRoomDao = mockk()
    val activity: AppCompatActivity = mockk()
    val viewModelScope = CoroutineScope(Dispatchers.IO)
    DeleteRecentSearch(
      searchListItem = searchListItem,
      recentSearchRoomDao = recentSearchDao,
      viewModelScope = viewModelScope
    ).invokeWith(activity)
    verify { recentSearchDao.deleteSearchString(searchListItem.value) }
  }
}
