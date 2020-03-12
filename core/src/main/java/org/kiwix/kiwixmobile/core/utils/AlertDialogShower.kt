/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
import androidx.appcompat.app.AlertDialog
import javax.inject.Inject

class AlertDialogShower @Inject constructor(private val activity: Activity) : DialogShower {
  override fun show(dialog: KiwixDialog, vararg clickListeners: () -> Unit) {
    AlertDialog.Builder(activity)
      .apply {
        dialog.title?.let(this::setTitle)
        dialog.icon?.let(this::setIcon)
        dialog.message?.let {
          setMessage(
            activity.getString(
              it,
              *bodyArguments(dialog)
            )
          )
        }
        setPositiveButton(dialog.positiveMessage) { _, _ ->
          clickListeners.getOrNull(0)
            ?.invoke()
        }
        dialog.negativeMessage?.let {
          setNegativeButton(it) { _, _ ->
            clickListeners.getOrNull(1)
              ?.invoke()
          }
        }
        dialog.neutralMessage?.let {
          setNeutralButton(it) { _, _ ->
            clickListeners.getOrNull(2)
              ?.invoke()
          }
        }
        dialog.getView?.let { setView(it()) }
        setCancelable(dialog.cancelable)
      }
      .show()
  }

  private fun bodyArguments(dialog: KiwixDialog) =
    if (dialog is HasBodyFormatArgs) dialog.args.toTypedArray()
    else emptyArray()
}
