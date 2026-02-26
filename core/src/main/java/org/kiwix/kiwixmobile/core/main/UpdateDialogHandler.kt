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

import kotlinx.coroutines.flow.first
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.dao.DownloadApkDao
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.workManager.VersionId
import javax.inject.Inject

const val THREE_DAYS_IN_MILLISECONDS = 3 * 24 * 60 * 60 * 1000L

class UpdateDialogHandler @Inject constructor(
  private val apkDao: DownloadApkDao,
  private val kiwixDataStore: KiwixDataStore
) {
  private var showUpdateDialogCallback: ShowUpdateDialogCallback? = null

  fun setUpdateDialogCallBack(showUpdateDialogCallback: ShowUpdateDialogCallback?) {
    this.showUpdateDialogCallback = showUpdateDialogCallback
  }

  // need more refinement here, maybe convert some conditions to a function.
  @Suppress("ComplexCondition")
  suspend fun attemptToShowUpdatePopup() {
    val apkInfo = apkDao.getDownload() ?: return
    val currentMilliSeconds = System.currentTimeMillis()
    val lastPopupMillis = apkInfo.lastDialogShownInMilliSeconds
    val currentVersion = VersionId(BuildConfig.VERSION_NAME)
    val available = VersionId(apkInfo.version)
    val shouldShowPopup =
      (lastPopupMillis == 0L) ||
        isThreeDaysElapsed(currentMilliSeconds, lastPopupMillis)
    if (
      shouldShowPopup &&
      isTimeToShowUpdate(currentMilliSeconds) &&
      available > currentVersion &&
      !kiwixDataStore.isPlayStoreBuild.first()
    ) {
      showUpdateDialogCallback?.showUpdateDialog()
      resetUpdateLater()
    }
  }

  private suspend fun isTimeToShowUpdate(currentMillis: Long): Boolean {
    val lastLaterClick = apkDao.getDownload()?.laterClickedMilliSeconds ?: 0L
    return lastLaterClick == 0L ||
      isThreeDaysElapsed(currentMillis, lastLaterClick)
  }

  private fun isThreeDaysElapsed(currentMilliSeconds: Long, lastPopupMillis: Long): Boolean {
    if (lastPopupMillis == 0L) return false
    val timeDifference = currentMilliSeconds - lastPopupMillis
    return timeDifference >= THREE_DAYS_IN_MILLISECONDS
  }

  suspend fun updateLastUpdatePopupShownTime() {
    apkDao.addLastDialogShownInfo(
      lastDialogShownInMilliSeconds = System.currentTimeMillis()
    )
  }

  suspend fun updateLater() {
    apkDao.addLaterClickedInfo(
      laterClickedMilliSeconds = System.currentTimeMillis()
    )
  }

  private suspend fun resetUpdateLater() {
    apkDao.addLaterClickedInfo(
      laterClickedMilliSeconds = System.currentTimeMillis()
    )
  }

  interface ShowUpdateDialogCallback {
    fun showUpdateDialog()
  }
}
