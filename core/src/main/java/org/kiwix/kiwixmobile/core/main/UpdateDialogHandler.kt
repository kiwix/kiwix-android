/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.main

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.dao.DownloadApkDao
import org.kiwix.kiwixmobile.core.utils.workManager.VersionId
import javax.inject.Inject

// set at 0 for testing
const val THREE_DAYS_IN_MILLISECONDS = 0L // 3 * 24 * 60 * 60 * 1000L

@Suppress("all")
class UpdateDialogHandler @Inject constructor(
  private val apkDao: DownloadApkDao
) {
  private var showUpdateDialogCallback: ShowUpdateDialogCallback? = null

  fun setUpdateDialogCallBack(showUpdateDialogCallback: ShowUpdateDialogCallback?) {
    this.showUpdateDialogCallback = showUpdateDialogCallback
  }

  suspend fun attemptToShowUpdatePopup() {
    CoroutineScope(Dispatchers.IO).launch {
      val currentMilliSeconds = System.currentTimeMillis()
      // hardcoded values for testing
      val currentVersion = VersionId("3.9.11")
      val available = VersionId("3.9.12")
      val lastPopupMillis = apkDao.getDownload()?.lastDialogShownInMilliSeconds ?: 0L
      val shouldShowPopup =
        (lastPopupMillis == 0L) ||
          isThreeDaysElapsed(currentMilliSeconds, lastPopupMillis)
      if (shouldShowPopup &&
        isTimeToShowUpdate(currentMilliSeconds) &&
        available > currentVersion
      ) {
        showUpdateDialogCallback?.showUpdateDialog()
        resetUpdateLater()
      }
    }
  }

  private fun isTimeToShowUpdate(currentMillis: Long): Boolean {
    val lastLaterClick = apkDao.getDownload()?.laterClickedMilliSeconds ?: 0L
    return lastLaterClick == 0L ||
      isThreeDaysElapsed(currentMillis, lastLaterClick)
  }

  private fun isThreeDaysElapsed(currentMilliSeconds: Long, lastPopupMillis: Long): Boolean {
    if (lastPopupMillis == 0L) return false
    val timeDifference = currentMilliSeconds - lastPopupMillis
    return timeDifference >= THREE_DAYS_IN_MILLISECONDS
  }

  fun updateLastUpdatePopupShownTime() {
    CoroutineScope(Dispatchers.IO).launch {
      apkDao.addLastDialogShownInfo(
        lastDialogShownInMilliSeconds = System.currentTimeMillis()
      )
    }
  }

  fun updateLater() {
    CoroutineScope(Dispatchers.IO).launch {
      apkDao.addLaterClickedInfo(
        laterClickedMilliSeconds = System.currentTimeMillis()
      )
    }
  }

  private fun resetUpdateLater() {
    CoroutineScope(Dispatchers.IO).launch {
      apkDao.addLaterClickedInfo(
        laterClickedMilliSeconds = System.currentTimeMillis()
      )
    }
  }

  interface ShowUpdateDialogCallback {
    fun showUpdateDialog()
  }
}
