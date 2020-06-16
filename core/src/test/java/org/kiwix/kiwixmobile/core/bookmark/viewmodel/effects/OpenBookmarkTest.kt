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

package org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.bookmark
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_FILE
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_URL

internal class OpenBookmarkTest {
  @Test
  fun `invokeWith returns an Ok Result with bookmarkUrl`() {
    val item = bookmark()
    val zimReaderContainer: ZimReaderContainer = mockk()
    every { zimReaderContainer.zimCanonicalPath } returns "zimFilePath"
    val activity: AppCompatActivity = mockk()
    mockkConstructor(Intent::class)
    val intent: Intent = mockk()
    every { anyConstructed<Intent>().putExtra(EXTRA_CHOSE_X_URL, item.bookmarkUrl) } returns intent
    OpenBookmark(item, zimReaderContainer).invokeWith(activity)
    verify {
      activity.setResult(Activity.RESULT_OK, intent)
      activity.finish()
      anyConstructed<Intent>().putExtra(EXTRA_CHOSE_X_URL, item.bookmarkUrl)
    }
    confirmVerified(intent)
  }

  @Test
  fun `invokeWith returns an Ok Result with bookmarkUrl and zimFilePath`() {
    val item = bookmark()
    val zimReaderContainer: ZimReaderContainer = mockk()
    every { zimReaderContainer.zimCanonicalPath } returns "notZimFilePath"
    val activity: AppCompatActivity = mockk()
    mockkConstructor(Intent::class)
    val intent: Intent = mockk()
    every { anyConstructed<Intent>().putExtra(EXTRA_CHOSE_X_URL, item.bookmarkUrl) } returns intent
    every { intent.putExtra(EXTRA_CHOSE_X_FILE, item.zimFilePath) } returns intent
    OpenBookmark(item, zimReaderContainer).invokeWith(activity)
    verify {
      intent.putExtra(EXTRA_CHOSE_X_FILE, item.zimFilePath)
      activity.setResult(Activity.RESULT_OK, intent)
      activity.finish()
    }
  }
}
