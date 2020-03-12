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

import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.utils.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.ServerUtils
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BookOnDiskDelegate
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskAdapter
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotService
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotService.ACTION_CHECK_IP_ADDRESS
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotService.ACTION_START_SERVER
import org.kiwix.kiwixmobile.webserver.wifi_hotspot.HotspotService.ACTION_STOP_SERVER
import java.util.ArrayList
import javax.inject.Inject

class ZimHostActivity : BaseActivity(), ZimHostCallbacks, ZimHostContract.View {
  @Inject
  internal lateinit var presenter: ZimHostContract.Presenter
  @Inject
  internal lateinit var alertDialogShower: AlertDialogShower
  private lateinit var recyclerViewZimHost: RecyclerView
  private lateinit var toolbar: Toolbar
  private lateinit var startServerButton: Button
  private lateinit var serverTextView: TextView
  private lateinit var booksAdapter: BooksOnDiskAdapter
  private lateinit var bookDelegate: BookOnDiskDelegate.BookDelegate
  private var hotspotService: HotspotService? = null
  private var ip: String? = null
  private var serviceConnection: ServiceConnection? = null
  private var progressDialog: ProgressDialog? = null
  private val tag = "ZimHostActivity"

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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_zim_host)
    recyclerViewZimHost = findViewById(R.id.recyclerViewZimHost)
    toolbar = findViewById(R.id.toolbar)
    startServerButton = findViewById(R.id.startServerButton)
    serverTextView = findViewById(R.id.serverTextView)
    setUpToolbar()

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

      override fun onServiceConnected(className: ComponentName, service: IBinder) {
        hotspotService = (service as HotspotService.HotspotBinder).service
        hotspotService!!.registerCallBack(this@ZimHostActivity)
      }

      override fun onServiceDisconnected(arg0: ComponentName) {}
    }

    startServerButton.setOnClickListener { startStopServer() }
  }

  override fun injection(coreComponent: CoreComponent) {
    kiwixActivityComponent.inject(this)
  }

  private fun startStopServer() {
    when {
      ServerUtils.isServerStarted -> stopServer()
      selectedBooksPath.size > 0 -> startHotspotManuallyDialog()
      else -> toast(R.string.no_books_selected_toast_message, Toast.LENGTH_SHORT)
    }
  }

  private fun stopServer() {
    startService(createHotspotIntent(ACTION_STOP_SERVER))
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
    bindService(
      Intent(this, HotspotService::class.java), serviceConnection,
      Context.BIND_AUTO_CREATE
    )
  }

  private fun unbindService() {
    hotspotService?.let {
      unbindService(serviceConnection)
      it.registerCallBack(null)
    }
  }

  override fun onResume() {
    super.onResume()
    presenter.loadBooks(sharedPreferenceUtil.hostedBooks)
    if (ServerUtils.isServerStarted) {
      ip = ServerUtils.getSocketAddress()
      layoutServerStarted()
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
    serverTextView.text = getString(R.string.server_started_message, ip)
    startServerButton.text = getString(R.string.stop_server_label)
    startServerButton.setBackgroundColor(resources.getColor(R.color.stopServer))
    bookDelegate.selectionMode = SelectionMode.NORMAL
    booksAdapter.notifyDataSetChanged()
  }

  private fun layoutServerStopped() {
    serverTextView.text = getString(R.string.server_textview_default_message)
    startServerButton.text = getString(R.string.start_server_label)
    startServerButton.setBackgroundColor(resources.getColor(R.color.greenTick))
    bookDelegate.selectionMode = SelectionMode.MULTI
    booksAdapter.notifyDataSetChanged()
  }

  override fun onDestroy() {
    super.onDestroy()
    presenter.detachView()
  }

  private fun setUpToolbar() {
    setSupportActionBar(toolbar)
    supportActionBar!!.title = getString(R.string.menu_host_books)
    supportActionBar!!.setHomeButtonEnabled(true)
    supportActionBar!!.setDisplayHomeAsUpEnabled(true)

    toolbar.setNavigationOnClickListener { onBackPressed() }
  }

  // Advice user to turn on hotspot manually for API<26
  private fun startHotspotManuallyDialog() {

    alertDialogShower.show(KiwixDialog.StartHotspotManually,
      ::launchTetheringSettingsScreen,
      {},
      {
        progressDialog = ProgressDialog.show(
          this,
          getString(R.string.progress_dialog_starting_server), "",
          true
        )
        startService(createHotspotIntent(ACTION_CHECK_IP_ADDRESS))
      }
    )
  }

  private fun createHotspotIntent(action: String): Intent =
    Intent(this, HotspotService::class.java).setAction(action)

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
    startService(
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
  }
}
