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

package org.kiwix.kiwixmobile.core.extensions

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import org.kiwix.kiwixmobile.core.utils.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.EXTRA_EXTERNAL_LINK
import org.kiwix.kiwixmobile.core.utils.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

fun Uri.browserIntent(
  sharedPreferenceUtil: SharedPreferenceUtil,
  alertDialogShower: AlertDialogShower,
  context: Context
) {
  val intent = Intent(Intent.ACTION_VIEW, this).putExtra(EXTRA_EXTERNAL_LINK, true)
  if (sharedPreferenceUtil.prefExternalLinkPopup) {
    alertDialogShower.show(
      KiwixDialog.ExternalLinkPopup, { startActivity(context, intent, null) },
      {},
      {
        sharedPreferenceUtil.putPrefExternalLinkPopup(false)
        startActivity(context, intent, null)
      })
  } else {
    startActivity(context, intent, null)
  }
}
