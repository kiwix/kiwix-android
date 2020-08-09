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
package org.kiwix.kiwixmobile.localfiletransfer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_local_file_transfer.list_peer_devices
import kotlinx.android.synthetic.main.activity_local_file_transfer.progress_bar_searching_peers
import kotlinx.android.synthetic.main.activity_local_file_transfer.recycler_view_transfer_files
import kotlinx.android.synthetic.main.activity_local_file_transfer.text_view_device_name
import kotlinx.android.synthetic.main.activity_local_file_transfer.text_view_empty_peer_list
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.utils.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.KiwixDialog
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.localfiletransfer.WifiDirectManager.Companion.getDeviceStatus
import org.kiwix.kiwixmobile.localfiletransfer.adapter.WifiP2pDelegate
import org.kiwix.kiwixmobile.localfiletransfer.adapter.WifiPeerListAdapter
import java.util.ArrayList
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
 * 1) Handshake with the selected peer device, using [PeerGroupHandshakeAsyncTask]
 * 2) After handshake, starting the files transfer using [SenderDeviceAsyncTask] on the sender
 * device and [ReceiverDeviceAsyncTask] files receiving device
 */
@SuppressLint("GoogleAppIndexingApiWarning", "Registered")
class LocalFileTransferActivity : BaseActivity(),
  WifiDirectManager.Callbacks {
  @Inject
  lateinit var alertDialogShower: AlertDialogShower

  @Inject
  lateinit var wifiDirectManager: WifiDirectManager

  @Inject
  lateinit var locationManager: LocationManager

  private var isFileSender = false // Whether the device is the file sender or not
  private var filesForTransfer = ArrayList<FileItem>()
  private var fileListAdapter: FileListAdapter? = null
  private var wifiPeerListAdapter: WifiPeerListAdapter? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_local_file_transfer)

    /*
     * Presence of file Uris decides whether the device with the activity open is a sender or receiver:
     * - On the sender device, this activity is started from the app chooser post selection
     * of files to share in the Library
     * - On the receiver device, the activity is started directly from within the 'Get Content'
     * activity, without any file Uris
     * */
    val filesIntent = intent
    val fileUriArrayList: ArrayList<Uri>?
    fileUriArrayList = filesIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
    isFileSender = fileUriArrayList?.isNotEmpty() == true && fileUriArrayList.size > 0

    val toolbar: Toolbar =
      findViewById(R.id.toolbar)
    // I know this is not the best method, but android extensions were showing 'unknown reference' error
    setSupportActionBar(toolbar)
    toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp)
    toolbar.setNavigationOnClickListener { finish() }
    wifiPeerListAdapter = WifiPeerListAdapter(
      WifiP2pDelegate(wifiDirectManager::sendToDevice)
    )

    list_peer_devices?.adapter = wifiPeerListAdapter
    list_peer_devices?.layoutManager = LinearLayoutManager(this)
    list_peer_devices?.setHasFixedSize(true)
    if (isFileSender) {
      fileUriArrayList.map {
        filesForTransfer.add(FileItem(it))
      }
      displayFileTransferProgress(filesForTransfer)
    }
    wifiDirectManager.startWifiDirectManager(filesForTransfer)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.wifi_file_share_items, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    var status = false
    return if (item.itemId == R.id.menu_item_search_devices) {
      /* Permissions essential for this module */
      if (!checkCoarseLocationAccessPermission()) {
        status = true
      }
      if (!checkExternalStorageWritePermission()) {
        status = true
      }
      /* Initiate discovery */
      if (!wifiDirectManager.isWifiP2pEnabled) {
        requestEnableWifiP2pServices()
        status = true
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isLocationServiceEnabled) {
        requestEnableLocationServices()
        status = true
      }
      showPeerDiscoveryProgressBar()
      wifiDirectManager.discoverPeerDevices()
      status
    } else
      return super.onOptionsItemSelected(item)
  }

  private fun showPeerDiscoveryProgressBar() { // Setup UI for searching peers
    progress_bar_searching_peers?.visibility = View.VISIBLE
    list_peer_devices?.visibility = View.INVISIBLE
    text_view_empty_peer_list?.visibility = View.INVISIBLE
  }

  /* From WifiDirectManager.Callbacks interface */
  override fun onUserDeviceDetailsAvailable(userDevice: WifiP2pDevice?) {
    // Update UI with user device's details
    if (userDevice != null) {
      text_view_device_name?.text = userDevice.deviceName
      Log.d(
        TAG,
        getDeviceStatus(userDevice.status)
      )
    }
  }

  override fun onConnectionToPeersLost() {
    wifiPeerListAdapter?.items = emptyList()
  }

  override fun onFilesForTransferAvailable(filesForTransfer: ArrayList<FileItem>) {
    this.filesForTransfer = filesForTransfer
    displayFileTransferProgress(filesForTransfer)
  }

  private fun displayFileTransferProgress(filesToSend: ArrayList<FileItem>) {
    fileListAdapter = FileListAdapter(filesToSend)
    recycler_view_transfer_files?.adapter = fileListAdapter
    recycler_view_transfer_files?.layoutManager = LinearLayoutManager(this)
  }

  override fun onFileStatusChanged(itemIndex: Int) {
    fileListAdapter?.notifyItemChanged(itemIndex)
  }

  override fun updateListOfAvailablePeers(peers: WifiP2pDeviceList) {
    val deviceList: List<WifiP2pDevice> = ArrayList<WifiP2pDevice>(peers.deviceList)
    progress_bar_searching_peers?.visibility = View.GONE
    list_peer_devices?.visibility = View.VISIBLE
    wifiPeerListAdapter?.items = deviceList
    if (deviceList.isEmpty()) {
      Log.d(TAG, "No devices found")
    }
  }

  override fun onFileTransferComplete() {
    finish()
  }

  /* Helper methods used for checking permissions and states of services */
  private fun checkCoarseLocationAccessPermission(): Boolean {
    // Required by Android to detect wifi-p2p peers
    return if (ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
      )
      == PackageManager.PERMISSION_DENIED
    ) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(
          this,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      ) {
        alertDialogShower.show(KiwixDialog.LocationPermissionRationale,
          {
            ActivityCompat.requestPermissions(
              this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
              PERMISSION_REQUEST_CODE_COARSE_LOCATION
            )
          })
      } else {
        ActivityCompat.requestPermissions(
          this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
          PERMISSION_REQUEST_CODE_COARSE_LOCATION
        )
      }
      false
    } else {
      true
      // Control reaches here: Either permission granted at install time, or at the time of request
    }
  }

  private fun checkExternalStorageWritePermission(): Boolean { // To access and store the zims
    return if (ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      )
      == PackageManager.PERMISSION_DENIED
    ) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(
          this,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
      ) {
        alertDialogShower.show(KiwixDialog.StoragePermissionRationale, {
          ActivityCompat.requestPermissions(
            this@LocalFileTransferActivity,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS
          )
        })
      } else {
        ActivityCompat.requestPermissions(
          this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
          PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS
        )
      }
      false
    } else {
      true
      // Control reaches here: Either permission granted at install time, or at the time of request
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
      when (requestCode) {
        PERMISSION_REQUEST_CODE_COARSE_LOCATION -> {
          Log.e(
            TAG,
            "Location permission not granted"
          )
          Toast.makeText(
            this,
            R.string.permission_refused_location,
            Toast.LENGTH_LONG
          ).show()

          finish()
        }
        PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS -> {
          Log.e(
            TAG,
            "Storage write permission not granted"
          )
          Toast.makeText(
            this,
            R.string.permission_refused_storage,
            Toast.LENGTH_LONG
          ).show()

          finish()
        }
        else -> {
          super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
      }
    }
  }

  private val isLocationServiceEnabled: Boolean
    get() = (isProviderEnabled(LocationManager.GPS_PROVIDER) ||
      isProviderEnabled(LocationManager.NETWORK_PROVIDER))

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
        startActivityForResult(
          Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
          REQUEST_ENABLE_LOCATION_SERVICES
        )
      }, Toast.makeText(
        this@LocalFileTransferActivity, R.string.discovery_needs_location,
        Toast.LENGTH_SHORT
      )::show
    )
  }

  private fun requestEnableWifiP2pServices() {
    alertDialogShower.show(
      KiwixDialog.EnableWifiP2pServices, {
        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
      }, Toast.makeText(
        this@LocalFileTransferActivity, R.string.discovery_needs_wifi,
        Toast.LENGTH_SHORT
      )::show
    )
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    when (requestCode) {
      REQUEST_ENABLE_LOCATION_SERVICES -> {
        if (!isLocationServiceEnabled) {
          Toast.makeText(
            this,
            R.string.permission_refused_location,
            Toast.LENGTH_LONG
          ).show()
        }
      }
      else -> {
        super.onActivityResult(requestCode, resultCode, data)
      }
    }
  }

  override fun onDestroy() {
    wifiDirectManager.stopWifiDirectManager()
    super.onDestroy()
  }

  override fun injection(coreComponent: CoreComponent) {
    this.kiwixActivityComponent.inject(this)
  }

  companion object {
    // Not a typo, 'Log' tags have a length upper limit of 25 characters
    const val TAG = "LocalFileTransferActvty"
    const val REQUEST_ENABLE_LOCATION_SERVICES = 1
    private const val PERMISSION_REQUEST_CODE_COARSE_LOCATION = 1
    private const val PERMISSION_REQUEST_CODE_STORAGE_WRITE_ACCESS = 2
  }
}
