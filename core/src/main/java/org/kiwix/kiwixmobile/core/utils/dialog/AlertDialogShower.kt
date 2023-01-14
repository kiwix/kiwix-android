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
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Color
import android.text.Html
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import java.net.URL
import javax.inject.Inject

@Suppress("MagicNumber")
class AlertDialogShower @Inject constructor(private val activity: Activity) :
  DialogShower {
  private val viewSpacingLeftForLink = 0
  private val viewSpacingRightForLink = 0
  private val viewSpacingTopForLink = 10
  private val viewSpacingBottomForLink = 0

  override fun show(dialog: KiwixDialog, vararg clickListeners: () -> Unit, url: URL?) =
    create(dialog, *clickListeners, url = url).show()

  override fun create(dialog: KiwixDialog, vararg clickListeners: () -> Unit, url: URL?): Dialog {
    return AlertDialog.Builder(activity)
      .apply {
        dialog.title?.let(this::setTitle)
        dialog.icon?.let(this::setIcon)

        dialog.message?.let { setMessage(activity.getString(it, *bodyArguments(dialog))) }
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
        if (url != null) {
          val textView = TextView(activity.baseContext)
          textView.setPadding(5, 5, 5, 5)
          textView.gravity = Gravity.CENTER
          textView.setLinkTextColor(Color.BLUE)
          textView.setOnLongClickListener {
            val clipboard =
              ContextCompat.getSystemService(activity.baseContext, ClipboardManager::class.java)
            val clip = ClipData.newPlainText("External Url", "$url")
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(
              activity.baseContext,
              "External Link Copied to Clipboard",
              Toast.LENGTH_SHORT
            ).show()
            true
          }
          textView.text = Html.fromHtml("</br><a href=$url> <b>$url</b>")

          setView(
            textView,
            viewSpacingLeftForLink,
            viewSpacingTopForLink,
            viewSpacingRightForLink,
            viewSpacingBottomForLink
          )
        }
        dialog.getView?.let { setView(it()) }
        setCancelable(dialog.cancelable)
      }
      .create()
  }

  private fun bodyArguments(dialog: KiwixDialog) =
    if (dialog is HasBodyFormatArgs) dialog.args.toTypedArray()
    else emptyArray()
}
