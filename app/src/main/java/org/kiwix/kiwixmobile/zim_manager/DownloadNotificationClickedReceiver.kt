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
package org.kiwix.kiwixmobile.zim_manager

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import org.kiwix.kiwixmobile.KiwixApplication
import org.kiwix.kiwixmobile.database.DownloadDao
import javax.inject.Inject

class DownloadNotificationClickedReceiver : BaseBroadcastReceiver() {
  override val action: String = DownloadManager.ACTION_NOTIFICATION_CLICKED

  @Inject lateinit var downloadDao: DownloadDao

  override fun onIntentWithActionReceived(
    context: Context,
    intent: Intent
  ) {
    KiwixApplication.getApplicationComponent()
        .inject(this)
    val longArray =
      intent.extras?.getLongArray(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS)
          ?: longArrayOf()
    if (downloadDao.containsAny(longArray.toTypedArray())) {
      context.startActivity(
          Intent(context, ZimManageActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(ZimManageActivity.TAB_EXTRA, 2)
          }
      )
    }
  }
}
