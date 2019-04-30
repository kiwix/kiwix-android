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

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.download_item.description
import kotlinx.android.synthetic.main.download_item.downloadProgress
import kotlinx.android.synthetic.main.download_item.favicon
import kotlinx.android.synthetic.main.download_item.stop
import kotlinx.android.synthetic.main.download_item.title
import org.kiwix.kiwixmobile.downloader.model.Base64String
import org.kiwix.kiwixmobile.downloader.model.DownloadItem

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
  }

  private fun ImageView.setBitmap(base64String: Base64String) {
    if (tag != base64String) {
      base64String.toBitmap()
          ?.let {
            setImageBitmap(it)
            tag = base64String
          }
    }
  }
}