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

package org.kiwix.kiwixmobile.utils.effects

import androidx.appcompat.app.AppCompatActivity
import eu.mhutti1.utils.storage.StorageDevice
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertNotNull
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.nav.destination.library.StorageSelectDialogConfig

class ShowStorageSelectionDialogTest {
  private val activity: AppCompatActivity = mockk()
  private val dialogShower: AlertDialogShower = mockk(relaxed = true)
  private val dialogConfig: StorageSelectDialogConfig = mockk(relaxed = true)

  @Test
  fun invokeWith_showsStorageSelectionDialog() {
    val slot = slot<KiwixDialog.StorageSelectionDialog>()

    every { dialogShower.show(capture(slot)) } returns Unit

    ShowStorageSelectionDialog(
      dialogShower = dialogShower,
      dialogConfig = dialogConfig
    ).invokeWith(activity)

    verify(exactly = 1) {
      dialogShower.show(any<KiwixDialog.StorageSelectionDialog>())
    }

    assert(slot.isCaptured)
  }

  @Test
  fun `invokeWith should pass StorageSelectDialogScreen composable`() {
    val dialogShower = AlertDialogShower()

    val config = mockk<StorageSelectDialogConfig>(relaxed = true)

    val sideEffect = ShowStorageSelectionDialog(dialogShower, config)

    sideEffect.invokeWith(mockk(relaxed = true))

    val dialog = dialogShower.dialogState.value?.first
      as KiwixDialog.StorageSelectionDialog

    assertNotNull(dialog.customGetView)
  }

  @Test
  fun `handleStorageSelection should invoke callback`() {
    val callback: (StorageDevice) -> Unit = mockk(relaxed = true)

    val config = mockk<StorageSelectDialogConfig>(relaxed = true) {
      every { onSelectAction } returns callback
    }

    val sideEffect = ShowStorageSelectionDialog(
      dialogShower = AlertDialogShower(),
      dialogConfig = config
    )

    val device = mockk<StorageDevice>()

    sideEffect.handleStorageSelection(device)

    verify(exactly = 1) { callback(device) }
  }
}
