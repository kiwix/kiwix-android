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

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem.RecentSearchListItem
import org.kiwix.kiwixmobile.core.utils.Constants

internal class OpenSearchItemTest {

  @Test
  fun `invoke with returns an Ok Result with list item value`() {
    val searchListItem = RecentSearchListItem("")
    val activity: AppCompatActivity = mockk()
    mockkConstructor(Intent::class)
    val intent = mockk<Intent>()
    every {
      anyConstructed<Intent>().putExtra(Constants.TAG_FILE_SEARCHED, searchListItem.value)
    } returns intent
    OpenSearchItem(searchListItem).invokeWith(activity)
    verify {
      activity.setResult(Activity.RESULT_OK, intent)
      activity.finish()
    }
  }
}
