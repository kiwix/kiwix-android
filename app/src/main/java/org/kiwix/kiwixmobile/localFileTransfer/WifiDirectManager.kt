/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.localFileTransfer

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.Uri
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.net.wifi.p2p.WifiP2pManager.ChannelListener
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.os.AsyncTask
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.FileTransferConfirmation
import org.kiwix.kiwixmobile.localFileTransfer.FileItem.FileStatus
import org.kiwix.kiwixmobile.localFileTransfer.KiwixWifiP2pBroadcastReceiver.P2pEventListener
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.util.ArrayList
import javax.inject.Inject

/**
 * Manager for the Wifi-P2p API, used in the local file transfer module
 */
@SuppressWarnings("MissingPermission", "ProtectedMemberInFinalClass")
class WifiDirectManager @Inject constructor(
  private val activity: Activity,
  private val sharedPreferenceUtil: SharedPreferenceUtil,
  private val alertDialogShower: AlertDialogShower
) : ChannelListener, PeerListListener, ConnectionInfoListener, P2pEventListener {
  var callbacks: Callbacks? = null

  /* Helper methods */
  /* Variables related to the WiFi P2P API */
  // Whether WiFi has been enabled or not
  var isWifiP2pEnabled = false
    private set

  // Whether channel has retried connecting previously
  private var shouldRetry = true

  // Overall manager of Wifi p2p connections for the module
  private lateinit var manager: WifiP2pManager

  // Interface to the device's underlying wifi-p2p framework
  private lateinit var channel: Channel

  // For receiving the broadcasts given by above filter
  private lateinit var receiver: BroadcastReceiver

  // Corresponds to P2P group formed between the two devices
  private lateinit var groupInfo: WifiP2pInfo
  private lateinit var senderSelectedPeerDevice: WifiP2pDevice
  private var peerGroupHandshakeAsyncTask: PeerGroupHandshakeAsyncTask? = null
  private var senderDevice: SenderDevice? = null
  private var receiverDeviceAsyncTask: ReceiverDeviceAsyncTask? = null
  private lateinit var selectedPeerDeviceInetAddress: InetAddress

  // IP address of the file receiving device
  private lateinit var fileReceiverDeviceAddress: InetAddress
  private lateinit var filesForTransfer: List<FileItem>

  // Whether the device is the file sender or not
  var isFileSender = false
    private set

  private var hasSenderStartedConnection = false

  /* Initialisations for using the WiFi P2P API */
  fun startWifiDirectManager(filesForTransfer: List<FileItem>) {
    this.filesForTransfer = filesForTransfer
    isFileSender = filesForTransfer.isNotEmpty()
    manager = activity.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    channel = manager.initialize(activity, Looper.getMainLooper(), null)
    registerWifiDirectBroadcastReceiver()
  }

  private fun registerWifiDirectBroadcastReceiver() {
    receiver = KiwixWifiP2pBroadcastReceiver(this)

    // For specifying broadcasts (of the P2P API) that the module needs to respond to
    val intentFilter = IntentFilter()
    intentFilter.apply {
      addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
      addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
      addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
      addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }
    activity.registerReceiver(receiver, intentFilter)
  }

  private fun unregisterWifiDirectBroadcastReceiver() = activity.unregisterReceiver(receiver)

  fun discoverPeerDevices() {
    manager.discoverPeers(channel, object : ActionListener {
      override fun onSuccess() {
        activity.toast(R.string.discovery_initiated, Toast.LENGTH_SHORT)
      }

      override fun onFailure(reason: Int) {
        Log.d(
          TAG, "${activity.getString(R.string.discovery_failed)}: " +
            getErrorMessage(reason)
        )
        activity.toast(R.string.discovery_failed, Toast.LENGTH_SHORT)
      }
    })
  }

  /* From KiwixWifiP2pBroadcastReceiver.P2pEventListener callback-interface*/
  override fun onWifiP2pStateChanged(isEnabled: Boolean) {
    isWifiP2pEnabled = isEnabled
    if (!isWifiP2pEnabled) {
      activity.toast(R.string.discovery_needs_wifi, Toast.LENGTH_SHORT)
      callbacks?.onConnectionToPeersLost()
    }
    Log.d(TAG, "WiFi P2P state changed - $isWifiP2pEnabled")
  }

  override fun onPeersChanged() {
    /* List of available peers has changed, so request & use the new list through
     * PeerListListener.requestPeers() callback */
    manager.requestPeers(channel, this)
    Log.d(TAG, "P2P peers changed")
  }

  override fun onConnectionChanged(isConnected: Boolean) {
    if (isConnected) {
      // Request connection info about the wifi p2p group formed upon connection
      manager.requestConnectionInfo(channel, this)
    } else {
      // Not connected after connection change -> Disconnected
      callbacks?.onConnectionToPeersLost()
    }
  }

  // Update UI with wifi-direct details about the user device
  override fun onDeviceChanged(userDevice: WifiP2pDevice?) {
    callbacks?.onUserDeviceDetailsAvailable(userDevice)
  }

  /* From WifiP2pManager.ChannelListener interface */
  override fun onChannelDisconnected() {
    // Upon disconnection, retry one more time
    if (shouldRetry) {
      Log.d(TAG, "Channel lost, trying again")
      callbacks?.onConnectionToPeersLost()
      shouldRetry = false
      manager.initialize(activity, Looper.getMainLooper(), this)
    } else {
      activity.toast(R.string.severe_loss_error, Toast.LENGTH_LONG)
    }
  }

  /* From WifiP2pManager.PeerListListener callback-interface */
  override fun onPeersAvailable(peers: WifiP2pDeviceList) {
    callbacks?.updateListOfAvailablePeers(peers)
  }

  /* From WifiP2pManager.ConnectionInfoListener callback-interface */
  override fun onConnectionInfoAvailable(groupInfo: WifiP2pInfo) {
    /* Devices have successfully connected, and 'info' holds information about the wifi p2p group formed */
    this.groupInfo = groupInfo
    performHandshakeWithSelectedPeerDevice()
  }

  val isGroupFormed: Boolean
    get() = groupInfo.groupFormed

  val isGroupOwner: Boolean
    get() = groupInfo.isGroupOwner

  val groupOwnerAddress: InetAddress
    get() = groupInfo.groupOwnerAddress

  fun sendToDevice(senderSelectedPeerDevice: WifiP2pDevice) {
    /* Connection can only be initiated by user of the sender device, & only when transfer has not been started */
    if (isFileSender && !hasSenderStartedConnection) {
      this.senderSelectedPeerDevice = senderSelectedPeerDevice
      alertDialogShower.show(
        FileTransferConfirmation(senderSelectedPeerDevice.deviceName), {
          hasSenderStartedConnection = true
          connect()
          activity.toast(R.string.performing_handshake, Toast.LENGTH_LONG)
        })
    }
  }

  private fun connect() {
    val config = WifiP2pConfig().apply {
      deviceAddress = senderSelectedPeerDevice.deviceAddress
      wps.setup = WpsInfo.PBC
    }
    manager.connect(channel, config, object : ActionListener {
      override fun onSuccess() {
        // UI updated from broadcast receiver
      }

      override fun onFailure(reason: Int) {
        val errorMessage = getErrorMessage(reason)
        Log.d(TAG, activity.getString(R.string.connection_failed) + ": " + errorMessage)
        activity.toast(R.string.connection_failed, Toast.LENGTH_LONG)
      }
    })
  }

  private fun performHandshakeWithSelectedPeerDevice() {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "Starting handshake")
    }
    peerGroupHandshakeAsyncTask = PeerGroupHandshakeAsyncTask(this)
      .also { it.execute() }
  }

  val totalFilesForTransfer: Int
    get() = filesForTransfer.size

  fun getFilesForTransfer() = filesForTransfer

  fun setFilesForTransfer(fileItems: ArrayList<FileItem>) {
    filesForTransfer = fileItems
  }

  val zimStorageRootPath
    get() = sharedPreferenceUtil.prefStorage + "/Kiwix/"

  fun getFileReceiverDeviceAddress() = fileReceiverDeviceAddress

  fun setClientAddress(clientAddress: InetAddress) {

    // If control reaches here, means handshake was successful
    selectedPeerDeviceInetAddress = clientAddress
    startFileTransfer()
  }

  private fun startFileTransfer() {
    if (isGroupFormed) {
      if (isFileSender) {
        Log.d(LocalFileTransferFragment.TAG, "Starting file transfer")
        fileReceiverDeviceAddress =
          if (isGroupOwner) selectedPeerDeviceInetAddress else groupOwnerAddress
        activity.toast(R.string.preparing_files, Toast.LENGTH_LONG)
        senderDevice = SenderDevice(this, activity).also {
          CoroutineScope(Dispatchers.IO).launch {
            val hasSend = it.send(filesForTransfer.toTypedArray())
            withContext(Dispatchers.Main) {
              if (BuildConfig.DEBUG) Log.d(TAG, "SenderDeviceAsyncTask complete")
              onFileTransferAsyncTaskComplete(hasSend)
            }

          }
        }
      } else {
        callbacks?.onFilesForTransferAvailable(filesForTransfer)
        receiverDeviceAsyncTask = ReceiverDeviceAsyncTask(this).also {
          it.execute()
        }
      }
    }
  }

  fun changeStatus(itemIndex: Int, status: FileStatus) {
    filesForTransfer[itemIndex].fileStatus = status
    callbacks?.onFileStatusChanged(itemIndex)
    if (status == FileStatus.ERROR) {
      displayToast(
        R.string.error_transferring, filesForTransfer[itemIndex].fileName,
        Toast.LENGTH_SHORT
      )
    }
  }

  private fun cancelAsyncTasks(vararg tasks: AsyncTask<*, *, *>?) =
    tasks.forEach {
      it?.cancel(true)
    }

  fun stopWifiDirectManager() {
    cancelAsyncTasks(peerGroupHandshakeAsyncTask, receiverDeviceAsyncTask)
    if (isFileSender) {
      closeChannel()
    } else {
      disconnect()
    }
    unregisterWifiDirectBroadcastReceiver()
  }

  private fun disconnect() {
    manager.removeGroup(channel, object : ActionListener {
      override fun onFailure(reasonCode: Int) {
        Log.d(TAG, "Disconnect failed. Reason: $reasonCode")
        closeChannel()
      }

      override fun onSuccess() {
        Log.d(TAG, "Disconnect successful")
        closeChannel()
      }
    })
  }

  private fun closeChannel() {
    if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
      channel.close()
    }
  }

  fun getErrorMessage(reason: Int): String {
    return when (reason) {
      WifiP2pManager.ERROR -> "Internal error"
      WifiP2pManager.BUSY -> "Framework busy, unable to service request"
      WifiP2pManager.P2P_UNSUPPORTED -> "P2P unsupported on this device"
      else -> "Unknown error code - $reason"
    }
  }

  fun displayToast(stringResourceId: Int, templateValue: String, duration: Int) =
    activity.toast(activity.getString(stringResourceId, templateValue), duration)

  fun onFileTransferAsyncTaskComplete(wereAllFilesTransferred: Boolean) {
    if (wereAllFilesTransferred) {
      activity.toast(R.string.file_transfer_complete, Toast.LENGTH_LONG)
    } else {
      activity.toast(R.string.error_during_transfer, Toast.LENGTH_LONG)
    }
    callbacks?.onFileTransferComplete()
  }

  interface Callbacks {
    fun onUserDeviceDetailsAvailable(userDevice: WifiP2pDevice?)
    fun onConnectionToPeersLost()
    fun updateListOfAvailablePeers(peers: WifiP2pDeviceList)
    fun onFilesForTransferAvailable(filesForTransfer: List<FileItem>)
    fun onFileStatusChanged(itemIndex: Int)
    fun onFileTransferComplete()
  }

  companion object {
    private const val TAG = "WifiDirectManager"
    @JvmField var FILE_TRANSFER_PORT = 8008
    @JvmStatic @Throws(IOException::class) fun copyToOutputStream(
      inputStream: InputStream,
      outputStream: OutputStream
    ) {
      inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }
      Log.d(LocalFileTransferFragment.TAG, "Both streams closed")
    }

    @JvmStatic fun getDeviceStatus(status: Int): String {
      if (BuildConfig.DEBUG) Log.d(TAG, "Peer Status: $status")
      return when (status) {
        WifiP2pDevice.AVAILABLE -> "Available"
        WifiP2pDevice.INVITED -> "Invited"
        WifiP2pDevice.CONNECTED -> "Connected"
        WifiP2pDevice.FAILED -> "Failed"
        WifiP2pDevice.UNAVAILABLE -> "Unavailable"
        else -> "Unknown"
      }
    }

    // Returns text after location of last slash in the file path
    @JvmStatic fun getFileName(fileUri: Uri) = "$fileUri".substringAfterLast('/')
  }
}
