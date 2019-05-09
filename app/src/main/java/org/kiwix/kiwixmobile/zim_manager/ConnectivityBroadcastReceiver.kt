package org.kiwix.kiwixmobile.zim_manager

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.extensions.networkState
import javax.inject.Inject

class ConnectivityBroadcastReceiver @Inject constructor(private val connectivityManager: ConnectivityManager) :
    BaseBroadcastReceiver() {

  override val action: String = ConnectivityManager.CONNECTIVITY_ACTION

  val networkStates =
    BehaviorProcessor.createDefault(connectivityManager.networkState)

  override fun onIntentWithActionReceived(
    context: Context,
    intent: Intent
  ) {
    networkStates.onNext(connectivityManager.networkState)
  }

}