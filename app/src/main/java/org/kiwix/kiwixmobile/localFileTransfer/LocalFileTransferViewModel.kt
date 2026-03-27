package org.kiwix.kiwixmobile.localFileTransfer

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.NEARBY_WIFI_DEVICES
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.localFileTransfer.UiEvent.NavigateBack
import org.kiwix.kiwixmobile.localFileTransfer.UiEvent.RequestPermission
import org.kiwix.kiwixmobile.localFileTransfer.UiEvent.ShowDialog
import org.kiwix.kiwixmobile.localFileTransfer.WifiDirectManager.Companion.getDeviceStatus
import javax.inject.Inject

data class LocalFileTransferUiState(
  val deviceName: String = "",
  val isReceiver: Boolean = false,
  val isPeerSearching: Boolean = false,
  val peers: List<WifiP2pDevice> = emptyList(),
  val transferFiles: List<FileItem> = emptyList(),
  val shouldShowShowCase: Boolean = false,
  val isWritePermissionRequired: Boolean = false
)

class LocalFileTransferViewModel @Inject constructor(
  val kiwixDataStore: KiwixDataStore,
  private val wifiDirectManager: WifiDirectManager,
  private val locationManager: LocationManager,
  private val permissionChecker: KiwixPermissionChecker
) : ViewModel(), WifiDirectManager.Callbacks {
  private val _uiState = MutableStateFlow(LocalFileTransferUiState())
  val uiState: StateFlow<LocalFileTransferUiState> = _uiState.asStateFlow()

  private val _events = MutableSharedFlow<UiEvent>()
  val events = _events.asSharedFlow()

  private val isAndroid13OrAbove = permissionChecker.isAndroid13orAbove()

  init {
    viewModelScope.launch {
      kiwixDataStore.showShowCaseToUser.collect { shouldShow ->
        _uiState.update { it.copy(shouldShowShowCase = shouldShow) }
      }
    }
    viewModelScope.launch {
      val isRequired = permissionChecker.isWriteExternalStoragePermissionRequired()
      _uiState.update {
        it.copy(isWritePermissionRequired = isRequired)
      }
    }
  }

  fun showDialog(dialog: DialogEvent) {
    viewModelScope.launch {
      _events.emit(ShowDialog(dialog))
    }
  }

  fun initialize(uris: List<Uri>, alertDialogShower: AlertDialogShower) {
    val files = uris.map { FileItem(it) }
    val isReceiver = files.isEmpty()

    _uiState.update {
      it.copy(transferFiles = files, isReceiver = isReceiver)
    }

    initializeWifiDirectManager(files, alertDialogShower)
  }

  private fun initializeWifiDirectManager(
    filesForTransfer: List<FileItem>,
    alertDialogShower: AlertDialogShower
  ) {
    wifiDirectManager.apply {
      callbacks = this@LocalFileTransferViewModel
      setLifeCycleScope(viewModelScope)
      startWifiDirectManager(filesForTransfer)
      setAlertDialogShower(alertDialogShower)
    }
  }

  fun onSearchMenuClicked() {
    viewModelScope.launch {
      if (checkDiscoveryPreconditions()) {
        showPeerDiscoveryProgressBar()
        wifiDirectManager.discoverPeerDevices()
      }
    }
  }

  @Suppress("ReturnCount")
  private suspend fun checkDiscoveryPreconditions(): Boolean {
    if (!checkFineLocationOrWifiPermission()) {
      Log.d(TAG, "Location or wifi permission not granted")
      return false
    }

    if (!checkExternalStorageWritePermission()) {
      Log.d(TAG, "Storage permission not granted")
      return false
    }

    if (!wifiDirectManager.isWifiP2pEnabled) {
      Log.d(TAG, "wifi p2p is not enabled")
      requestEnableWifiP2pServices()
      return false
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
      !isLocationServiceEnabled
    ) {
      Log.d(TAG, "location service is not enabled")
      requestEnableLocationServices()
      return false
    }
    return true
  }

  // Setup UI for searching peers
  private fun showPeerDiscoveryProgressBar() {
    _uiState.update { it.copy(isPeerSearching = true) }
  }

  val isLocationServiceEnabled: Boolean
    get() =
      if (isAndroid13OrAbove) {
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
    showDialog(DialogEvent.ShowEnableWifiP2p)
  }

  private fun requestEnableLocationServices() {
    showDialog(DialogEvent.ShowEnableLocationServices)
  }

  fun onPermissionGranted() {
    onSearchMenuClicked()
  }

  private suspend fun checkFineLocationOrWifiPermission(): Boolean {
    val hasLocalPermission = if (isAndroid13OrAbove) {
      permissionChecker.hasNearbyWifiPermission()
    } else {
      permissionChecker.hasFineLocationPermission()
    }
    if (!hasLocalPermission) {
      requestPermission(locationPermission)
    }
    return hasLocalPermission
  }

  private suspend fun checkExternalStorageWritePermission(): Boolean {
    val isGranted = permissionChecker.hasWriteExternalStoragePermission()
    if (!isGranted) {
      requestPermission(WRITE_EXTERNAL_STORAGE)
    }
    return isGranted
  }

  private suspend fun requestPermission(permission: String) {
    _events.emit(RequestPermission(permission))
  }

  fun onPermissionRationaleRequired(permission: String) {
    val dialog = when (permission) {
      NEARBY_WIFI_DEVICES -> DialogEvent.ShowNearbyWifiRationale
      ACCESS_FINE_LOCATION -> DialogEvent.ShowLocationRationale
      WRITE_EXTERNAL_STORAGE -> DialogEvent.ShowStorageRationale
      else -> null
    }
    dialog?.let { showDialog(it) }
  }

  fun onDeviceSelected(device: WifiP2pDevice) {
    wifiDirectManager.sendToDevice(device)
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

  override fun onFileStatusChanged(itemIndex: Int, fileStatus: FileItem.FileStatus) {
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
      _events.emit(NavigateBack)
    }
  }

  fun onShowCaseDisplayed() {
    viewModelScope.launch {
      kiwixDataStore.setShowCaseViewForFileTransferShown()
    }
  }

  val locationPermission = if (isAndroid13OrAbove) {
    NEARBY_WIFI_DEVICES
  } else {
    ACCESS_FINE_LOCATION
  }

  override fun onCleared() {
    super.onCleared()
    wifiDirectManager.stopWifiDirectManager()
    wifiDirectManager.callbacks = null
  }

  companion object {
    const val TAG = "LocalFileTransfer"
  }
}

sealed class DialogEvent {
  object ShowNearbyWifiRationale : DialogEvent()
  object ShowLocationRationale : DialogEvent()
  object ShowStorageRationale : DialogEvent()
  object ShowEnableWifiP2p : DialogEvent()
  object ShowEnableLocationServices : DialogEvent()
}

sealed class UiEvent {
  data class RequestPermission(val permission: String) : UiEvent()
  object NavigateBack : UiEvent()
  data class ShowDialog(val dialog: DialogEvent) : UiEvent()
}
