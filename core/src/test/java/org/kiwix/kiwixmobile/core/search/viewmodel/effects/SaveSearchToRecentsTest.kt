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
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.dao.RecentSearchRoomDao
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.search.SearchListItem.RecentSearchListItem
import org.kiwix.sharedFunctions.MainDispatcherRule

internal class SaveSearchToRecentsTest {
  private val newRecentSearchDao: RecentSearchRoomDao = mockk()
  private val searchListItem = RecentSearchListItem("", ZimFileReader.CONTENT_PREFIX)

  private val activity: AppCompatActivity = mockk()

  @RegisterExtension
  private val mainDispatcherRule = MainDispatcherRule()
  private val testViewModelScope = TestScope(mainDispatcherRule.dispatcher)

  @Test
  fun `invoke with null Id does nothing`() =
    runBlocking {
      SaveSearchToRecents(
        newRecentSearchDao,
        searchListItem,
        null,
        testViewModelScope
      ).invokeWith(
        activity
      )
      verify { newRecentSearchDao wasNot Called }
    }

  @Test
  fun `invoke with non null Id saves search`() =
    runBlocking {
      val id = "8812214350305159407L"
      SaveSearchToRecents(
        newRecentSearchDao,
        searchListItem,
        id,
        testViewModelScope
      ).invokeWith(activity)
      delay(50)
      verify {
        newRecentSearchDao.saveSearch(searchListItem.value, id, ZimFileReader.CONTENT_PREFIX)
      }
    }
}
