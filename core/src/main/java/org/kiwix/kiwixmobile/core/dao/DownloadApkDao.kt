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

package org.kiwix.kiwixmobile.core.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tonyodev.fetch2.Download
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.kiwix.kiwixmobile.core.dao.entities.DownloadApkEntity
import org.kiwix.kiwixmobile.core.downloader.DownloadRequester
import org.kiwix.kiwixmobile.core.downloader.model.DownloadApkModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadRequest

@Dao
interface DownloadApkDao {
  fun update(download: Download) {
    getDownload().let { downloadApkEntity ->
      downloadApkEntity?.updateWith(download)
        .takeIf { updatedEntity -> updatedEntity != downloadApkEntity }
        ?.let { updateApkDownload(it) }
    }
  }

  fun resetDownloadInfoState() {
    getDownload().let { downloadApkEntity ->
      downloadApkEntity?.resetDownloadSate()
        ?.let { updateApkDownload(it) }
    }
  }

  @Query("SELECT * FROM downloadapkentity LIMIT 1")
  fun getActiveDownload(): Flow<DownloadApkEntity>

  fun downloads(): Flow<DownloadApkModel> =
    getActiveDownload()
      .distinctUntilChanged()
      .map { it.let(::DownloadApkModel) }

  @Query("UPDATE downloadapkentity SET downloadId = :downloadId WHERE id = 1")
  fun addDownloadId(downloadId: Long)

  suspend fun addDownload(
    url: String,
    downloadRequester: DownloadRequester
  ) {
    addDownloadId(
      downloadId = downloadRequester.enqueue(DownloadRequest(url))
    )
  }

  @Query("UPDATE downloadapkentity SET lastDialogShownInMilliSeconds = :lastDialogShownInMilliSeconds WHERE id = 1")
  fun addLastDialogShownInfo(lastDialogShownInMilliSeconds: Long)

  @Query("UPDATE downloadapkentity SET laterClickedMilliSeconds = :laterClickedMilliSeconds WHERE id = 1")
  fun addLaterClickedInfo(laterClickedMilliSeconds: Long)

  @Query("SELECT * FROM downloadapkentity LIMIT 1")
  fun getDownload(): DownloadApkEntity?

  @Update
  fun updateApkDownload(downloadApkEntity: DownloadApkEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun addApkDownload(downloadApkEntity: DownloadApkEntity)
}
