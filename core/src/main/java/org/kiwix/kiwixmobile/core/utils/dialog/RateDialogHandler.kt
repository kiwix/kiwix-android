/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.utils.dialog

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getPackageInformation
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.di.ActivityScope
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isBrandedApp
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import javax.inject.Inject

const val VISITS_REQUIRED_TO_SHOW_RATE_DIALOG = 20
const val READING_MILESTONE_THRESHOLD = 10

@ActivityScope
class RateDialogHandler @Inject constructor(
  private val activity: Activity,
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk,
  private val kiwixDataStore: KiwixDataStore
) {
  private var alertDialogShower: AlertDialogShower? = null

  fun setAlertDialogShower(alertDialogShower: AlertDialogShower) {
    this.alertDialogShower = alertDialogShower
  }

  fun checkForRateDialog() {
    (activity as CoreMainActivity).lifecycleScope.launch {
      val currentCount = kiwixDataStore.rateAppCount.first()
      val newCount = currentCount + 1
      kiwixDataStore.setRateAppCount(newCount)

      if (shouldShowRateDialog(newCount) && NetworkUtils.isNetworkAvailable(activity)) {
        kiwixDataStore.resetRateAppTriggers()
        launchInAppReviewFlow()
      }
    }
  }

  internal suspend fun shouldShowRateDialog(newCount: Int): Boolean {
    val meetVisitCount = newCount >= VISITS_REQUIRED_TO_SHOW_RATE_DIALOG
    val meetDownload = kiwixDataStore.rateAppDownloadCompleted.first()
    val meetReading = kiwixDataStore.rateAppReadingCount.first() >= READING_MILESTONE_THRESHOLD

    return isPlayStoreVariant() &&
      (meetVisitCount || meetDownload || meetReading) &&
      isTwoWeekPassed() &&
      isZimFilesAvailableInLibrary()
  }

  internal suspend fun isPlayStoreVariant(): Boolean =
    kiwixDataStore.isPlayStoreBuild.first()

  internal suspend fun isZimFilesAvailableInLibrary(): Boolean {
    // If it is a custom app, return true since custom apps always have the ZIM file.
    if (activity.isBrandedApp()) return true
    // For Kiwix app, check if there are ZIM files available in the library.
    return libkiwixBookOnDisk.getBooks().isNotEmpty()
  }

  @Suppress("MagicNumber")
  internal fun isTwoWeekPassed(): Boolean {
    val firstTimeInstallTime =
      activity.packageManager
        .getPackageInformation(activity.packageName, 0).firstInstallTime
    val timeDifference = System.currentTimeMillis() - firstTimeInstallTime
    val twoWeeksInMillis = 14 * 24 * 60 * 60 * 1000L
    // Check if the time difference is at least 2 weeks
    return timeDifference >= twoWeeksInMillis
  }

  @Suppress("TooGenericExceptionCaught")
  internal fun launchInAppReviewFlow() {
    try {
      val reviewManager = ReviewManagerFactory.create(activity)
      reviewManager.requestReviewFlow()
        .addOnCompleteListener { requestTask ->
          if (requestTask.isSuccessful) {
            val reviewInfo = requestTask.result
            reviewManager.launchReviewFlow(activity, reviewInfo)
          } else {
            Log.e(TAG, "Failed to request review flow", requestTask.exception)
            goToRateApp()
          }
        }
    } catch (exception: Exception) {
      Log.e(TAG, "Unexpected error while launching in-app review", exception)
      goToRateApp()
    }
  }

  internal fun goToRateApp() {
    val kiwixLocalMarketUri =
      "market://details?id=${activity.packageName}".toUri()
    val kiwixBrowserMarketUri =
      "http://play.google.com/store/apps/details?id=${activity.packageName}".toUri()
    val goToMarket = Intent(Intent.ACTION_VIEW, kiwixLocalMarketUri)
    goToMarket.addFlags(
      Intent.FLAG_ACTIVITY_NO_HISTORY or
        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
    )
    try {
      activity.startActivity(goToMarket)
    } catch (_: ActivityNotFoundException) {
      activity.startActivity(Intent(Intent.ACTION_VIEW, kiwixBrowserMarketUri))
    }
  }

  companion object {
    private const val TAG = "RateDialogHandler"
  }
}
