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

package org.kiwix.kiwixmobile.webserver

import android.Manifest
import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_zim_host.recyclerViewZimHost
import kotlinx.android.synthetic.main.activity_zim_host.serverTextView
import kotlinx.android.synthetic.main.activity_zim_host.shareServerUrlIcon
import kotlinx.android.synthetic.main.activity_zim_host.startServerButton
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.utils.ConnectivityReporter
import org.kiwix.kiwixmobile.core.utils.ServerUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BookOnDiskDelegate
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskAdapter
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotService
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotService.ACTION_CHECK_IP_ADDRESS
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotService.ACTION_START_SERVER
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotService.ACTION_STOP_SERVER
import java.util.ArrayList
import javax.inject.Inject

class ZimHostFragment : BaseFragment(), ZimHostCallbacks, ZimHostContract.View {
  @Inject
  internal lateinit var presenter: ZimHostContract.Presenter

  @Inject
  internal lateinit var connectivityReporter: ConnectivityReporter

  @Inject
  internal lateinit var alertDialogShower: AlertDialogShower

  @Inject
  lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  private lateinit var booksAdapter: BooksOnDiskAdapter
  private lateinit var bookDelegate: BookOnDiskDelegate.BookDelegate
  private var hotspotService: HotspotService? = null
  private var ip: String? = null
  private lateinit var serviceConnection: ServiceConnection
  private var progressDialog: ProgressDialog? = null

