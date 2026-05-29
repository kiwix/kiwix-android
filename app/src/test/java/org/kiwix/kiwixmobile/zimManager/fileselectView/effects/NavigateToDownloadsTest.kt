/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.zimManager.fileselectView.effects

import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.navigate
import org.kiwix.kiwixmobile.ui.KiwixDestination

class NavigateToDownloadsTest {
  @Test
  fun `invokeWith should navigate to downloads screen`() {
    val activity = mockk<AppCompatActivity>(relaxed = true)

    mockkObject(ActivityExtensions)

    every {
      activity.navigate(any(), any())
    } returns Unit

    NavigateToDownloads.invokeWith(activity)

    verify(exactly = 1) {
      activity.navigate(
        KiwixDestination.Downloads.route,
        match {
          it.shouldRestoreState()
        }
      )
    }

    unmockkObject(ActivityExtensions)
  }
}
