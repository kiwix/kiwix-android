package org.kiwix.kiwixmobile.core.history.viewmodel.effects/*
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

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_FILE
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_URL

data class OpenHistoryItem(
  private val historyItem: HistoryItem,
  private val zimReaderContainer: ZimReaderContainer
) : SideEffect<Unit> {
  override fun invokeWith(activity: AppCompatActivity) {
    val intent = Intent().putExtra(EXTRA_CHOSE_X_URL, historyItem.historyUrl)
    if (historyItem.zimFilePath != zimReaderContainer.zimCanonicalPath) {
      intent.putExtra(EXTRA_CHOSE_X_FILE, historyItem.zimFilePath)
    }
    activity.setResult(
      Activity.RESULT_OK,
      intent
    )
    activity.finish()
  }
}
