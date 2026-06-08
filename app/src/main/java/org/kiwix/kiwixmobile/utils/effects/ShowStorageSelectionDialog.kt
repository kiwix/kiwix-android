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
import androidx.compose.ui.unit.dp
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.StorageSelectionDialog
import org.kiwix.kiwixmobile.nav.destination.library.StorageSelectDialogConfig
import org.kiwix.kiwixmobile.storage.StorageSelectDialogScreen

class ShowStorageSelectionDialog(
  private val dialogShower: AlertDialogShower,
  private val dialogConfig: StorageSelectDialogConfig
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    dialogShower.show(
      StorageSelectionDialog(
        ZERO.dp,
        {
          StorageSelectDialogScreen(
            title = dialogConfig.title,
            titleSize = dialogConfig.titleSize,
            storageDeviceList = dialogConfig.storageDeviceList,
            storageCalculator = dialogConfig.storageCalculator,
            kiwixDataStore = dialogConfig.kiwixDataStore,
            shouldShowStorageSelected = false,
            onSelectAction = {
              dialogConfig.onSelectAction.invoke(it)
              dialogShower.dismiss()
            }
          )
        }
      )
    )
  }
}
