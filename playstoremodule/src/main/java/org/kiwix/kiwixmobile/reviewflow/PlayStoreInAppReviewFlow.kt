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

package org.kiwix.kiwixmobile.reviewflow

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import org.kiwix.kiwixmobile.core.utils.dialog.InAppReviewFlow
import javax.inject.Inject

class PlayStoreInAppReviewFlow @Inject constructor(
  private val activity: Activity
) : InAppReviewFlow {
  override fun launch() {
    runCatching {
      val reviewManager = ReviewManagerFactory.create(activity)
      reviewManager.requestReviewFlow()
        .addOnCompleteListener { requestTask ->
          if (requestTask.isSuccessful) {
            val reviewInfo = requestTask.result
            reviewManager.launchReviewFlow(activity, reviewInfo)
          } else {
            Log.e(TAG, "Failed to request review flow", requestTask.exception)
          }
        }
    }.onFailure {
      Log.e(TAG, "Unexpected error while launching in-app review", it)
    }
  }

  companion object {
    private const val TAG = "PlayStoreInAppReviewFlow"
  }
}
