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
import android.net.Uri
import androidx.annotation.IdRes
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.di.ActivityScope
import org.kiwix.kiwixmobile.core.utils.NetworkUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

const val VISITS_REQUIRED_TO_SHOW_RATE_DIALOG = 10

@ActivityScope
class RateDialogHandler @Inject constructor(
  private val activity: Activity,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val alertDialogShower: AlertDialogShower
) {
  private var visitCounterPref: RateAppCounter? = null
  private var tempVisitCount = 0
  private var isFirstRun = false

  private fun showRateDialog(iconResId: Int) {
    alertDialogShower.show(
      KiwixDialog.ShowRate(iconResId, activity),
      {
        visitCounterPref?.noThanksState = true
        goToRateApp(activity)
      },
      {
        visitCounterPref?.noThanksState = true
      },
      {
        tempVisitCount = 0
        visitCounterPref?.count = tempVisitCount
      }
    )
  }

  fun checkForRateDialog(@IdRes iconResId: Int) {
    isFirstRun = sharedPreferenceUtil.prefIsFirstRun
    visitCounterPref = RateAppCounter(activity)
    tempVisitCount = visitCounterPref?.count ?: 0
    ++tempVisitCount
    visitCounterPref?.count = tempVisitCount
    if (shouldShowRateDialog() && NetworkUtils.isNetworkAvailable(activity)) {
      showRateDialog(iconResId)
    }
  }

  private fun shouldShowRateDialog(): Boolean {
    return tempVisitCount >= VISITS_REQUIRED_TO_SHOW_RATE_DIALOG &&
      visitCounterPref?.noThanksState == false && !BuildConfig.DEBUG
  }

  private fun goToRateApp(activity: Activity) {
    val kiwixLocalMarketUri =
      Uri.parse("market://details?id=${activity.packageName}")
    val kiwixBrowserMarketUri =
      Uri.parse("http://play.google.com/store/apps/details?id=${activity.packageName}")
    val goToMarket = Intent(Intent.ACTION_VIEW, kiwixLocalMarketUri)
    goToMarket.addFlags(
      Intent.FLAG_ACTIVITY_NO_HISTORY or
        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
    )
    try {
      activity.startActivity(goToMarket)
    } catch (e: ActivityNotFoundException) {
      activity.startActivity(Intent(Intent.ACTION_VIEW, kiwixBrowserMarketUri))
    }
  }
}
