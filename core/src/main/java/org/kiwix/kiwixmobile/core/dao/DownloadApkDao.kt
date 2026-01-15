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
import com.tonyodev.fetch2.Download
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.kiwix.kiwixmobile.core.dao.entities.DownloadApkEntity
import org.kiwix.kiwixmobile.core.downloader.DownloadRequester
import org.kiwix.kiwixmobile.core.downloader.model.DownloadApkModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadRequest
import org.kiwix.kiwixmobile.core.entity.ApkInfo

@Dao
interface DownloadApkDao {
  fun update(download: Download) {
    getApkDownload().let { downloadApkEntity ->
      downloadApkEntity?.updateWith(download)
        .takeIf { updatedEntity -> updatedEntity != downloadApkEntity }
        ?.let { updateApkDownload(it) }
    }
  }

  fun downloads(): Flow<DownloadApkModel> =
    getActiveDownload()
      .distinctUntilChanged()
      .map { it.let(::DownloadApkModel) }

  suspend fun addDownload(
    apkInfo: ApkInfo,
    downloadRequester: DownloadRequester
  ) {
    updateApkDownload(
      DownloadApkEntity(
        downloadId = downloadRequester.enqueue(DownloadRequest(apkInfo.apkUrl)),
        apkInfo = apkInfo
      )
    )
  }

  fun delete(download: Download) {
    deleteApkDownloadByDownloadId(download.id.toLong())
  }

  @Query("SELECT * FROM downloadapkentity LIMIT 1")
  fun getApkDownload(): DownloadApkEntity?

  @Query("SELECT * FROM downloadapkentity LIMIT 1")
  fun getActiveDownload(): Flow<DownloadApkEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun updateApkDownload(downloadApkEntity: DownloadApkEntity)

  @Query("DELETE FROM DownloadApkEntity WHERE downloadId=:downloadId")
  fun deleteApkDownloadByDownloadId(downloadId: Long)
}
