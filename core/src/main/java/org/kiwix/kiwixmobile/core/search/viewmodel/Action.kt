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

package org.kiwix.kiwixmobile.core.search.viewmodel

import android.content.Intent
import org.kiwix.kiwixmobile.core.search.adapter.SearchListItem

sealed class Action {
  object ExitedSearch : Action()
  object ClickedSearchInText : Action()
  object ReceivedPromptForSpeechInput : Action()
  object StartSpeechInputFailed : Action()

  data class OnItemClick(val searchListItem: SearchListItem) : Action()
  data class OnItemLongClick(val searchListItem: SearchListItem) : Action()
  data class Filter(val term: String) : Action()
  data class ConfirmedDelete(val searchListItem: SearchListItem) : Action()
  data class CreatedWithIntent(val intent: Intent?) : Action()
  data class ActivityResultReceived(val requestCode: Int, val resultCode: Int, val data: Intent?) :
    Action()
}
