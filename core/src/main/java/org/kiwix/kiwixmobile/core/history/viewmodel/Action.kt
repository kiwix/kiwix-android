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

package org.kiwix.kiwixmobile.core.history.viewmodel

import android.content.Intent
import android.widget.ImageView
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem

sealed class Action {
  object ExitHistory : Action()
  object ClickedSearchInText : Action()
  object ReceivedPromptForSpeechInput : Action()
  object ExitActionModeMenu : Action()

  object StartSpeechInputFailed : Action()
  data class OnItemClick(val historyListItem: HistoryListItem) : Action()
  data class OnItemLongClick(val historyItem: HistoryItem) : Action()
  data class ToggleShowHistoryFromAllBooks(val isChecked: Boolean) : Action()
  data class Filter(val searchTerm: String) : Action()
  data class UserClickedItem(val historyItem: HistoryItem) : Action()
  data class ShowAllSwitchToggled(val isToggled: Boolean) : Action()
  data class ConfirmedDelete(val historyListItems: List<HistoryListItem.HistoryItem>) : Action()
  data class CreatedWithIntent(val searchTerm: String) : Action()
  data class ActivityResultReceived(val requestCode: Int, val resultCode: Int, val data: Intent?) :
    Action()
}
