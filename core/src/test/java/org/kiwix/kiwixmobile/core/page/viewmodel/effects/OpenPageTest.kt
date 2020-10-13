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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.page.PageImpl
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer

internal class OpenPageTest {
  private val page = PageImpl()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  val activity: CoreMainActivity = mockk()

  @BeforeEach
  internal fun setUp() {
    every { activity.navController.popBackStack() } returns true
  }

  @Test
  fun `invokeWith navigates to page with historyUrl`() {
    every { zimReaderContainer.zimCanonicalPath } returns "zimFilePath"
    OpenPage(page, zimReaderContainer).invokeWith(activity)
    verify {
      activity.openPage(page.url)
    }
  }

  @Test
  fun `invokeWith navigates to page with historyUrl and zimFilePath`() {
    every { zimReaderContainer.zimCanonicalPath } returns "notZimFilePath"
    OpenPage(page, zimReaderContainer).invokeWith(activity)
    verify {
      activity.openPage(page.url, page.zimFilePath!!)
    }
  }
}
