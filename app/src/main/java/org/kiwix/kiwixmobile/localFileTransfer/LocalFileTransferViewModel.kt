package org.kiwix.kiwixmobile.localFileTransfer

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.NEARBY_WIFI_DEVICES
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Application
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.page.SEARCH_ICON_TESTING_TAG
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.ui.models.IconItem.Vector
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.localFileTransfer.WifiDirectManager.Companion.getDeviceStatus
import java.lang.Exception
import javax.inject.Inject

class LocalFileTransferViewModel @Inject constructor(
  private val application: Application,
  val kiwixDataStore: KiwixDataStore,
  val wifiDirectManager: WifiDirectManager,
  val alertDialogShower: AlertDialogShower,
  private val locationManager: android.location.LocationManager
) : ViewModel(), WifiDirectManager.Callbacks {

  private val _deviceName = MutableStateFlow("")
  val deviceName: StateFlow<String> = _deviceName.asStateFlow()

  private val _isPeerSearching = MutableStateFlow(false)
  val isPeerSearching: StateFlow<Boolean> = _isPeerSearching.asStateFlow()

  private val _peerDeviceList = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
  val peerDeviceList: StateFlow<List<WifiP2pDevice>> = _peerDeviceList.asStateFlow()

  private val _transferFileList = MutableStateFlow<List<FileItem>>(emptyList())
  val transferFileList: StateFlow<List<FileItem>> = _transferFileList.asStateFlow()

  private val _permissionAction = MutableStateFlow<PermissionAction?>(null)
  val permissionAction: StateFlow<PermissionAction?> = _permissionAction.asStateFlow()

  private val _dialogState = MutableStateFlow<DialogEvent?>(null)
  val dialogState: StateFlow<DialogEvent?> = _dialogState.asStateFlow()

  private val _navigationEvent = MutableStateFlow<NavigationEvent?>(null)
  val navigationEvent: StateFlow<NavigationEvent?> = _navigationEvent.asStateFlow()

  fun initializeWifiDirectManager(
    filesForTransfer: List<FileItem>,
    lifecycleScope: CoroutineScope
  ) {
    _transferFileList.value = filesForTransfer
    wifiDirectManager.hasSenderStartedConnection = false

    wifiDirectManager.apply {
      callbacks = this@LocalFileTransferViewModel
      lifecycleCoroutineScope = lifecycleScope as LifecycleCoroutineScope
      startWifiDirectManager(filesForTransfer)
      setAlertDialogShower(alertDialogShower)
    }
  }

  fun actionMenuItem() = listOf(
    ActionMenuItem(
      Vector(Icons.Default.Search),
      string.search_label,
      {
        onSearchMenuClicked()
      },
      testingTag = SEARCH_ICON_TESTING_TAG
    )
  )

  fun showDialog(dialog: DialogEvent) {
    _dialogState.value = dialog
  }

  fun clearDialogEvent() {
    _dialogState.value = null
  }

  fun clearNavigationEvent() {
    _navigationEvent.value = null
  }

  fun clearPermissionAction() {
    _permissionAction.value = null
  }

  override fun onCleared() {
    super.onCleared()
    wifiDirectManager.stopWifiDirectManager()
    wifiDirectManager.callbacks = null
  }

  private val customScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private fun onSearchMenuClicked() {
    customScope.launch {
      if (!checkFineLocationAccessPermission()) {
        Log.d(TAG, "Location permission not granted")
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

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isLocationServiceEnabled) {
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
    _isPeerSearching.value = true
  }

  val isLocationServiceEnabled: Boolean
    get() =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        true
      } else {
        isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
            isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
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
    _dialogState.value = DialogEvent.ShowEnableWifiP2p
  }

  private fun requestEnableLocationServices() {
    _dialogState.value = DialogEvent.ShowEnableLocationServices
  }

  fun onPermissionGranted() {
    _permissionAction.value = null
    viewModelScope.launch {
      onSearchMenuClicked()
    }
  }

  fun onPermissionDenied() {
    _permissionAction.value = null
    _navigationEvent.value = NavigationEvent.NavigateBack
  }

  private fun checkFineLocationAccessPermission(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val isGranted = ContextCompat.checkSelfPermission(
        application,
        NEARBY_WIFI_DEVICES
      ) == PackageManager.PERMISSION_GRANTED

      if (!isGranted) {
        _permissionAction.value = PermissionAction.RequestPermission(NEARBY_WIFI_DEVICES)
      }

      return isGranted
    }

    val isGranted = ContextCompat.checkSelfPermission(
      application,
      ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!isGranted) {
      _permissionAction.value = PermissionAction.RequestPermission(ACCESS_FINE_LOCATION)
    }

    return isGranted
  }

  private suspend fun checkExternalStorageWritePermission(): Boolean {
    if (!kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() &&
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    ) {
      val isGranted = ContextCompat.checkSelfPermission(
        application,
        WRITE_EXTERNAL_STORAGE
      ) == PackageManager.PERMISSION_GRANTED

      if (!isGranted) {
        _permissionAction.value = PermissionAction.RequestPermission(WRITE_EXTERNAL_STORAGE)
      }

      return isGranted
    }
    return true
  }

  override fun onUserDeviceDetailsAvailable(userDevice: WifiP2pDevice?) {
    if (userDevice != null) {
      _deviceName.value = userDevice.deviceName
      Log.d(TAG, getDeviceStatus(userDevice.status))
    }
  }

  override fun onConnectionToPeersLost() {
    _peerDeviceList.value = emptyList()
  }

  override fun updateListOfAvailablePeers(peers: WifiP2pDeviceList) {
    val deviceList: List<WifiP2pDevice> = ArrayList<WifiP2pDevice>(peers.deviceList)
    _isPeerSearching.value = false
    _peerDeviceList.value = deviceList
    if (deviceList.isEmpty()) {
      Log.d(TAG, "No devices found")
    }
  }

  override fun onFilesForTransferAvailable(filesForTransfer: List<FileItem>) {
    _transferFileList.value = filesForTransfer
  }

  override fun onFileStatusChanged(
    itemIndex: Int,
    fileStatus: FileItem.FileStatus
  ) {
    val tempTransferList = _transferFileList.value.toMutableList()
    if (itemIndex in tempTransferList.indices) {
      tempTransferList[itemIndex].fileStatus = fileStatus
      _transferFileList.value = tempTransferList
    }
  }

  override fun onFileTransferComplete() {
    _navigationEvent.value = NavigationEvent.NavigateBack
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


