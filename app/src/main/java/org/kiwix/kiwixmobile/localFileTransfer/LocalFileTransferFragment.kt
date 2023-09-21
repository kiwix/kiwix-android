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

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.NEARBY_WIFI_DEVICES
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.popNavigationBackstack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.databinding.FragmentLocalFileTransferBinding
import org.kiwix.kiwixmobile.localFileTransfer.WifiDirectManager.Companion.getDeviceStatus
import org.kiwix.kiwixmobile.localFileTransfer.adapter.WifiP2pDelegate
import org.kiwix.kiwixmobile.localFileTransfer.adapter.WifiPeerListAdapter
import org.kiwix.kiwixmobile.core.webserver.ZimHostFragment.Companion.PERMISSION_REQUEST_CODE_COARSE_LOCATION
import javax.inject.Inject

/**
 * Created by @Aditya-Sood as a part of GSoC 2019.
 *
 * This activity is the starting point for the module used for sharing zims between devices.
 *
 * The module is used for transferring ZIM files from one device to another, from within the
 * app. Two devices are connected to each other using WiFi Direct, followed by file transfer.
 *
 * File transfer involves two phases:
 * 1) Handshake with the selected peer device, using [PeerGroupHandshake]
 * 2) After handshake, starting the files transfer using [SenderDevice] on the sender
 * device and [ReceiverDevice] files receiving device
 */

const val URIS_KEY = "uris"

