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

package org.kiwix.kiwixmobile.core.utils.effects

import androidx.appcompat.app.AppCompatActivity
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Test
import org.kiwix.kiwixmobile.core.extensions.navigateToAppSettings
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog

class ReadPermissionRequiredDialogTest {
  @Test
  fun `invokeWith shows read permission dialog and navigates to settings`() {
    val dialogShower = mockk<AlertDialogShower>()
    val activity = mockk<AppCompatActivity>(relaxed = true)

    val slot = slot<() -> Unit>()

    every {
      dialogShower.show(
        KiwixDialog.ReadPermissionRequired,
        capture(slot)
      )
    } just Runs

    mockkStatic("org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt")

    every {
      activity.navigateToAppSettings()
    } just Runs

    val effect = ReadPermissionRequiredDialog(dialogShower)

    effect.invokeWith(activity)

    verify {
      dialogShower.show(
        KiwixDialog.ReadPermissionRequired,
        any()
      )
    }

    slot.captured.invoke()

    verify {
      activity.navigateToAppSettings()
    }

    unmockkStatic("org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt")
  }
}
