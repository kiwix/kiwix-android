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

package org.kiwix.kiwixmobile.core.page.viewmodel.effects

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.page.PageImpl
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_FILE
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_URL

internal class OpenPageTest {
  val page = PageImpl()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  val activity: AppCompatActivity = mockk(relaxed = true)
  val intent: Intent = mockk()

  init {
    mockkConstructor(Intent::class)
    every { anyConstructed<Intent>().putExtra(EXTRA_CHOSE_X_URL, page.url) } returns intent
  }

  @Test
  fun `invokeWith returns an Ok Result with historyUrl`() {
    every { zimReaderContainer.zimCanonicalPath } returns "zimFilePath"
    OpenPage(page, zimReaderContainer).invokeWith(activity)
    verify {
      activity.setResult(Activity.RESULT_OK, intent)
      activity.finish()
      anyConstructed<Intent>().putExtra(EXTRA_CHOSE_X_URL, page.url)
    }
    confirmVerified(intent)
  }

  @Test
  fun `invokeWith returns an Ok Result with historyUrl and zimFilePath`() {
    every { zimReaderContainer.zimCanonicalPath } returns "notZimFilePath"
    every { intent.putExtra(EXTRA_CHOSE_X_FILE, page.zimFilePath) } returns intent
    OpenPage(page, zimReaderContainer).invokeWith(activity)
    verify {
      intent.putExtra(EXTRA_CHOSE_X_FILE, page.zimFilePath)
      activity.setResult(Activity.RESULT_OK, intent)
      activity.finish()
    }
  }
}
