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
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.utils.TAG_FILE_SEARCHED

internal class SearchInPreviousScreenTest {

  @Test
  fun `invoke with returns positive result with string to previous screen`() {
    val searchString = "search"
    mockkConstructor(Intent::class)
    val activity = mockk<AppCompatActivity>()
    SearchInPreviousScreen(searchString).invokeWith(activity)
    verify {
      anyConstructed<Intent>().putExtra(SearchInPreviousScreen.EXTRA_SEARCH_IN_TEXT, true)
      anyConstructed<Intent>().putExtra(TAG_FILE_SEARCHED, searchString)
      activity.setResult(Activity.RESULT_OK, any())
      activity.finish()
    }
  }
}
