/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.reactivex.Flowable
import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity

@Dao
abstract class WebViewHistoryRoomDao {
  @Insert
  abstract fun insertWebViewPageHistoryItem(webViewHistoryEntity: WebViewHistoryEntity)

  @Query("SELECT * FROM WebViewHistoryEntity ORDER BY isForward ASC,timestamp ASC")
  abstract fun getAllWebViewPagesHistory(): Flowable<List<WebViewHistoryEntity>>

  @Query("Delete from WebViewHistoryEntity")
  abstract fun clearWebViewPagesHistory()

  fun clearPageHistoryWithPrimaryKey() {
    clearWebViewPagesHistory()
  }

  @Query("DELETE FROM sqlite_sequence WHERE name='PageHistoryRoomEntity'")
  abstract fun resetPrimaryKey()
}
