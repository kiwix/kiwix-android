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

package org.kiwix.kiwixmobile.zim_manager.fileselect_view.effects

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.startActionMode
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.DeviceTabActions
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.DeviceTabActions.MultiModeFinished
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.DeviceTabActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.zim_manager.ZimManageViewModel.DeviceTabActions.RequestShareMultiSelection

data class StartMultiSelection(
  val deviceTabActions: PublishProcessor<DeviceTabActions>
) : SideEffect<ActionMode?> {
  override fun invokeWith(activity: AppCompatActivity): ActionMode? =
    activity.startActionMode(
      R.menu.menu_zim_files_contextual,
      mapOf(
        R.id.zim_file_delete_item to { deviceTabActions.offer(RequestDeleteMultiSelection) },
        R.id.zim_file_share_item to { deviceTabActions.offer(RequestShareMultiSelection) }
      )
    ) { deviceTabActions.offer(MultiModeFinished) }
}
