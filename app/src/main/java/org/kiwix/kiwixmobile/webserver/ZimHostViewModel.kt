/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.qr.GenerateQR
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.StartServerGreen
import org.kiwix.kiwixmobile.core.ui.theme.StopServerRed
import org.kiwix.kiwixmobile.core.utils.ConnectivityReporter
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.ServerUtils
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.AllFilesPermissionDialog
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.AskNotificationPermission
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.AskReadWritePermission
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.DismissDialog
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.NotificationPermissionRationaleDialog
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.ReadPermissionRationaleDialog
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.ShowErrorToast
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.ShowManualHotspotDialog
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.ShowNoBooksToast
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.ShowWifiDialog
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.StartIpCheck
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.StartServer
import org.kiwix.kiwixmobile.webserver.ZimHostViewModel.Event.StopServer
import javax.inject.Inject

@Suppress("LongParameterList")
class ZimHostViewModel @Inject constructor(
  private val context: Application,
  private val dataSource: DataSource,
  private val kiwixDataStore: KiwixDataStore,
  private val generateQr: GenerateQR,
  private val connectivityReporter: ConnectivityReporter,
  private val zimReaderContainer: ZimReaderContainer,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  private val kiwixPermissionChecker: KiwixPermissionChecker
) : ViewModel(), ZimHostCallbacks {
  data class UiState(
    @StringRes val startServerButtonTextRes: Int = string.start_server_label,
    val startServerButtonColor: Color = StartServerGreen,
    val serverIpDisplayText: String = "",
    val serverIpAddress: String = "",
    val showShareIcon: Boolean = false,
    val qrVisible: Boolean = false,
    val qrIcon: IconItem = IconItem.Drawable(R.drawable.ic_storage),
    val books: List<BooksOnDiskListItem> = emptyList()
  )

  sealed class Event {
    object StartIpCheck : Event()
    data class StartServer(val paths: ArrayList<String>, val restart: Boolean) : Event()
    object StopServer : Event()
    object ShowWifiDialog : Event()
    object ShowManualHotspotDialog : Event()
    object ShowNoBooksToast : Event()
    data class ShowErrorToast(val messageRes: Int) : Event()
    object DismissDialog : Event()
    object AskNotificationPermission : Event()
    object AskReadWritePermission : Event()
    object NotificationPermissionRationaleDialog : Event()
    object ReadPermissionRationaleDialog : Event()
    object AllFilesPermissionDialog : Event()
  }

  val isAndroid13OrAbove = kiwixPermissionChecker.isAndroid13orAbove()

  private val _uiState = MutableStateFlow(UiState())
  val uiState: StateFlow<UiState> = _uiState

  private val _events = MutableSharedFlow<Event>(extraBufferCapacity = Int.MAX_VALUE)
  val events = _events.asSharedFlow()

  fun loadBooks(isCustomApp: Boolean) {
    viewModelScope.launch(ioDispatcher) {
      val previouslyHostedBookIds = kiwixDataStore.hostedBookIds.first()
      val books = dataSource.getLanguageCategorizedBooks().first()
      val zimFileReader = zimReaderContainer.zimFileReader
      val processedBooks = processBooks(books, previouslyHostedBookIds, isCustomApp, zimFileReader)
      _uiState.update { it.copy(books = processedBooks) }

      if (ServerUtils.isServerStarted) {
        onServerStarted(ServerUtils.serverAddress)
      } else {
        layoutStopped()
      }
    }
  }

  private fun processBooks(
    bookItems: List<BooksOnDiskListItem>,
    hostedIds: Set<String>,
    isCustomApp: Boolean,
    zimFileReader: ZimFileReader?
  ): List<BooksOnDiskListItem> {
    return if (isCustomApp && zimFileReader != null) {
      bookItems.mapNotNull { item ->
        if (item is BookOnDisk) {
          BookOnDisk(zimFileReader, isSelected = true)
        } else {
          null
        }
      }
    } else {
      bookItems.map { item ->
        if (item is BookOnDisk) {
          item.copy(isSelected = shouldSelectBook(item, hostedIds))
        } else {
          item
        }
      }
    }
  }

  private fun shouldSelectBook(
    book: BookOnDisk,
    previouslyHostedBookIds: Set<String>
  ): Boolean {
    return when {
      // Hosted books are now saved using the unique book ID.
      previouslyHostedBookIds.contains(book.book.id) -> true
      // Backward compatibility: for users who have not been migrated to the new logic yet,
      // fall back to checking only the title.
      previouslyHostedBookIds.contains(book.book.title) -> true
      // If no previously hosted books are saved, select all books by default.
      previouslyHostedBookIds.isEmpty() -> true
      else -> false
    }
  }

  fun startServerButtonClick() {
    viewModelScope.launch {
      if (checkStartServerPreconditions()) {
        if (ServerUtils.isServerStarted) {
          sendEvent(StopServer)
          return@launch
        }

        val paths = selectedBooksPath(uiState.value.books)
        if (paths.isEmpty()) {
          sendEvent(ShowNoBooksToast)
          return@launch
        }

        when {
          connectivityReporter.checkWifi() -> sendEvent(ShowWifiDialog)
          connectivityReporter.checkTethering() -> sendEvent(StartIpCheck)
          else -> sendEvent(ShowManualHotspotDialog)
        }
      }
    }
  }

  @Suppress("ReturnCount")
  private suspend fun checkStartServerPreconditions(): Boolean {
    if (!kiwixPermissionChecker.hasNotificationPermission()) {
      sendEvent(AskNotificationPermission)
      return false
    }
    if (!kiwixPermissionChecker.hasReadExternalStoragePermission()) {
      sendEvent(AskReadWritePermission)
      return false
    }
    if (!kiwixPermissionChecker.isManageExternalStoragePermissionGranted()) {
      sendEvent(AllFilesPermissionDialog)
      return false
    }
    return true
  }

  fun onBookSelected(book: BookOnDisk) {
    val updatedBooks = uiState.value.books.map { item ->
      if (item is BookOnDisk && item == book) {
        item.copy(isSelected = !item.isSelected)
      } else {
        item
      }
    }
    _uiState.update { it.copy(books = updatedBooks) }

    viewModelScope.launch {
      saveHostedBooks(updatedBooks)
      if (ServerUtils.isServerStarted) {
        sendEvent(StartServer(selectedBooksPath(uiState.value.books), true))
      }
    }
  }

  private fun selectedBooksPath(books: List<BooksOnDiskListItem>): ArrayList<String> =
    books
      .filterIsInstance<BooksOnDiskListItem.BookOnDisk>()
      .filter { it.isSelected }
      .map { it.zimReaderSource.toDatabase() }
      .onEach { Log.v("Hosted Book", "ZIM PATH : $it") }
      .toCollection(ArrayList())

  private suspend fun saveHostedBooks(booksList: List<BooksOnDiskListItem>) {
    val hostedBooks = booksList.asSequence()
      .filterIsInstance<BookOnDisk>()
      .filter(BookOnDisk::isSelected)
      .map { it.book.id }
      .toSet()
    kiwixDataStore.setHostedBookIds(hostedBooks)
  }

  fun onWifiConfirmed() {
    sendEvent(StartIpCheck)
  }

  fun showNotificationPermissionRationaleDialog() {
    sendEvent(NotificationPermissionRationaleDialog)
  }

  fun showReadPermissionRationalDialog() {
    sendEvent(ReadPermissionRationaleDialog)
  }

  override fun onIpAddressValid() {
    sendEvent(StartServer(selectedBooksPath(uiState.value.books), false))
  }

  override fun onServerStarted(ip: String) {
    ServerUtils.serverAddress = ip
    _uiState.update {
      it.copy(
        serverIpAddress = ip,
        showShareIcon = ip.isNotBlank(),
        qrVisible = true,
        qrIcon = getQrIcon(ip),
        serverIpDisplayText = context.getString(string.server_started_message, ip),
        startServerButtonColor = StopServerRed
      )
    }
    sendEvent(DismissDialog)
  }

  private fun layoutStopped() {
    _uiState.update {
      it.copy(
        serverIpAddress = "",
        showShareIcon = false,
        qrVisible = false,
        qrIcon = getQrIcon(null),
        serverIpDisplayText = context.getString(string.server_textview_default_message),
        startServerButtonColor = StartServerGreen
      )
    }
  }

  private fun getQrIcon(ip: String?) =
    if (ip.isNullOrBlank()) {
      IconItem.Drawable(R.drawable.ic_storage)
    } else {
      val qr = generateQr.createQR(ip)
      IconItem.ImageBitmap(qr.asImageBitmap())
    }

  override fun onServerStopped() {
    layoutStopped()
  }

  override fun onServerFailedToStart(errorMessage: Int?) {
    sendEvent(DismissDialog)
    errorMessage?.let { sendEvent(ShowErrorToast(it)) }
  }

  override fun onIpAddressInvalid() {
    sendEvent(DismissDialog)
  }

  private fun sendEvent(event: Event) {
    viewModelScope.launch { _events.emit(event) }
  }
}
