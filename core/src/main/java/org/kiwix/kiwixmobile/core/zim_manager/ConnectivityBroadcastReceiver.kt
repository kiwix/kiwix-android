package org.kiwix.kiwixmobile.core.zim_manager

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import org.kiwix.kiwixmobile.core.extensions.networkState
import javax.inject.Inject

class ConnectivityBroadcastReceiver @Inject constructor(
  private val connectivityManager: ConnectivityManager
) :
  org.kiwix.kiwixmobile.core.zim_manager.BaseBroadcastReceiver() {

  override val action: String = ConnectivityManager.CONNECTIVITY_ACTION

  private val _networkStates = BehaviorProcessor.createDefault(connectivityManager.networkState)
  val networkStates: Flowable<NetworkState> = _networkStates

  override fun onIntentWithActionReceived(
    context: Context,
    intent: Intent
  ) {
    _networkStates.onNext(connectivityManager.networkState)
  }
}
