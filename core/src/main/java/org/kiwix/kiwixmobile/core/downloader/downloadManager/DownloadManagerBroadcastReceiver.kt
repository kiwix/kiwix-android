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

package org.kiwix.kiwixmobile.core.downloader.downloadManager

import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.content.Context
import android.content.Intent
import org.kiwix.kiwixmobile.core.base.BaseBroadcastReceiver
import javax.inject.Inject

class DownloadManagerBroadcastReceiver @Inject constructor(private val callback: Callback) :
  BaseBroadcastReceiver() {
  // This broadcast will trigger when a download is completed or cancelled.
  override val action: String = ACTION_DOWNLOAD_COMPLETE

  override fun onIntentWithActionReceived(context: Context, intent: Intent) {
  }

  interface Callback {
    fun downloadInformation()
  }
}
