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

package org.kiwix.kiwixmobile.core.utils

import android.app.Activity
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isCustomApp
import javax.inject.Inject

const val THREE_DAYS_IN_MILLISECONDS = 3 * 24 * 60 * 60 * 1000L
const val THREE_MONTHS_IN_MILLISECONDS = 90 * 24 * 60 * 60 * 1000L

class DonationDialogHandler @Inject constructor(
  private val activity: Activity,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val newBookDao: NewBookDao
) {

  private var showDonationDialogCallback: ShowDonationDialogCallback? = null

  fun setDonationDialogCallBack(showDonationDialogCallback: ShowDonationDialogCallback?) {
    this.showDonationDialogCallback = showDonationDialogCallback
  }

  suspend fun attemptToShowDonationPopup() {
    val currentMilliSeconds = System.currentTimeMillis()
    val lastPopupMillis = sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds
    val timeDifference = currentMilliSeconds - lastPopupMillis
    if (shouldShowInitialPopup(lastPopupMillis) || timeDifference >= THREE_MONTHS_IN_MILLISECONDS) {
      if (isZimFilesAvailableInLibrary() && isTimeToShowDonation(currentMilliSeconds)) {
        showDonationDialogCallback?.showDonationDialog()
        resetDonateLater()
      }
    }
  }

  private suspend fun shouldShowInitialPopup(lastPopupMillis: Long): Boolean =
    lastPopupMillis == 0L && isZimFilesAvailableInLibrary()

  private fun isTimeToShowDonation(currentMillis: Long): Boolean =
    isLaterNotClicked() || isLaterPeriodOver(currentMillis)

  private fun isLaterNotClicked(): Boolean = sharedPreferenceUtil.laterClickedMilliSeconds == 0L

  private fun isLaterPeriodOver(currentMillis: Long): Boolean {
    val timeDifference = currentMillis - sharedPreferenceUtil.laterClickedMilliSeconds
    return timeDifference >= THREE_DAYS_IN_MILLISECONDS
  }

  suspend fun isZimFilesAvailableInLibrary(): Boolean =
    if (activity.isCustomApp()) true else newBookDao.getBooks().isNotEmpty()

  fun updateLastDonationPopupShownTime() {
    sharedPreferenceUtil.lastDonationPopupShownInMilliSeconds = System.currentTimeMillis()
  }

  fun donateLater(currentMillis: Long = System.currentTimeMillis()) {
    sharedPreferenceUtil.laterClickedMilliSeconds = currentMillis
  }

  fun resetDonateLater() {
    sharedPreferenceUtil.laterClickedMilliSeconds = 0L
  }

  interface ShowDonationDialogCallback {
    fun showDonationDialog()
  }
}
