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

package org.kiwix.kiwixmobile.core.page.history.viewmodel

import androidx.lifecycle.viewModelScope
import org.kiwix.kiwixmobile.core.dao.HistoryDao
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.ShowDeleteHistoryDialog
import org.kiwix.kiwixmobile.core.page.history.viewmodel.effects.UpdateAllHistoryPreference
import org.kiwix.kiwixmobile.core.page.viewmodel.Action
import org.kiwix.kiwixmobile.core.page.viewmodel.PageViewModel
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
  historyDao: HistoryDao,
  zimReaderContainer: ZimReaderContainer,
  sharedPrefs: SharedPreferenceUtil
) : PageViewModel<HistoryItem, HistoryState>(historyDao, sharedPrefs, zimReaderContainer) {

  override fun initialState(): HistoryState =
    HistoryState(emptyList(), sharedPreferenceUtil.showHistoryAllBooks, zimReaderContainer.id)

  override fun updatePagesBasedOnFilter(
    state: HistoryState,
    action: Action.Filter
  ): HistoryState =
    state.copy(searchTerm = action.searchTerm)

  override fun updatePages(
    state: HistoryState,
    action: Action.UpdatePages
  ): HistoryState =
    state.copy(pageItems = action.pages.filterIsInstance<HistoryItem>())

  override fun offerUpdateToShowAllToggle(
    action: Action.UserClickedShowAllToggle,
    state: HistoryState
  ): HistoryState {
    effects.offer(UpdateAllHistoryPreference(sharedPreferenceUtil, action.isChecked))
    return state.copy(showAll = action.isChecked)
  }

  override fun createDeletePageDialogEffect(state: HistoryState) =
    ShowDeleteHistoryDialog(effects, state, pageDao, viewModelScope)

  override fun deselectAllPages(state: HistoryState): HistoryState =
    state.copy(pageItems = state.pageItems.map { it.copy(isSelected = false) })

  override fun copyWithNewItems(state: HistoryState, newItems: List<HistoryItem>): HistoryState =
    state.copy(pageItems = newItems)
}
