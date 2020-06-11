package org.kiwix.kiwixmobile.core.history.viewmodel.effects

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

import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.history.viewmodel.HistoryState

data class DeleteHistoryItems(
  private val state: HistoryState,
  private val historyDao: HistoryDao
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    if (state.isInSelectionState) {
      historyDao.deleteHistory(state.historyItems.filter(HistoryItem::isSelected))
    } else {
      historyDao.deleteHistory(state.historyItems)
    }
  }
}
