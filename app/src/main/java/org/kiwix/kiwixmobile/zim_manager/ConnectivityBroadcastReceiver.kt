package org.kiwix.kiwixmobile.zim_manager

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.extensions.networkState
import javax.inject.Inject

class ConnectivityBroadcastReceiver @Inject constructor(private val connectivityManager: ConnectivityManager) :
    BaseBroadcastReceiver() {

  override val action: String = ConnectivityManager.CONNECTIVITY_ACTION

  private val _networkStates =
    PublishProcessor.create<NetworkState>()
  val networkStates = _networkStates.startWith(connectivityManager.networkState)

  override fun onIntentWithActionReceived(
    context: Context,
    intent: Intent
  ) {
    _networkStates.onNext(connectivityManager.networkState)
  }

}