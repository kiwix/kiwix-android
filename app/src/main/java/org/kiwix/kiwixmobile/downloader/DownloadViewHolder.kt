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
package org.kiwix.kiwixmobile.downloader

import android.content.Context

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.download_item.description
import kotlinx.android.synthetic.main.download_item.downloadProgress
import kotlinx.android.synthetic.main.download_item.downloadState
import kotlinx.android.synthetic.main.download_item.favicon
import kotlinx.android.synthetic.main.download_item.stop
import kotlinx.android.synthetic.main.download_item.title
import org.kiwix.kiwixmobile.downloader.model.DownloadItem
import org.kiwix.kiwixmobile.downloader.model.DownloadState
import org.kiwix.kiwixmobile.downloader.model.DownloadState.Failed
import org.kiwix.kiwixmobile.downloader.model.DownloadState.Paused
import org.kiwix.kiwixmobile.downloader.model.DownloadState.Pending
import org.kiwix.kiwixmobile.downloader.model.DownloadState.Running
import org.kiwix.kiwixmobile.downloader.model.DownloadState.Successful
import org.kiwix.kiwixmobile.downloader.model.FailureReason.Rfc2616HttpCode
import org.kiwix.kiwixmobile.extensions.setBitmap

class DownloadViewHolder(override val containerView: View) : RecyclerView.ViewHolder(containerView),
  LayoutContainer {
  fun bind(
    downloadItem: DownloadItem,
    itemClickListener: (DownloadItem) -> Unit
  ) {
    favicon.setBitmap(downloadItem.favIcon)
    title.text = downloadItem.title
    description.text = downloadItem.description
    downloadProgress.progress = downloadItem.progress
    stop.setOnClickListener {
      itemClickListener.invoke(downloadItem)
    }
    downloadState.text = toReadableState(
      downloadItem.downloadState, containerView.context
    )
  }

  private fun toReadableState(
    downloadState: DownloadState,
    context: Context
  ) = when (downloadState) {
    is Paused -> context.getString(
      downloadState.stringId,
      context.getString(downloadState.reason.stringId)
    )
    is Failed -> context.getString(
      downloadState.stringId,
      getTemplateString(downloadState, context)
    )
    Pending,
    Running,
    Successful -> context.getString(downloadState.stringId)
  }

  private fun getTemplateString(
    downloadState: Failed,
    context: Context
  ) = when (downloadState.reason) {
    is Rfc2616HttpCode -> context.getString(
      downloadState.reason.stringId,
      downloadState.reason.code
    )
    else -> context.getString(downloadState.reason.stringId)
  }
}