  private val selectedBooksPath: ArrayList<String>
    get() {
      return booksAdapter.items
        .filter(BooksOnDiskListItem::isSelected)
        .filterIsInstance<BookOnDisk>()
        .map {
          it.file.absolutePath
        }
        .also {
          if (BuildConfig.DEBUG) {
            it.forEach { path ->
              Log.v(tag, "ZIM PATH : $path")
            }
          }
        }
        as ArrayList<String>
    }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.activity_zim_host, container, false)

  override fun inject(baseActivity: BaseActivity) {
    (baseActivity as KiwixMainActivity).cachedComponent.inject(this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setUpToolbar(view)

    bookDelegate =
      BookOnDiskDelegate.BookDelegate(sharedPreferenceUtil, multiSelectAction = ::select)
    bookDelegate.selectionMode = SelectionMode.MULTI
    booksAdapter = BooksOnDiskAdapter(
      bookDelegate,
      BookOnDiskDelegate.LanguageDelegate
    )

    recyclerViewZimHost.adapter = booksAdapter
    presenter.attachView(this)

    serviceConnection = object : ServiceConnection {
      override fun onServiceDisconnected(name: ComponentName?) {
        /*do nothing*/
      }

      override fun onServiceConnected(className: ComponentName, service: IBinder) {
        hotspotService = (service as HotspotService.HotspotBinder).service
        hotspotService!!.registerCallBack(this@ZimHostFragment)
      }
    }

    startServerButton.setOnClickListener {
      if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P || checkCoarseLocationAccessPermission()) {
        startStopServer()
      }
    }
  }

  private fun checkCoarseLocationAccessPermission(): Boolean =
    if (ContextCompat.checkSelfPermission(
        requireActivity(),
        Manifest.permission.ACCESS_COARSE_LOCATION
      ) == PackageManager.PERMISSION_DENIED
    ) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(
          requireActivity(),
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      ) {
        alertDialogShower.show(
          KiwixDialog.LocationPermissionRationaleOnHostZimFile,
          ::askCoarseLocationPermission
        )
      } else {
        askCoarseLocationPermission()
      }
      false
    } else {
      true
    }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      if (requestCode == PERMISSION_REQUEST_CODE_COARSE_LOCATION) {
        startStopServer()
      }
    }
  }

  private fun askCoarseLocationPermission() {
    ActivityCompat.requestPermissions(
      requireActivity(), arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
      PERMISSION_REQUEST_CODE_COARSE_LOCATION
    )
  }

  private fun startStopServer() {
    when {
      ServerUtils.isServerStarted -> stopServer()
      selectedBooksPath.size > 0 -> {
        when {
          connectivityReporter.checkWifi() -> startWifiDialog()
          connectivityReporter.checkTethering() -> startKiwixHotspot()
          else -> startHotspotManuallyDialog()
        }
      }
      else -> toast(R.string.no_books_selected_toast_message, Toast.LENGTH_SHORT)
    }
  }

  private fun startKiwixHotspot() {
    progressDialog = ProgressDialog.show(
      requireActivity(),
      getString(R.string.progress_dialog_starting_server), "",
      true
    )
    requireActivity().startService(createHotspotIntent(ACTION_CHECK_IP_ADDRESS))
  }

  private fun stopServer() {
    requireActivity().startService(createHotspotIntent(ACTION_STOP_SERVER))
  }

  private fun select(bookOnDisk: BooksOnDiskListItem.BookOnDisk) {
    val booksList: List<BooksOnDiskListItem> = booksAdapter.items.map {
      if (it == bookOnDisk) {
        it.isSelected = !it.isSelected
      }
      it
    }
    booksAdapter.items = booksList
    saveHostedBooks(booksList)
  }

  override fun onStart() {
    super.onStart()
    bindService()
  }

  override fun onStop() {
    super.onStop()
    unbindService()
  }

  private fun bindService() {
    requireActivity().bindService(
      Intent(requireActivity(), HotspotService::class.java), serviceConnection,
      Context.BIND_AUTO_CREATE
    )
  }

  private fun unbindService() {
    hotspotService?.let {
      requireActivity().unbindService(serviceConnection)
    }
  }

  override fun onResume() {
    super.onResume()
    presenter.loadBooks(sharedPreferenceUtil.hostedBooks)
    if (ServerUtils.isServerStarted) {
      ip = ServerUtils.getSocketAddress()
      layoutServerStarted()
    } else {
      layoutServerStopped()
    }
  }

  private fun saveHostedBooks(booksList: List<BooksOnDiskListItem>) {
    sharedPreferenceUtil.hostedBooks = booksList.asSequence()
      .filter(BooksOnDiskListItem::isSelected)
      .filterIsInstance<BookOnDisk>()
      .mapNotNull { it.book.title }
      .toSet()
  }

  private fun layoutServerStarted() {
    serverTextView.apply {
      text = getString(R.string.server_started_message, ip)
      movementMethod = LinkMovementMethod.getInstance()
    }
    configureUrlSharingIcon()
    startServerButton.text = getString(R.string.stop_server_label)
    startServerButton.setBackgroundColor(resources.getColor(R.color.stopServerRed))
    bookDelegate.selectionMode = SelectionMode.NORMAL
    booksAdapter.notifyDataSetChanged()
  }

  private fun configureUrlSharingIcon() {
    shareServerUrlIcon.apply {
      visibility = View.VISIBLE
      setOnClickListener {
        val urlSharingIntent = Intent(Intent.ACTION_SEND)
        urlSharingIntent.apply {
          type = "text/plain"
          putExtra(Intent.EXTRA_TEXT, ip)
        }
        startActivity(urlSharingIntent)
      }
    }
  }

  private fun layoutServerStopped() {
    serverTextView.text = getString(R.string.server_textview_default_message)
    shareServerUrlIcon.visibility = View.GONE
    startServerButton.text = getString(R.string.start_server_label)
    startServerButton.setBackgroundColor(resources.getColor(R.color.startServerGreen))
    bookDelegate.selectionMode = SelectionMode.MULTI
    booksAdapter.notifyDataSetChanged()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    recyclerViewZimHost.adapter = null
    hotspotService?.registerCallBack(null)
    presenter.detachView()
  }

  private fun setUpToolbar(view: View) {
    val activity = requireActivity() as AppCompatActivity
    activity.setSupportActionBar(view.findViewById(R.id.toolbar))
    activity.supportActionBar?.apply {
      title = getString(R.string.menu_wifi_hotspot)
      setHomeButtonEnabled(true)
      setDisplayHomeAsUpEnabled(true)
    }
  }

  // Advice user to turn on hotspot manually for API<26
  private fun startHotspotManuallyDialog() {

    alertDialogShower.show(
      KiwixDialog.StartHotspotManually,
      ::launchTetheringSettingsScreen,
      ::openWifiSettings,
      {}
    )
  }

  private fun startWifiDialog() {
    alertDialogShower.show(
      KiwixDialog.WiFiOnWhenHostingBooks,
      ::openWifiSettings,
      {},
      ::startKiwixHotspot
    )
  }

  private fun openWifiSettings() {
    startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
  }

  private fun createHotspotIntent(action: String): Intent =
    Intent(requireActivity(), HotspotService::class.java).setAction(action)

  override fun onServerStarted(ip: String) {
    this.ip = ip
    layoutServerStarted()
  }

  override fun onServerStopped() {
    layoutServerStopped()
  }

  override fun onServerFailedToStart() {
    toast(R.string.server_failed_toast_message)
  }

  private fun launchTetheringSettingsScreen() {
    startActivity(
      Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        component = ComponentName("com.android.settings", "com.android.settings.TetherSettings")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
    )
  }

  override fun addBooks(books: List<BooksOnDiskListItem>) {
    booksAdapter.items = books
  }

  override fun onIpAddressValid() {
    progressDialog!!.dismiss()
    requireActivity().startService(
      createHotspotIntent(ACTION_START_SERVER).putStringArrayListExtra(
        SELECTED_ZIM_PATHS_KEY, selectedBooksPath
      )
    )
  }

  override fun onIpAddressInvalid() {
    progressDialog!!.dismiss()
    toast(R.string.server_failed_message, Toast.LENGTH_SHORT)
  }

  companion object {
    const val SELECTED_ZIM_PATHS_KEY = "selected_zim_paths"
    private const val PERMISSION_REQUEST_CODE_COARSE_LOCATION = 10
  }
}
