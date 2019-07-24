package org.kiwix.kiwixmobile.zim_manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

abstract class BaseBroadcastReceiver : BroadcastReceiver() {

  abstract val action: String

  override fun onReceive(
    context: Context,
    intent: Intent?
  ) {
    if (intent?.action == action) {
      onIntentWithActionReceived(context, intent)
    }
  }

  abstract fun onIntentWithActionReceived(
    context: Context,
    intent: Intent
  )
}
