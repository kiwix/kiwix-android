package org.kiwix.kiwixmobile.extensions

import android.graphics.Color
import android.view.View
import com.google.android.material.snackbar.Snackbar

fun View.snack(
  stringId: Int,
  actionStringId: Int,
  actionClick: () -> Unit,
  actionTextColor: Int = Color.WHITE
) {
  Snackbar.make(
    this, stringId, Snackbar.LENGTH_LONG
  )
    .setAction(actionStringId) {
      actionClick.invoke()
    }
    .setActionTextColor(actionTextColor)
    .show()
}