@SuppressLint("GoogleAppIndexingApiWarning", "Registered")
class LocalFileTransferFragment :
  BaseFragment(),
  WifiDirectManager.Callbacks {
  @Inject
  lateinit var alertDialogShower: AlertDialogShower

  @Inject
  lateinit var wifiDirectManager: WifiDirectManager

  @Inject
  lateinit var locationManager: LocationManager

  @Inject
  lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  private var fileListAdapter: FileListAdapter? = null
  private var wifiPeerListAdapter: WifiPeerListAdapter? = null
  private var fragmentLocalFileTransferBinding: FragmentLocalFileTransferBinding? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    fragmentLocalFileTransferBinding =
      FragmentLocalFileTransferBinding.inflate(inflater, container, false)
    return fragmentLocalFileTransferBinding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupMenu()
    val activity = requireActivity() as CoreMainActivity
    val filesForTransfer = getFilesForTransfer()
    val isReceiver = filesForTransfer.isEmpty()
    setupToolbar(view, activity, isReceiver)

    wifiPeerListAdapter = WifiPeerListAdapter(WifiP2pDelegate(wifiDirectManager::sendToDevice))

    setupPeerDevicesList(activity)

    displayFileTransferProgress(filesForTransfer)

    wifiDirectManager.callbacks = this
    wifiDirectManager.lifecycleCoroutineScope = lifecycleScope
    wifiDirectManager.startWifiDirectManager(filesForTransfer)
  }

  private fun setupMenu() {
    (requireActivity() as MenuHost).addMenuProvider(
      object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
          menuInflater.inflate(R.menu.wifi_file_share_items, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
          if (menuItem.itemId == R.id.menu_item_search_devices) {
            /* Permissions essential for this module */
            return when {
              !checkFineLocationAccessPermission() ->
                true
              !checkExternalStorageWritePermission() ->
                true
              /* Initiate discovery */
              !wifiDirectManager.isWifiP2pEnabled -> {
                requestEnableWifiP2pServices()
                true
              }
              Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isLocationServiceEnabled -> {
                requestEnableLocationServices()
                true
              }
              else -> {
                showPeerDiscoveryProgressBar()
                wifiDirectManager.discoverPeerDevices()
                true
              }
            }
          }
          return false
        }
      },
      viewLifecycleOwner,
      Lifecycle.State.RESUMED
    )
  }

  private fun setupPeerDevicesList(activity: CoreMainActivity) {
    fragmentLocalFileTransferBinding?.listPeerDevices?.apply {
      adapter = wifiPeerListAdapter
      layoutManager = LinearLayoutManager(activity)
      setHasFixedSize(true)
    }
  }

  private fun setupToolbar(view: View, activity: CoreMainActivity, isReceiver: Boolean) {
    val toolbar: Toolbar = view.findViewById(R.id.toolbar)
    activity.setSupportActionBar(toolbar)
    toolbar.title =
      if (isReceiver) getString(R.string.receive_files_title)
      else getString(R.string.send_files_title)
    toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
    toolbar.setNavigationOnClickListener { activity.popNavigationBackstack() }
  }

  private fun getFilesForTransfer() =
    LocalFileTransferFragmentArgs.fromBundle(requireArguments()).uris?.map(::FileItem)
      ?: emptyList()

  private fun showPeerDiscoveryProgressBar() { // Setup UI for searching peers
    fragmentLocalFileTransferBinding?.progressBarSearchingPeers?.visibility = View.VISIBLE
    fragmentLocalFileTransferBinding?.listPeerDevices?.visibility = View.INVISIBLE
    fragmentLocalFileTransferBinding?.textViewEmptyPeerList?.visibility = View.INVISIBLE
  }

  /* From WifiDirectManager.Callbacks interface */
  override fun onUserDeviceDetailsAvailable(userDevice: WifiP2pDevice?) {
    // Update UI with user device's details
    if (userDevice != null) {
      fragmentLocalFileTransferBinding?.textViewDeviceName?.text = userDevice.deviceName
      Log.d(TAG, getDeviceStatus(userDevice.status))
    }
  }

  override fun onConnectionToPeersLost() {
    wifiPeerListAdapter?.items = emptyList()
  }

  override fun onFilesForTransferAvailable(filesForTransfer: List<FileItem>) {
    displayFileTransferProgress(filesForTransfer)
  }

  private fun displayFileTransferProgress(filesToSend: List<FileItem>) {
    fileListAdapter = FileListAdapter(filesToSend)
    fragmentLocalFileTransferBinding?.recyclerViewTransferFiles?.apply {
      adapter = fileListAdapter
      layoutManager =
        LinearLayoutManager(requireActivity())
    }
  }

  override fun onFileStatusChanged(itemIndex: Int) {
    fileListAdapter?.notifyItemChanged(itemIndex)
  }

  override fun updateListOfAvailablePeers(peers: WifiP2pDeviceList) {
    val deviceList: List<WifiP2pDevice> = ArrayList<WifiP2pDevice>(peers.deviceList)
    fragmentLocalFileTransferBinding?.progressBarSearchingPeers?.visibility = View.GONE
    fragmentLocalFileTransferBinding?.listPeerDevices?.visibility = View.VISIBLE
    wifiPeerListAdapter?.items = deviceList
    if (deviceList.isEmpty()) {
      Log.d(TAG, "No devices found")
    }
  }

  override fun onFileTransferComplete() {
    requireActivity().popNavigationBackstack()
  }

  /* Helper methods used for checking permissions and states of services */
  private fun checkFineLocationAccessPermission(): Boolean {
    // Required by Android to detect wifi-p2p peers
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      return permissionIsGranted(NEARBY_WIFI_DEVICES).also { permissionGranted ->
        if (!permissionGranted) {
          if (shouldShowRationale(NEARBY_WIFI_DEVICES)) {
            alertDialogShower.show(
              KiwixDialog.NearbyWifiPermissionRationaleOnHostZimFile,
              ::askNearbyWifiDevicesPermission
            )
          } else {
            askNearbyWifiDevicesPermission()
          }
        }
      }
    }
    return permissionIsGranted(ACCESS_FINE_LOCATION).also { permissionGranted ->
      if (!permissionGranted) {
        if (shouldShowRationale(ACCESS_FINE_LOCATION)) {
          alertDialogShower.show(
            KiwixDialog.LocationPermissionRationale,
            ::requestLocationPermission
          )
        } else {
          requestLocationPermission()
        }
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private fun askNearbyWifiDevicesPermission() {
    ActivityCompat.requestPermissions(
      requireActivity(), arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES),
      PERMISSION_REQUEST_CODE_COARSE_LOCATION
    )
  }

  private fun requestLocationPermission() {
    requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, PERMISSION_REQUEST_FINE_LOCATION)
  }

  private fun checkExternalStorageWritePermission(): Boolean { // To access and store the zims
    if (!sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove() &&
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    ) {
      return permissionIsGranted(WRITE_EXTERNAL_STORAGE).also { permissionGranted ->
        if (!permissionGranted) {
          if (shouldShowRationale(WRITE_EXTERNAL_STORAGE)) {
            alertDialogShower.show(
              KiwixDialog.StoragePermissionRationale,
              ::requestStoragePermissionPermission
            )
          } else {
            requestStoragePermissionPermission()
          }
        }
      }
    }
    return true
  }

  private fun shouldShowRationale(writeExternalStorage: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), writeExternalStorage)

  private fun permissionIsGranted(writeExternalStorage: String) =
    ContextCompat.checkSelfPermission(requireActivity(), writeExternalStorage) == PERMISSION_GRANTED

  private fun requestStoragePermissionPermission() {
    requestPermission(WRITE_EXTERNAL_STORAGE, PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS)
  }

  private fun requestPermission(permission: String, requestCode: Int) {
    ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), requestCode)
  }

  @Suppress("DEPRECATION")
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    if (grantResults[0] == PERMISSION_DENIED) {
      when (requestCode) {
        PERMISSION_REQUEST_FINE_LOCATION -> {
          Log.e(TAG, "Location permission not granted")
          toast(R.string.permission_refused_location, Toast.LENGTH_SHORT)
          requireActivity().popNavigationBackstack()
        }
        PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS -> {
          Log.e(TAG, "Storage write permission not granted")
          toast(R.string.permission_refused_storage, Toast.LENGTH_SHORT)
          requireActivity().popNavigationBackstack()
        }
        else ->
          super.onRequestPermissionsResult(requestCode, permissions, grantResults)
      }
    }
  }

  private val isLocationServiceEnabled: Boolean
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      true
    } else {
      isProviderEnabled(GPS_PROVIDER) || isProviderEnabled(NETWORK_PROVIDER)
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

  private fun requestEnableLocationServices() {
    alertDialogShower.show(
      KiwixDialog.EnableLocationServices, {
        enableLocationServicesLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
      },
      { toast(R.string.discovery_needs_location, Toast.LENGTH_SHORT) }
    )
  }

  private fun requestEnableWifiP2pServices() {
    alertDialogShower.show(
      KiwixDialog.EnableWifiP2pServices, {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
      },
      { toast(R.string.discovery_needs_wifi, Toast.LENGTH_SHORT) }
    )
  }

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  private val enableLocationServicesLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode != Activity.RESULT_OK) {
        if (!isLocationServiceEnabled) {
          toast(R.string.permission_refused_location, Toast.LENGTH_SHORT)
        }
      }
    }

  override fun onDestroyView() {
    wifiDirectManager.stopWifiDirectManager()
    wifiDirectManager.callbacks = null
    fragmentLocalFileTransferBinding = null
    super.onDestroyView()
  }

  companion object {
    // Not a typo, 'Log' tags have a length upper limit of 25 characters
    const val TAG = "LocalFileTransferActvty"
    private const val PERMISSION_REQUEST_FINE_LOCATION = 2
    private const val PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS = 3
  }
}
