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

import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.qr.GenerateQR
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.ConnectivityReporter
import org.kiwix.kiwixmobile.core.utils.ServerUtils
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import javax.inject.Inject

class ZimHostViewModel @Inject constructor(
  private val dataSource: DataSource,
  private val kiwixDataStore: KiwixDataStore,
  private val generateQr: GenerateQR,
  private val zimReaderContainer: ZimReaderContainer,
  private val connectivityReporter: ConnectivityReporter
) : ViewModel(), ZimHostCallbacks {
  data class UiState(
    val ipAddress: String = "",
    val isServerRunning: Boolean = false,
    val shareVisible: Boolean = false,
    val qrVisible: Boolean = false,
    val qrIcon: IconItem = IconItem.Drawable(R.drawable.ic_storage),
    val books: List<BooksOnDiskListItem> = emptyList(),
    val isPlayStoreBuild: Boolean = false,
    // Tracks Checkboxes
    val selectedBookIds: Set<String> = emptySet()
  )

  private val _uiState = MutableStateFlow(UiState())
  val uiState: StateFlow<UiState> = _uiState

  private val _events = Channel<Event>(Channel.BUFFERED)
  val events = _events.receiveAsFlow()

  sealed class Event {
    object StartIpCheck : Event()
    data class StartServer(val paths: ArrayList<String>, val restart: Boolean) : Event()
    object StopServer : Event()
    object ShowWifiDialog : Event()
    object ShowManualHotspotDialog : Event()
    object ShowNoBooksToast : Event()
    data class ShowErrorToast(val messageRes: Int) : Event()
    object DismissDialog : Event()
  }

  fun onResumeLoad(isCustomApp: Boolean) {
    viewModelScope.launch {
      val hosted = kiwixDataStore.hostedBookIds.first()
      val isPlayStore = kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()
      val rawBooks = dataSource.getLanguageCategorizedBooks().first()

      // Resolve Books
      val processedBooks = if (isCustomApp) {
        rawBooks.mapNotNull { item ->
          if (item is BooksOnDiskListItem.BookOnDisk) {
            zimReaderContainer.zimFileReader?.let { BooksOnDiskListItem.BookOnDisk(it) }
          } else {
            item
          }
        }
      } else {
        rawBooks
      }

      // Initially Select All
      val allBookIds = processedBooks
        .filterIsInstance<BooksOnDiskListItem.BookOnDisk>()
        .map { it.book.id }
        .toSet()

      val initialSelection = when {
        isCustomApp || hosted.isEmpty() -> allBookIds
        else -> hosted
      }

      _uiState.update {
        it.copy(
          books = processedBooks,
          selectedBookIds = initialSelection,
          isPlayStoreBuild = isPlayStore
        )
      }

      if (!isCustomApp && hosted.isEmpty() && initialSelection.isNotEmpty()) {
        kiwixDataStore.setHostedBookIds(initialSelection)
      }

      if (ServerUtils.isServerStarted) onServerStarted(ServerUtils.serverAddress) else layoutStopped()
    }
  }

  fun onButtonClicked() {
    viewModelScope.launch {
      if (ServerUtils.isServerStarted) {
        _events.send(Event.StopServer)
        return@launch
      }

      val paths = selectedPaths(_uiState.value.books, _uiState.value.selectedBookIds)
      if (paths.isEmpty()) {
        _events.send(Event.ShowNoBooksToast)
        return@launch
      }

      when {
        connectivityReporter.checkWifi() -> _events.send(Event.ShowWifiDialog)
        connectivityReporter.checkTethering() -> _events.send(Event.StartIpCheck)
        else -> _events.send(Event.ShowManualHotspotDialog)
      }
    }
  }

  fun toggleSelection(book: BooksOnDiskListItem.BookOnDisk) {
    val newSelection = _uiState.value.selectedBookIds.toMutableSet().apply {
      if (!remove(book.id)) add(book.id)
    }

    _uiState.update { it.copy(selectedBookIds = newSelection) }

    viewModelScope.launch {
      kiwixDataStore.setHostedBookIds(newSelection)

      if (ServerUtils.isServerStarted) {
        _events.send(Event.StartServer(selectedPaths(_uiState.value.books, newSelection), true))
      }
    }
  }

  private fun selectedPaths(
    books: List<BooksOnDiskListItem>,
    selectedIds: Set<String>
  ): ArrayList<String> =
    books
      .filterIsInstance<BooksOnDiskListItem.BookOnDisk>()
      .filter { selectedIds.contains(it.book.id) }
      .map { it.zimReaderSource.toDatabase() }
      .toCollection(ArrayList())

  fun onWifiConfirmed() {
    viewModelScope.launch { _events.send(Event.StartIpCheck) }
  }

  override fun onIpAddressValid() {
    viewModelScope.launch {
      _events.send(
        Event.StartServer(
          selectedPaths(
            _uiState.value.books,
            _uiState.value.selectedBookIds
          ),
          false
        )
      )
    }
  }

  override fun onServerStarted(ip: String) {
    ServerUtils.serverAddress = ip

    val generatedQrIcon = if (ip.isNotBlank()) {
      val qr = generateQr.createQR(ip)
      IconItem.ImageBitmap(qr.asImageBitmap())
    } else {
      IconItem.Drawable(R.drawable.ic_storage)
    }

    _uiState.update {
      it.copy(
        ipAddress = ip,
        isServerRunning = true,
        shareVisible = ip.isNotBlank(),
        qrVisible = true,
        qrIcon = generatedQrIcon
      )
    }

    viewModelScope.launch { _events.send(Event.DismissDialog) }
  }

  private fun layoutStopped() {
    _uiState.update {
      it.copy(
        ipAddress = "",
        isServerRunning = false,
        shareVisible = false,
        qrVisible = false,
        qrIcon = IconItem.Drawable(R.drawable.ic_storage)
      )
    }
  }

  override fun onServerStopped() {
    layoutStopped()
  }

  override fun onServerFailedToStart(errorMessage: Int?) {
    viewModelScope.launch {
      _events.send(Event.DismissDialog)
      errorMessage?.let { _events.send(Event.ShowErrorToast(it)) }
    }
  }

  override fun onIpAddressInvalid() {
    viewModelScope.launch { _events.send(Event.DismissDialog) }
  }
}
