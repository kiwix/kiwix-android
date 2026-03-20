package org.kiwix.kiwixmobile.localFileTransfer

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.NEARBY_WIFI_DEVICES
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.location.LocationManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.localFileTransfer.WifiDirectManager.Companion.getDeviceStatus
import javax.inject.Inject

data class LocalFileTransferUiState(
  val deviceName: String = "",
  val isPeerSearching: Boolean = false,
  val peers: List<WifiP2pDevice> = emptyList(),
  val transferFiles: List<FileItem> = emptyList()
)

class LocalFileTransferViewModel @Inject constructor(
  val kiwixDataStore: KiwixDataStore,
  val wifiDirectManager: WifiDirectManager,
  val alertDialogShower: AlertDialogShower,
  private val locationManager: LocationManager,
  private val permissionChecker: KiwixPermissionChecker
) : ViewModel(), WifiDirectManager.Callbacks {
  private val _uiState = MutableStateFlow(LocalFileTransferUiState())
  val uiState: StateFlow<LocalFileTransferUiState> = _uiState.asStateFlow()

  private val _permissionEvent = Channel<PermissionAction>(Channel.BUFFERED)
  val permissionEvent = _permissionEvent.receiveAsFlow()

  private val _dialogEvent = Channel<DialogEvent>(Channel.BUFFERED)
  val dialogEvent = _dialogEvent.receiveAsFlow()

  fun showDialog(dialog: DialogEvent) {
    viewModelScope.launch {
      _dialogEvent.send(dialog)
    }
  }

  private val _navigationEvent = Channel<NavigationEvent>(Channel.BUFFERED)
  val navigationEvent = _navigationEvent.receiveAsFlow()

  val android13OrAbove = permissionChecker.isAndroid13orAbove()

  suspend fun isWritePermissionRequired(): Boolean =
    permissionChecker.isWriteExternalStoragePermissionRequired()

  fun initializeWifiDirectManager(filesForTransfer: List<FileItem>) {
    _uiState.update { it.copy(transferFiles = filesForTransfer) }
    wifiDirectManager.hasSenderStartedConnection = false

    wifiDirectManager.apply {
      callbacks = this@LocalFileTransferViewModel
      lifecycleCoroutineScope = viewModelScope
      startWifiDirectManager(filesForTransfer)
      setAlertDialogShower(alertDialogShower)
    }
  }

  fun onDeviceSelected(device: WifiP2pDevice) {
    wifiDirectManager.sendToDevice(device)
  }

  @SuppressLint("InlinedApi")
  fun onSearchMenuClicked() {
    viewModelScope.launch {
      if (!checkFineLocationAccessPermission()) {
        Log.d(TAG, "Location or wifi permission not granted")
        return@launch
      }

      if (!checkExternalStorageWritePermission()) {
        Log.d(TAG, "Storage permission not granted")
        return@launch
      }

      if (!wifiDirectManager.isWifiP2pEnabled) {
        Log.d(TAG, "wifi p2p is not enabled")
        requestEnableWifiP2pServices()
        return@launch
      }

      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
        !isLocationServiceEnabled
      ) {
        Log.d(TAG, "location service is not enabled")
        requestEnableLocationServices()
        return@launch
      }

      Log.d(TAG, "All true in onSearchMenuClicked - checks passed")
      showPeerDiscoveryProgressBar()
      wifiDirectManager.discoverPeerDevices()
    }
  }

  // Setup UI for searching peers
  private fun showPeerDiscoveryProgressBar() {
    _uiState.update { it.copy(isPeerSearching = true) }
  }

  val isLocationServiceEnabled: Boolean
    get() =
      if (android13OrAbove) {
        true
      } else {
        isProviderEnabled(LocationManager.GPS_PROVIDER) ||
          isProviderEnabled(LocationManager.NETWORK_PROVIDER)
      }

  private fun isProviderEnabled(locationProvider: String): Boolean {
    return try {
      locationManager.isProviderEnabled(locationProvider)
    } catch (ex: SecurityException) {
      ex.printStackTrace()
      false
    } catch (ex: IllegalArgumentException) {
      ex.printStackTrace()
      false
    }
  }

  private fun requestEnableWifiP2pServices() {
    viewModelScope.launch {
      _dialogEvent.send(DialogEvent.ShowEnableWifiP2p)
    }
  }

  private fun requestEnableLocationServices() {
    viewModelScope.launch {
      _dialogEvent.send(DialogEvent.ShowEnableLocationServices)
    }
  }

  @SuppressLint("InlinedApi")
  fun onPermissionGranted() {
    onSearchMenuClicked()
  }

  @SuppressLint("InlinedApi")
  private suspend fun checkFineLocationAccessPermission(): Boolean {
    if (android13OrAbove) {
      val hasWifiPerm = permissionChecker.hasNearbyWifiPermission()

      if (!hasWifiPerm) {
        _permissionEvent.send(PermissionAction.RequestPermission(NEARBY_WIFI_DEVICES))
      }

      return hasWifiPerm
    }

    val hasLocationPermission = permissionChecker.hasFineLocationPermission()

    if (!hasLocationPermission) {
      _permissionEvent.send(PermissionAction.RequestPermission(ACCESS_FINE_LOCATION))
    }

    return hasLocationPermission
  }

  private suspend fun checkExternalStorageWritePermission(): Boolean {
    val hasPermission = permissionChecker.hasWriteExternalStoragePermission()

    return when (hasPermission) {
      true -> true
      false -> {
        _permissionEvent.send(PermissionAction.RequestPermission(WRITE_EXTERNAL_STORAGE))
        false
      }
    }
  }

  override fun onUserDeviceDetailsAvailable(userDevice: WifiP2pDevice?) {
    if (userDevice != null) {
      _uiState.update { it.copy(deviceName = userDevice.deviceName) }
      Log.d(TAG, getDeviceStatus(userDevice.status))
    }
  }

  override fun onConnectionToPeersLost() {
    _uiState.update { it.copy(peers = emptyList()) }
  }

  override fun updateListOfAvailablePeers(peers: WifiP2pDeviceList) {
    val deviceList: List<WifiP2pDevice> = ArrayList<WifiP2pDevice>(peers.deviceList)
    _uiState.update { it.copy(isPeerSearching = false, peers = deviceList) }
    if (deviceList.isEmpty()) {
      Log.d(TAG, "No devices found")
    }
  }

  override fun onFilesForTransferAvailable(filesForTransfer: List<FileItem>) {
    _uiState.update { it.copy(transferFiles = filesForTransfer) }
  }

  override fun onFileStatusChanged(
    itemIndex: Int,
    fileStatus: FileItem.FileStatus
  ) {
    _uiState.update { state ->
      val tempTransferList = state.transferFiles.toMutableList()
      if (itemIndex in tempTransferList.indices) {
        tempTransferList[itemIndex].fileStatus = fileStatus
      }
      state.copy(transferFiles = tempTransferList)
    }
  }

  override fun onFileTransferComplete() {
    viewModelScope.launch {
      _navigationEvent.send(NavigationEvent.NavigateBack)
    }
  }

  override fun onCleared() {
    super.onCleared()
    wifiDirectManager.stopWifiDirectManager()
    wifiDirectManager.callbacks = null
  }

  companion object {
    // Not a typo, 'Log' tags have a length upper limit of 25 characters
    const val TAG = "LocalFileTransferActvty"
  }
}

sealed class NavigationEvent {
  object NavigateBack : NavigationEvent()
}

sealed class PermissionAction {
  data class RequestPermission(val permission: String) : PermissionAction()
}

sealed class DialogEvent {
  object ShowNearbyWifiRationale : DialogEvent()
  object ShowLocationRationale : DialogEvent()
  object ShowStorageRationale : DialogEvent()
  object ShowEnableWifiP2p : DialogEvent()
  object ShowEnableLocationServices : DialogEvent()
}
