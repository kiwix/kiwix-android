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

package org.kiwix.kiwixmobile.nav.destination.library.local

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog

class ShowFileSystemScanDialog(
  private val dialogShower: AlertDialogShower,
  private val coroutineScope: CoroutineScope,
  private val kiwixDataStore: KiwixDataStore,
  private val scanFileSystem: suspend () -> Unit
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    dialogShower.show(
      KiwixDialog.YesNoDialog.FileSystemScan,
      {
        coroutineScope.launch {
          // Sets true so that it can not show again.
          kiwixDataStore.setIsScanFileSystemDialogShown(true)
          scanFileSystem.invoke()
        }
      },
      {
        coroutineScope.launch {
          // User clicks on the "No" button so not show again.
          kiwixDataStore.setIsScanFileSystemDialogShown(true)
        }
      }
    )
  }
}
