/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.webserver

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.app.Dialog
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
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.CoreApp.Companion.coreComponent
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.databinding.ActivityZimHostBinding
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.cachedComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.hasNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.requestNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.utils.ConnectivityReporter
import org.kiwix.kiwixmobile.core.utils.REQUEST_POST_NOTIFICATION_PERMISSION
import org.kiwix.kiwixmobile.core.utils.ServerUtils
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog.StartServer
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BookOnDiskDelegate
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskAdapter
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.webserver.wifi_hotspot.HotspotService
import org.kiwix.kiwixmobile.core.webserver.wifi_hotspot.HotspotService.Companion.ACTION_CHECK_IP_ADDRESS
import org.kiwix.kiwixmobile.core.webserver.wifi_hotspot.HotspotService.Companion.ACTION_START_SERVER
import org.kiwix.kiwixmobile.core.webserver.wifi_hotspot.HotspotService.Companion.ACTION_STOP_SERVER
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

  @Inject
  lateinit var zimReaderFactory: ZimFileReader.Factory

  private lateinit var booksAdapter: BooksOnDiskAdapter
  private lateinit var bookDelegate: BookOnDiskDelegate.BookDelegate
  private var hotspotService: HotspotService? = null
  private var ip: String? = null
  private lateinit var serviceConnection: ServiceConnection
  private var dialog: Dialog? = null
  private var activityZimHostBinding: ActivityZimHostBinding? = null
  override val fragmentTitle: String? by lazy {
    getString(R.string.menu_wifi_hotspot)
  }
  override val fragmentToolbar: Toolbar? by lazy {
    activityZimHostBinding?.root?.findViewById(R.id.toolbar)
  }
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
  ): View? {
    activityZimHostBinding = ActivityZimHostBinding.inflate(inflater, container, false)
    return activityZimHostBinding?.root
  }

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    coreComponent
      .activityComponentBuilder()
      .activity(requireActivity())
      .build()
      .inject(this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    bookDelegate =
      BookOnDiskDelegate.BookDelegate(sharedPreferenceUtil, multiSelectAction = ::select)
    bookDelegate.selectionMode = SelectionMode.MULTI
    booksAdapter = BooksOnDiskAdapter(
      bookDelegate,
      BookOnDiskDelegate.LanguageDelegate
    )

    activityZimHostBinding?.recyclerViewZimHost?.adapter = booksAdapter
    presenter.attachView(this)

    serviceConnection = object : ServiceConnection {
      override fun onServiceDisconnected(name: ComponentName?) {
        /*do nothing*/
      }

      override fun onServiceConnected(className: ComponentName, service: IBinder) {
        hotspotService = (service as HotspotService.HotspotBinder).service.get()
        hotspotService?.registerCallBack(this@ZimHostFragment)
      }
    }

    activityZimHostBinding?.startServerButton?.setOnClickListener {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
        checkNearbyWifiDevicesPermission()
      ) {
        if (requireActivity().hasNotificationPermission(sharedPreferenceUtil)) {
          startStopServer()
        } else {
          requestNotificationPermission()
        }
      } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ||
        checkCoarseLocationAccessPermission()
      ) {
        startStopServer()
      }
    }
  }

  private fun requestNotificationPermission() {
    if (!ActivityCompat.shouldShowRequestPermissionRationale(
        requireActivity(),
        POST_NOTIFICATIONS
      )
    ) {
      requireActivity().requestNotificationPermission()
    } else {
      alertDialogShower.show(
        KiwixDialog.NotificationPermissionDialog,
        requireActivity()::navigateToAppSettings
      )
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

  private fun checkNearbyWifiDevicesPermission(): Boolean =
    if (ContextCompat.checkSelfPermission(
        requireActivity(),
        Manifest.permission.NEARBY_WIFI_DEVICES
      ) == PackageManager.PERMISSION_DENIED
    ) {
      if (ActivityCompat.shouldShowRequestPermissionRationale(
          requireActivity(),
          Manifest.permission.NEARBY_WIFI_DEVICES
        )
      ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          alertDialogShower.show(
            KiwixDialog.NearbyWifiPermissionRationaleOnHostZimFile,
            ::askNearbyWifiDevicesPermission
          )
        }
      } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          askNearbyWifiDevicesPermission()
        }
      }
      false
    } else {
      true
    }

  @Suppress("DEPRECATION")
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      if (requestCode == PERMISSION_REQUEST_CODE_COARSE_LOCATION ||
        requestCode == REQUEST_POST_NOTIFICATION_PERMISSION
      ) {
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

  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  private fun askNearbyWifiDevicesPermission() {
    ActivityCompat.requestPermissions(
      requireActivity(), arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES),
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

  @SuppressLint("InflateParams")
  private fun startKiwixHotspot() {
    if (dialog == null) {
      val dialogView: View =
        layoutInflater.inflate(R.layout.item_custom_spinner, null)
      dialog = alertDialogShower.create(StartServer { dialogView })
    }
    dialog?.show()
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
    activityZimHostBinding?.serverTextView?.apply {
      text = getString(R.string.server_started_message, ip)
      movementMethod = LinkMovementMethod.getInstance()
    }
    configureUrlSharingIcon()
    activityZimHostBinding?.startServerButton?.text = getString(R.string.stop_server_label)
    activityZimHostBinding?.startServerButton?.setBackgroundColor(
      ContextCompat.getColor(requireActivity(), R.color.stopServerRed)
    )
    bookDelegate.selectionMode = SelectionMode.NORMAL
    booksAdapter.notifyDataSetChanged()
  }

  private fun configureUrlSharingIcon() {
    activityZimHostBinding?.shareServerUrlIcon?.apply {
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
    activityZimHostBinding?.serverTextView?.text =
      getString(R.string.server_textview_default_message)
    activityZimHostBinding?.shareServerUrlIcon?.visibility = View.GONE
    activityZimHostBinding?.startServerButton?.text = getString(R.string.start_server_label)
    activityZimHostBinding?.startServerButton?.setBackgroundColor(
      ContextCompat.getColor(requireActivity(), R.color.startServerGreen)
    )
    bookDelegate.selectionMode = SelectionMode.MULTI
    booksAdapter.notifyDataSetChanged()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    activityZimHostBinding?.recyclerViewZimHost?.adapter = null
    hotspotService?.registerCallBack(null)
    presenter.detachView()
    activityZimHostBinding = null
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

  @Suppress("NestedBlockDepth")
  override fun addBooks(books: List<BooksOnDiskListItem>) {
    // Check if this is the app module, as custom apps may have multiple package names
    if (requireActivity().packageName == "org.kiwix.kiwixmobile") {
      booksAdapter.items = books
    } else {
      val updatedBooksList: MutableList<BooksOnDiskListItem> = arrayListOf()
      books.forEach {
        if (it is BookOnDisk) {
          zimReaderFactory.create(it.file)?.let { zimFileReader ->
            updatedBooksList.add(BookOnDisk(zimFileReader.zimFile, zimFileReader)).also {
              zimFileReader.dispose()
            }
          }
        } else {
          updatedBooksList.add(it)
        }
      }
      booksAdapter.items = updatedBooksList
    }
  }

  override fun onIpAddressValid() {
    dialog?.dismiss()
    requireActivity().startService(
      createHotspotIntent(ACTION_START_SERVER).putStringArrayListExtra(
        SELECTED_ZIM_PATHS_KEY, selectedBooksPath
      )
    )
  }

  override fun onIpAddressInvalid() {
    dialog?.dismiss()
    toast(R.string.server_failed_message, Toast.LENGTH_SHORT)
  }

  companion object {
    const val SELECTED_ZIM_PATHS_KEY = "selected_zim_paths"
    const val PERMISSION_REQUEST_CODE_COARSE_LOCATION = 10
  }
}
