package org.kiwix.kiwixmobile.utils

import android.app.Activity
import android.app.AlertDialog
import org.kiwix.kiwixmobile.R
import javax.inject.Inject

class AlertDialogShower @Inject constructor(
  private val activity: Activity,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) : DialogShower {
  override fun show(
    dialog: KiwixDialog,
    vararg clickListeners: () -> Unit
  ) {

    AlertDialog.Builder(activity, dialogStyle())
      .apply {
        dialog.title?.let(this::setTitle)
        setMessage(
          activity.getString(
            dialog.message,
            *bodyArguments(dialog)
          )
        )
        setPositiveButton(dialog.positiveMessage) { _, _ ->
          clickListeners.getOrNull(0)
            ?.invoke()
        }
        setNegativeButton(dialog.negativeMessage) { _, _ ->
          clickListeners.getOrNull(1)
            ?.invoke()
        }
      }
      .show()
  }

  private fun bodyArguments(dialog: KiwixDialog) =
    if (dialog is HasBodyFormatArgs) dialog.args
    else emptyArray()

  private fun dialogStyle() =
    if (sharedPreferenceUtil.nightMode()) {
      R.style.AppTheme_Dialog_Night
    } else {
      R.style.AppTheme_Dialog
    }
}
