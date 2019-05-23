package org.kiwix.kiwixmobile.extensions

import android.graphics.Color
import android.support.design.widget.Snackbar
import android.view.View

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