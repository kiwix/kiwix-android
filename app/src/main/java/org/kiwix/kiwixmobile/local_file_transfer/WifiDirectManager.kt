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
package org.kiwix.kiwixmobile.local_file_transfer

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
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.utils.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.KiwixDialog.FileTransferConfirmation
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.local_file_transfer.FileItem.FileStatus
import org.kiwix.kiwixmobile.local_file_transfer.KiwixWifiP2pBroadcastReceiver.P2pEventListener
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.util.ArrayList
import javax.inject.Inject

/**
 * Manager for the Wifi-P2p API, used in the local file transfer module
 */
class WifiDirectManager @Inject constructor(
  private val activity: Activity,
  sharedPreferenceUtil: SharedPreferenceUtil,
  alertDialogShower: AlertDialogShower
) : ChannelListener, PeerListListener, ConnectionInfoListener, P2pEventListener {
  private val callbacks: Callbacks
  private val sharedPreferenceUtil: SharedPreferenceUtil
  private val alertDialogShower: AlertDialogShower
  /* Helper methods */
  /* Variables related to the WiFi P2P API */
  var isWifiP2pEnabled = false // Whether WiFi has been enabled or not
    private set
  private var shouldRetry =
    true // Whether channel has retried connecting previously
  private var manager // Overall manager of Wifi p2p connections for the module
    : WifiP2pManager? = null
  private var channel // Interface to the device's underlying wifi-p2p framework
    : Channel? = null
  private var receiver: BroadcastReceiver? =
    null // For receiving the broadcasts given by above filter
  private var groupInfo // Corresponds to P2P group formed between the two devices
    : WifiP2pInfo? = null
  private var senderSelectedPeerDevice: WifiP2pDevice? = null
  private var peerGroupHandshakeAsyncTask: PeerGroupHandshakeAsyncTask? = null
  private var senderDeviceAsyncTask: SenderDeviceAsyncTask? = null
  private var receiverDeviceAsyncTask: ReceiverDeviceAsyncTask? = null
  private var selectedPeerDeviceInetAddress: InetAddress? = null
  private var fileReceiverDeviceAddress // IP address of the file receiving device
    : InetAddress? = null
  private var filesForTransfer: ArrayList<FileItem>? = null
  var isFileSender = false // Whether the device is the file sender or not
    private set
  private var hasSenderStartedConnection = false
  /* Initialisations for using the WiFi P2P API */
  fun startWifiDirectManager(filesForTransfer: ArrayList<FileItem>?) {
    this.filesForTransfer = filesForTransfer
    isFileSender = filesForTransfer != null && filesForTransfer.size > 0
    manager = activity.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    channel = manager!!.initialize(activity, Looper.getMainLooper(), null)
    registerWifiDirectBroadcastReceiver()
  }

  private fun registerWifiDirectBroadcastReceiver() {
    receiver = KiwixWifiP2pBroadcastReceiver(this)
    // For specifying broadcasts (of the P2P API) that the module needs to respond to
    val intentFilter = IntentFilter()
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
    intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    activity.registerReceiver(receiver, intentFilter)
  }

  private fun unregisterWifiDirectBroadcastReceiver() {
    activity.unregisterReceiver(receiver)
  }

  fun discoverPeerDevices() {
    manager!!.discoverPeers(channel, object : ActionListener {
      override fun onSuccess() {
        displayToast(string.discovery_initiated, Toast.LENGTH_SHORT)
      }

      override fun onFailure(reason: Int) {
        val errorMessage = getErrorMessage(reason)
        Log.d(
          TAG,
          activity.getString(string.discovery_failed) + ": " + errorMessage
        )
        displayToast(string.discovery_failed, Toast.LENGTH_SHORT)
      }
    })
  }

  /* From KiwixWifiP2pBroadcastReceiver.P2pEventListener callback-interface*/
  override fun onWifiP2pStateChanged(isEnabled: Boolean) {
    isWifiP2pEnabled = isEnabled
    if (!isWifiP2pEnabled) {
      displayToast(string.discovery_needs_wifi, Toast.LENGTH_SHORT)
      callbacks.onConnectionToPeersLost()
    }
    Log.d(
      TAG,
      "WiFi P2P state changed - $isWifiP2pEnabled"
    )
  }

  override fun onPeersChanged() { /* List of available peers has changed, so request & use the new list through
     * PeerListListener.requestPeers() callback */
    manager!!.requestPeers(channel, this)
    Log.d(TAG, "P2P peers changed")
  }

  override fun onConnectionChanged(isConnected: Boolean) {
    if (isConnected) { // Request connection info about the wifi p2p group formed upon connection
      manager!!.requestConnectionInfo(channel, this)
    } else { // Not connected after connection change -> Disconnected
      callbacks.onConnectionToPeersLost()
    }
  }

  override fun onDeviceChanged(userDevice: WifiP2pDevice?) { // Update UI with wifi-direct details about the user device
    callbacks.onUserDeviceDetailsAvailable(userDevice)
  }

  /* From WifiP2pManager.ChannelListener interface */
  override fun onChannelDisconnected() { // Upon disconnection, retry one more time
    if (shouldRetry) {
      Log.d(TAG, "Channel lost, trying again")
      callbacks.onConnectionToPeersLost()
      shouldRetry = false
      manager!!.initialize(activity, Looper.getMainLooper(), this)
    } else {
      displayToast(string.severe_loss_error, Toast.LENGTH_LONG)
    }
  }

  /* From WifiP2pManager.PeerListListener callback-interface */
  override fun onPeersAvailable(peers: WifiP2pDeviceList) {
    callbacks.updateListOfAvailablePeers(peers)
  }

  /* From WifiP2pManager.ConnectionInfoListener callback-interface */
  override fun onConnectionInfoAvailable(groupInfo: WifiP2pInfo) { /* Devices have successfully connected, and 'info' holds information about the wifi p2p group formed */
    this.groupInfo = groupInfo
    performHandshakeWithSelectedPeerDevice()
  }

  val isGroupFormed: Boolean
    get() = groupInfo!!.groupFormed

  val isGroupOwner: Boolean
    get() = groupInfo!!.isGroupOwner

  val groupOwnerAddress: InetAddress
    get() = groupInfo!!.groupOwnerAddress

  fun sendToDevice(senderSelectedPeerDevice: WifiP2pDevice) { /* Connection can only be initiated by user of the sender device, & only when transfer has not been started */
    if (!isFileSender || hasSenderStartedConnection) {
      return
    }
    this.senderSelectedPeerDevice = senderSelectedPeerDevice
    alertDialogShower.show(
      FileTransferConfirmation(senderSelectedPeerDevice.deviceName), {
        hasSenderStartedConnection = true
        connect()
        displayToast(string.performing_handshake, Toast.LENGTH_LONG)
      })
  }

  private fun connect() {
    if (senderSelectedPeerDevice == null) {
      Log.d(TAG, "No device set as selected")
    }
    val config = WifiP2pConfig()
    config.deviceAddress = senderSelectedPeerDevice!!.deviceAddress
    config.wps.setup = WpsInfo.PBC
    manager!!.connect(channel, config, object : ActionListener {
      override fun onSuccess() { // UI updated from broadcast receiver
      }

      override fun onFailure(reason: Int) {
        val errorMessage = getErrorMessage(reason)
        Log.d(
          TAG,
          activity.getString(string.connection_failed) + ": " + errorMessage
        )
        displayToast(string.connection_failed, Toast.LENGTH_LONG)
      }
    })
  }

  private fun performHandshakeWithSelectedPeerDevice() {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "Starting handshake")
    }
    peerGroupHandshakeAsyncTask = PeerGroupHandshakeAsyncTask(this)
    peerGroupHandshakeAsyncTask!!.execute()
  }

  val totalFilesForTransfer: Int
    get() = filesForTransfer!!.size

  fun getFilesForTransfer(): ArrayList<FileItem> = filesForTransfer!!

  fun setFilesForTransfer(fileItems: ArrayList<FileItem>) {
    filesForTransfer = fileItems
  }

  val zimStorageRootPath: String
    get() = sharedPreferenceUtil.prefStorage + "/Kiwix/"

  fun getFileReceiverDeviceAddress(): InetAddress = fileReceiverDeviceAddress!!

  fun setClientAddress(clientAddress: InetAddress?) {
    if (clientAddress == null) { // null is returned only in case of a failed handshake
      displayToast(string.device_not_cooperating, Toast.LENGTH_LONG)
      callbacks.onFileTransferComplete()
      return
    }
    // If control reaches here, means handshake was successful
    selectedPeerDeviceInetAddress = clientAddress
    startFileTransfer()
  }

  private fun startFileTransfer() {
    if (isGroupFormed) {
      if (isFileSender) {
        Log.d(LocalFileTransferActivity.TAG, "Starting file transfer")
        fileReceiverDeviceAddress =
          if (isGroupOwner) selectedPeerDeviceInetAddress else groupOwnerAddress
        displayToast(string.preparing_files, Toast.LENGTH_LONG)
        senderDeviceAsyncTask = SenderDeviceAsyncTask(this, activity)
        senderDeviceAsyncTask!!.execute(*filesForTransfer!!.toTypedArray())
      } else {
        callbacks.onFilesForTransferAvailable(filesForTransfer!!)
        receiverDeviceAsyncTask = ReceiverDeviceAsyncTask(this)
        receiverDeviceAsyncTask!!.execute()
      }
    }
  }

  fun changeStatus(itemIndex: Int, @FileStatus status: Int) {
    filesForTransfer!![itemIndex].fileStatus = status
    callbacks.onFileStatusChanged(itemIndex)
    if (status == FileStatus.ERROR) {
      displayToast(
        string.error_transferring, filesForTransfer!![itemIndex].fileName,
        Toast.LENGTH_SHORT
      )
    }
  }

  private fun cancelAsyncTasks(vararg tasks: AsyncTask<*, *, *>) {
    for (asyncTask in tasks) {
      asyncTask?.cancel(true)
    }
  }

  fun stopWifiDirectManager() {
    cancelAsyncTasks(
      peerGroupHandshakeAsyncTask!!,
      senderDeviceAsyncTask!!,
      receiverDeviceAsyncTask!!
    )
    if (isFileSender) {
      closeChannel()
    } else {
      disconnect()
    }
    unregisterWifiDirectBroadcastReceiver()
  }

  private fun disconnect() {
    manager!!.removeGroup(channel, object : ActionListener {
      override fun onFailure(reasonCode: Int) {
        Log.d(
          TAG,
          "Disconnect failed. Reason: $reasonCode"
        )
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
      channel!!.close()
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

  fun displayToast(
    stringResourceId: Int,
    templateValue: String,
    duration: Int
  ) {
    LocalFileTransferActivity.showToast(
      activity,
      activity.getString(stringResourceId, templateValue),
      duration
    )
  }

  fun displayToast(stringResourceId: Int, duration: Int) {
    LocalFileTransferActivity.showToast(activity, stringResourceId, duration)
  }

  fun onFileTransferAsyncTaskComplete(wereAllFilesTransferred: Boolean) {
    if (wereAllFilesTransferred) {
      displayToast(string.file_transfer_complete, Toast.LENGTH_LONG)
    } else {
      displayToast(string.error_during_transfer, Toast.LENGTH_LONG)
    }
    callbacks.onFileTransferComplete()
  }

  interface Callbacks {
    fun onUserDeviceDetailsAvailable(userDevice: WifiP2pDevice?)
    fun onConnectionToPeersLost()
    fun updateListOfAvailablePeers(peers: WifiP2pDeviceList)
    fun onFilesForTransferAvailable(filesForTransfer: ArrayList<FileItem>)
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
      val bufferForBytes = ByteArray(1024)
      var bytesRead: Int
      Log.d(TAG, "Copying to OutputStream...")
      while (inputStream.read(bufferForBytes).also { bytesRead = it } != -1) {
        outputStream.write(bufferForBytes, 0, bytesRead)
      }
      outputStream.close()
      inputStream.close()
      Log.d(LocalFileTransferActivity.TAG, "Both streams closed")
    }

    @JvmStatic fun getDeviceStatus(status: Int): String {
      if (BuildConfig.DEBUG) Log.d(
        TAG,
        "Peer Status: $status"
      )
      return when (status) {
        WifiP2pDevice.AVAILABLE -> "Available"
        WifiP2pDevice.INVITED -> "Invited"
        WifiP2pDevice.CONNECTED -> "Connected"
        WifiP2pDevice.FAILED -> "Failed"
        WifiP2pDevice.UNAVAILABLE -> "Unavailable"
        else -> "Unknown"
      }
    }

    @JvmStatic fun getFileName(fileUri: Uri): String {
      val fileUriString = "$fileUri"
      // Returns text after location of last slash in the file path
      return fileUriString.substring(fileUriString.lastIndexOf('/') + 1)
    }
  }

  init {
    callbacks =
      activity as Callbacks
    this.sharedPreferenceUtil = sharedPreferenceUtil
    this.alertDialogShower = alertDialogShower
  }
}
