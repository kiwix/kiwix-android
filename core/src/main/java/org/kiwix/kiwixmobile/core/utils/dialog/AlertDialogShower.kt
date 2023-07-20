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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.getAttribute
import org.kiwix.kiwixmobile.core.utils.StyleUtils.fromHtml
import javax.inject.Inject

class AlertDialogShower @Inject constructor(private val activity: Activity) :
  DialogShower {
  companion object {
    const val externalLinkLeftMargin = 10
    const val externalLinkRightMargin = 10
    const val externalLinkTopMargin = 10
    const val externalLinkBottomMargin = 0
  }

  override fun show(dialog: KiwixDialog, vararg clickListeners: () -> Unit, uri: Uri?) =
    create(dialog, *clickListeners, uri = uri).show()

  override fun create(dialog: KiwixDialog, vararg clickListeners: () -> Unit, uri: Uri?): Dialog {
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
        uri?.let {
          /*
          Check if it is valid url then show it to the user
          otherwise don't show uri to the user as they are not directly openable in the external app.
          We place this condition to improve the user experience see https://github.com/kiwix/kiwix-android/pull/3455
           */
          if (!"$it".startsWith("content://")) {
            showUrlInDialog(this, it)
          }
          dialog.getView?.let { setView(it()) }
          setCancelable(dialog.cancelable)
        }
      }
      .create()
  }

  private fun showUrlInDialog(builder: AlertDialog.Builder, uri: Uri) {
    val frameLayout = FrameLayout(activity.baseContext)

    val textView = TextView(activity.baseContext).apply {
      layoutParams = getFrameLayoutParams()
      gravity = Gravity.CENTER
      setLinkTextColor(activity.getAttribute(R.attr.colorPrimary))
      setOnLongClickListener {
        val clipboard =
          ContextCompat.getSystemService(activity.baseContext, ClipboardManager::class.java)
        val clip = ClipData.newPlainText("External Url", "$uri")
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(
          activity.baseContext,
          R.string.external_link_copied_message,
          Toast.LENGTH_SHORT
        ).show()
        true
      }
      @SuppressLint("SetTextI18n")
      text = "</br><a href=$uri> <b>$uri</b>".fromHtml()
    }
    frameLayout.addView(textView)
    builder.setView(frameLayout)
  }

  private fun getFrameLayoutParams() = FrameLayout.LayoutParams(
    LayoutParams.MATCH_PARENT,
    LayoutParams.WRAP_CONTENT
  ).apply {
    topMargin = externalLinkTopMargin
    bottomMargin = externalLinkBottomMargin
    leftMargin = externalLinkLeftMargin
    rightMargin = externalLinkRightMargin
  }

  private fun bodyArguments(dialog: KiwixDialog) =
    if (dialog is HasBodyFormatArgs) dialog.args.toTypedArray()
    else emptyArray()
}
