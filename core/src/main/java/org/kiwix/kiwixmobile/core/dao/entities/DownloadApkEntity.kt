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

package org.kiwix.kiwixmobile.core.dao.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.Status
import org.kiwix.kiwixmobile.core.entity.ApkInfo

@Entity
data class DownloadApkEntity(
  // Only one instance of download apk remains in the db at a time
  @PrimaryKey
  val id: Int = 1,
  val name: String,
  val version: String,
  val url: String,
  var lastDialogShownInMilliSeconds: Long = -1L,
  var laterClickedMilliSeconds: Long = -1L,
  var downloadId: Long,
  val file: String? = null,
  val etaInMilliSeconds: Long = -1L,
  val bytesDownloaded: Long = -1L,
  val totalSizeOfDownload: Long = -1L,
  val progress: Int = -1,
  @TypeConverters(StatusConverter::class)
  val status: Status = Status.NONE,
  @TypeConverters(ErrorConverter::class)
  val error: com.tonyodev.fetch2.Error = Error.NONE,
) {
  constructor(
    downloadId: Long,
    apkInfo: ApkInfo,
  ) : this(
    downloadId = downloadId,
    name = apkInfo.name,
    version = apkInfo.version,
    url = apkInfo.apkUrl
  )

  fun updateWith(download: Download) =
    copy(
      file = download.file,
      etaInMilliSeconds = download.etaInMilliSeconds,
      bytesDownloaded = download.downloaded,
      totalSizeOfDownload = download.total,
      progress = download.progress,
      status = download.status,
      error = download.error,
    )
}
