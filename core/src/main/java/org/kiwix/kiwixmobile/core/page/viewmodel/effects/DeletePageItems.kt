/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.page.viewmodel.effects

import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.BasePageDao
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.viewmodel.PageState

data class DeletePageItems(
  private val state: PageState<*>,
  private val pageDao: BasePageDao,
  private val coroutineScope: CoroutineScope
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    if (state.isInSelectionState) {
      pageDao.deletePages(state.pageItems.filter(Page::isSelected))
    } else {
      coroutineScope.launch(Dispatchers.IO) {
        if (state.isInSelectionState) {
          pageDao.deletePages(state.pageItems.filter(Page::isSelected))
        } else {
          pageDao.deletePages(state.pageItems)
        }
      }
    }
  }
}
