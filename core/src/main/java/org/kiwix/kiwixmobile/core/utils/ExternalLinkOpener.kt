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
import android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import javax.inject.Inject

class ExternalLinkOpener @Inject constructor(
  private val activity: Activity,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val alertDialogShower: AlertDialogShower
) {

  fun openExternalUrl(intent: Intent, errorMessageId: Int) {
    if (intent.resolveActivity(activity.packageManager) != null) {
      // Show popup with warning that this url is external and could lead to additional costs
      // or may event not work when the user is offline.
      if (sharedPreferenceUtil.prefExternalLinkPopup) {
        requestOpenLink(intent)
      } else {
        openLink(intent)
      }
    } else {
      activity.toast(errorMessageId)
    }
  }

  private fun openLink(intent: Intent) {
    activity.startActivity(intent)
  }

  private fun requestOpenLink(intent: Intent) {
    alertDialogShower.show(
      KiwixDialog.ExternalLinkPopup,
      { openLink(intent) },
      { },
      {
        sharedPreferenceUtil.putPrefExternalLinkPopup(false)
        openLink(intent)
      },
      uri = intent.data
    )
  }

  fun showTTSLanguageDownloadDialog() {
    alertDialogShower.show(
      KiwixDialog.DownloadTTSLanguage,
      {
        activity.startActivity(
          Intent().apply {
            action = ACTION_INSTALL_TTS_DATA
          }
        )
      }
    )
  }
}
