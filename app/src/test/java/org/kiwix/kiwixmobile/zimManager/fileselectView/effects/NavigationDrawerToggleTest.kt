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
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.main.CoreMainActivity

class NavigationDrawerToggleTest {
  @Test
  fun `invokeWith should close drawer when drawer is already open`() {
    val activity = mockk<CoreMainActivity>(relaxed = true)

    every { activity.navigationDrawerIsOpen() } returns true

    NavigationDrawerToggle.invokeWith(activity)

    verify(exactly = 1) {
      activity.closeNavigationDrawer()
    }

    verify(exactly = 0) {
      activity.openNavigationDrawer()
    }
  }

  @Test
  fun `invokeWith should open drawer when drawer is closed`() {
    val activity = mockk<CoreMainActivity>(relaxed = true)

    every { activity.navigationDrawerIsOpen() } returns false

    NavigationDrawerToggle.invokeWith(activity)

    verify(exactly = 1) {
      activity.openNavigationDrawer()
    }

    verify(exactly = 0) {
      activity.closeNavigationDrawer()
    }
  }

  @Test
  fun `invokeWith should do nothing when activity is not CoreMainActivity`() {
    val activity = mockk<AppCompatActivity>(relaxed = true)

    NavigationDrawerToggle.invokeWith(activity)

    verify { activity wasNot Called }
  }
}
