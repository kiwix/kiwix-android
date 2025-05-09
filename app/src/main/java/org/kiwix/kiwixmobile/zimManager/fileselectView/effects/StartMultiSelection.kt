/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.startActionMode
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.MultiModeFinished
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.zimManager.ZimManageViewModel.FileSelectActions.RequestShareMultiSelection

data class StartMultiSelection(
  private val fileSelectActions: MutableSharedFlow<FileSelectActions>
) : SideEffect<ActionMode?> {
  override fun invokeWith(activity: AppCompatActivity): ActionMode? {
    return activity.startActionMode(
      R.menu.menu_zim_files_contextual,
      mapOf(
        R.id.zim_file_delete_item to {
          activity.lifecycleScope.launch {
            fileSelectActions.emit(RequestDeleteMultiSelection)
          }
        },
        R.id.zim_file_share_item to {
          activity.lifecycleScope.launch {
            fileSelectActions.emit(RequestShareMultiSelection)
          }
        }
      )
    ) {
      activity.lifecycleScope.launch {
        fileSelectActions.emit(MultiModeFinished)
      }
    }
  }
}
