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

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.history.viewmodel.State

data class DeleteSelectedOrAllHistoryItems(
  private val state: MutableLiveData<State>,
  private val historyDao: HistoryDao
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    val historyItems = state.value?.historyItems?.filterIsInstance<HistoryItem>()
    if(historyItems?.any { it.isSelected } == true){
      historyDao.deleteHistory(historyItems.filter { it.isSelected })
    } else if(historyItems != null){
      historyDao.deleteHistory(historyItems)
    }
  }
}
