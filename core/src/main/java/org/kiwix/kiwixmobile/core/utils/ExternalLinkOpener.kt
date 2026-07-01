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

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import javax.inject.Inject

class ExternalLinkOpener @Inject constructor(
  private val context: Context,
  private val kiwixDataStore: KiwixDataStore
) {
  private var alertDialogShower: AlertDialogShower? = null

  fun setAlertDialogShower(alertDialogShower: AlertDialogShower) {
    this.alertDialogShower = alertDialogShower
  }

  private fun requireAlertDialogShower() = requireNotNull(alertDialogShower) {
    "AlertDialogShower is not set. Call ExternalLinkOpener.setAlertDialogShower before using it"
  }

  suspend fun openExternalUrl(
    intent: Intent,
    lifecycleScope: CoroutineScope
  ) {
    if (intent.resolveActivity(context.packageManager) != null) {
      if (kiwixDataStore.externalLinkPopup.first()) {
        requestOpenLink(intent, lifecycleScope)
      } else {
        openLink(intent)
      }
    } else {
      context.toast(R.string.no_reader_application_installed)
    }
  }

  private fun openLink(intent: Intent) {
    context.startActivity(intent)
  }

  private fun requestOpenLink(intent: Intent, lifecycleScope: CoroutineScope) {
    requireAlertDialogShower().show(
      KiwixDialog.ExternalLinkPopup,
      { openLink(intent) },
      { },
      {
        lifecycleScope.launch {
          kiwixDataStore.setExternalLinkPopup(false)
          openLink(intent)
        }
      },
      uri = intent.data
    )
  }

  fun showTTSLanguageDownloadDialog() {
    requireAlertDialogShower().show(
      KiwixDialog.DownloadTTSLanguage,
      {
        context.startActivity(
          Intent().apply {
            action = ACTION_INSTALL_TTS_DATA
          }
        )
      }
    )
  }

  fun openExternalLinkWithDialog(
    intent: Intent,
    destinationText: String
  ) {
    if (intent.resolveActivity(context.packageManager) == null) {
      context.toast(R.string.no_reader_application_installed)
      return
    }

    requireAlertDialogShower().show(
      KiwixDialog.ExternalRedirectDialog(destinationText),
      {
        context.startActivity(intent)
      }
    )
  }
}
