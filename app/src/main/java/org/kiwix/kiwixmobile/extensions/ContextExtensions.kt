package org.kiwix.kiwixmobile.extensions

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import org.kiwix.kiwixmobile.zim_manager.BaseBroadcastReceiver

fun Context?.toast(
  stringId: Int,
  length: Int = Toast.LENGTH_LONG
) {
  this?.let {
    Toast.makeText(this, stringId, length)
      .show()
  }
}

fun Context?.toast(
  text: String,
  length: Int = Toast.LENGTH_LONG
) {
  this?.let {
    Toast.makeText(this, text, length)
      .show()
  }
}

fun Context.registerReceiver(baseBroadcastReceiver: BaseBroadcastReceiver): Intent? =
  registerReceiver(baseBroadcastReceiver, IntentFilter(baseBroadcastReceiver.action))
