/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.database.newdb.dao

import io.objectbox.Box
import io.objectbox.kotlin.query
import org.kiwix.kiwixmobile.data.ZimContentProvider
import org.kiwix.kiwixmobile.database.newdb.entities.HistoryEntity
import org.kiwix.kiwixmobile.database.newdb.entities.HistoryEntity_
import org.kiwix.kiwixmobile.history.HistoryListItem.HistoryItem
import javax.inject.Inject

class HistoryDao @Inject constructor(val box: Box<HistoryEntity>) {

  fun history() = box.asFlowable()
    .map { it.map(::HistoryItem) }

  fun saveHistory(historyItem: HistoryItem) {
    box.store.callInTx {
      box
        .query {
          equal(HistoryEntity_.historyUrl, historyItem.historyUrl).and()
            .equal(HistoryEntity_.dateString, historyItem.dateString)
        }
        .remove()
      box.put(HistoryEntity(historyItem))
    }
  }

  fun getHistoryList(showHistoryCurrentBook: Boolean) = box
    .query {
      if (showHistoryCurrentBook) {
        equal(HistoryEntity_.zimFilePath, ZimContentProvider.getZimFile() ?: "")
      }
      orderDesc(HistoryEntity_.timeStamp)
    }
    .find()
    .map(::HistoryItem)

  fun deleteHistory(historyList: List<HistoryItem>) {
    box.remove(historyList.map(::HistoryEntity))
  }
}
