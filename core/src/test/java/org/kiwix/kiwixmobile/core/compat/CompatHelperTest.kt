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

package org.kiwix.kiwixmobile.core.compat

import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.isAirplaneModeOn
import org.robolectric.RobolectricTestRunner

/**
 * isAirplaneModeOn() reads Settings.Global directly rather than going through
 * ConnectivityManager (see its own doc comment for why - #5036), so it needs a real
 * ContentResolver to exercise, unlike the helpers that just mock this extension out.
 */
@RunWith(RobolectricTestRunner::class)
class CompatHelperTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun `isAirplaneModeOn returns false when the setting is off`() {
    Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0)
    assertFalse(context.isAirplaneModeOn())
  }

  @Test
  fun `isAirplaneModeOn returns true when the setting is on`() {
    Settings.Global.putInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 1)
    assertTrue(context.isAirplaneModeOn())
  }
}
