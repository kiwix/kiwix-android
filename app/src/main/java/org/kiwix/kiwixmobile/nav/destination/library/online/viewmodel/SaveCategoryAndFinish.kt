/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel

import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.zim_manager.Category

data class SaveCategoryAndFinish(
  val category: Category,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val lifecycleScope: CoroutineScope
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    lifecycleScope.launch {
      runCatching {
        sharedPreferenceUtil.selectedOnlineContentCategory = category.category
        activity.onBackPressedDispatcher.onBackPressed()
      }.onFailure {
        it.printStackTrace()
      }
    }
  }
}
