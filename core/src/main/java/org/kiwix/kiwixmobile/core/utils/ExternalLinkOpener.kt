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

package org.kiwix.kiwixmobile.core.utils

import android.app.Activity
import android.content.Intent
import androidx.core.content.ContextCompat
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.toast
import javax.inject.Inject

class ExternalLinkOpener @Inject constructor(
  private val activity: Activity,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val alertDialogShower: AlertDialogShower
) {

  fun openExternalUrl(intent: Intent) {
    if (intent.resolveActivity(activity.packageManager) != null) {
      // Show popup with warning that this url is external and could lead to additional costs
      // or may event not work when the user is offline.
      if (sharedPreferenceUtil.prefExternalLinkPopup) {
        requestOpenLink(intent)
      } else {
        openLink(intent)
      }
    } else {
      val error = activity.getString(R.string.no_reader_application_installed)
      activity.toast(error)
    }
  }

  private fun openLink(intent: Intent) {
    activity.startActivity(intent)
  }

  private fun requestOpenLink(intent: Intent) {
    alertDialogShower.show(
      KiwixDialog.ExternalLinkPopup, { ContextCompat.startActivity(activity, intent, null) },
      {}, {
        sharedPreferenceUtil.putPrefExternalLinkPopup(false)
        ContextCompat.startActivity(activity, intent, null)
      })
  }
}
