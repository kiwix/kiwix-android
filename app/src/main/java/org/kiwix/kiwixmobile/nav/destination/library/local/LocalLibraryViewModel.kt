/*
 * Kiwix Android
 * Copyright (c)2019–2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.nav.destination.library.local

import android.app.Application
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.MULTI
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.FileSelectActions.MultiModeFinished
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.FileSelectActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.FileSelectActions.RequestNavigateTo
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.FileSelectActions.RequestShareMultiSelection
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.FileSelectActions.RequestValidateZimFiles
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.FileSelectActions.RestartActionMode
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.FileSelectActions.UserClickedDownloadBooksButton
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.DeleteFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.NavigateToDownloads
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.None
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.OpenFileWithNavigation
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ShareFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.StartMultiSelection
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ValidateZIMFiles
import org.kiwix.libkiwix.Book
import java.io.File
import javax.inject.Inject

const val DEFAULT_PROGRESS = 0
const val MAX_PROGRESS = 100

/**
 * ViewModel for the Local Library screen.
 *
 * This ViewModel manages local ZIM file operations including:
 * - File system scanning for ZIM files
 * - File selection and multi-selection
 * - Side effects for file operations (delete, share, validate, navigate)
 */
class LocalLibraryViewModel @Inject constructor(
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk,
  private val storageObserver: StorageObserver,
  private val dataSource: DataSource,
  private val context: Application,
  private val processSelectedZimFilesForStandalone: ProcessSelectedZimFilesForStandalone,
  private val processSelectedZimFilesForPlayStore: ProcessSelectedZimFilesForPlayStore,
  private val repositoryActions: MainRepositoryActions,
  private val kiwixPermissionChecker: KiwixPermissionChecker,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel(), SelectedZimFileCallback {
  /**
   * Sealed class representing all file selection actions.
   */
  sealed class FileSelectActions {
    data class RequestNavigateTo(val bookOnDisk: BookOnDisk) : FileSelectActions()
    data class RequestSelect(val bookOnDisk: BookOnDisk) : FileSelectActions()
    data class RequestMultiSelection(val bookOnDisk: BookOnDisk) : FileSelectActions()
    data object RequestValidateZimFiles : FileSelectActions()
    data object RequestDeleteMultiSelection : FileSelectActions()
    data object RequestShareMultiSelection : FileSelectActions()
    data object MultiModeFinished : FileSelectActions()
    data object RestartActionMode : FileSelectActions()
    data object UserClickedDownloadBooksButton : FileSelectActions()
  }

  data class LocalLibraryScreenState(
    val fileSelectListState: FileSelectListState = FileSelectListState(emptyList()),
    val isSwipeRefreshing: Boolean = false,
    val scanning: ScanningState = ScanningState(false, ZERO),
    val noFileView: NoFileView = NoFileView("", "", false),
    val permissionDeniedLayoutShowing: Boolean = false,
    val actionMenuItems: List<ActionMenuItem> = emptyList()
  )

  data class ScanningState(val isScanning: Boolean, val progress: Int)
  data class NoFileView(val title: String, val buttonText: String, val isVisible: Boolean)

  private lateinit var validateZimViewModel: ValidateZimViewModel
  private lateinit var alertDialogShower: AlertDialogShower
  private val _uiState = MutableStateFlow(LocalLibraryScreenState())
  val uiState = _uiState.asStateFlow()

  val sideEffects: MutableSharedFlow<SideEffect<*>> = MutableSharedFlow()
  val requestFileSystemCheck = MutableSharedFlow<Unit>(replay = 0)
  val fileSelectActions = MutableSharedFlow<FileSelectActions>(extraBufferCapacity = 1)

  private val coroutineJobs: MutableList<Job> = mutableListOf()
  private val selectedZimFileUriList: MutableList<Uri> = mutableListOf()

  fun initialize(
    validateZimViewModel: ValidateZimViewModel,
    alertDialogShower: AlertDialogShower,
    snackBarHostState: SnackbarHostState,
    fragmentManager: FragmentManager,
  ) {
    this.validateZimViewModel = validateZimViewModel
    this.alertDialogShower = alertDialogShower
    processSelectedZimFilesForStandalone.setSelectedZimFileCallback(this)
    processSelectedZimFilesForPlayStore.init(
      lifecycleScope = viewModelScope,
      alertDialogShower = alertDialogShower,
      snackBarHostState = snackBarHostState,
      fragmentManager = fragmentManager,
      selectedZimFileCallback = this
    )
    observeCoroutineFlows()
  }

  private fun observeCoroutineFlows() {
    coroutineJobs.apply {
      add(scanBooksFromStorage())
      add(updateBookItems())
      add(processFileSelectActions())
    }
  }

  override fun onCleared() {
    coroutineJobs.forEach {
      it.cancel()
    }
    coroutineJobs.clear()
    super.onCleared()
  }

  private fun scanBooksFromStorage() =
    checkFileSystemForBooksOnRequest(books())
      .catch { it.printStackTrace() }
      .onEach { books -> libkiwixBookOnDisk.insert(books) }
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  private fun processFileSelectActions() =
    fileSelectActions
      .onEach { action ->
        runCatching {
          sideEffects.emit(
            when (action) {
              is RequestNavigateTo ->
                OpenFileWithNavigation(action.bookOnDisk)

              is RequestMultiSelection ->
                startMultiSelectionAndSelectBook(action.bookOnDisk)

              RequestDeleteMultiSelection ->
                DeleteFiles(selectionsFromState(), alertDialogShower)

              RequestShareMultiSelection ->
                ShareFiles(selectionsFromState())

              RequestValidateZimFiles ->
                ValidateZIMFiles(selectionsFromState(), alertDialogShower, validateZimViewModel)

              MultiModeFinished ->
                noSideEffectAndClearSelectionState()

              is RequestSelect ->
                noSideEffectSelectBook(action.bookOnDisk)

              RestartActionMode ->
                StartMultiSelection(fileSelectActions)

              UserClickedDownloadBooksButton ->
                NavigateToDownloads
            }
          )
        }.onFailure {
          it.printStackTrace()
        }
      }.launchIn(viewModelScope)

  private fun startMultiSelectionAndSelectBook(
    bookOnDisk: BookOnDisk
  ): StartMultiSelection {
    updateState { current ->
      val updatedList = selectBook(current.fileSelectListState, bookOnDisk)
      current.copy(
        current.fileSelectListState.copy(
          bookOnDiskListItems = updatedList,
          selectionMode = MULTI
        )
      )
    }
    return StartMultiSelection(fileSelectActions)
  }

  private fun selectBook(
    it: FileSelectListState,
    bookOnDisk: BookOnDisk
  ): List<BooksOnDiskListItem> {
    return it.bookOnDiskListItems.map { listItem ->
      if (listItem.id == bookOnDisk.id) {
        listItem.apply { isSelected = !isSelected }
      } else {
        listItem
      }
    }
  }

  private fun noSideEffectSelectBook(bookOnDisk: BookOnDisk): SideEffect<Unit> {
    updateState {
      it.copy(
        fileSelectListState = it.fileSelectListState.copy(
          bookOnDiskListItems = selectBook(it.fileSelectListState, bookOnDisk)
        )
      )
    }
    return None
  }

  private fun selectionsFromState() = uiState.value.fileSelectListState.selectedBooks

  private fun noSideEffectAndClearSelectionState(): SideEffect<Unit> {
    updateState {
      it.copy(
        fileSelectListState = it.fileSelectListState.copy(
          bookOnDiskListItems =
            it.fileSelectListState.bookOnDiskListItems.map { bookOnDisk ->
              bookOnDisk.apply { isSelected = false }
            },
          selectionMode = NORMAL
        )
      )
    }
    return None
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun checkFileSystemForBooksOnRequest(
    booksFromDao: Flow<List<Book>>
  ): Flow<List<Book>> = requestFileSystemCheck
    .flatMapLatest {
      // Initial progress
      updateState {
        it.copy(
          scanning = it.scanning.copy(isScanning = true, progress = DEFAULT_PROGRESS),
          isSwipeRefreshing = false
        )
      }
      booksFromStorageNotIn(
        booksFromDao,
        object : ScanningProgressListener {
          override fun onProgressUpdate(scannedDirectory: Int, totalDirectory: Int) {
            val overallProgress =
              (scannedDirectory.toDouble() / totalDirectory.toDouble() * MAX_PROGRESS).toInt()
            if (overallProgress != MAX_PROGRESS) {
              updateState {
                it.copy(scanning = ScanningState(isScanning = true, progress = overallProgress))
              }
            }
          }
        }
      )
    }
    .onEach {
      updateState {
        it.copy(
          scanning = ScanningState(isScanning = false, progress = MAX_PROGRESS),
          isSwipeRefreshing = false
        )
      }
    }
    .filter { it.isNotEmpty() }
    .map { books -> books.distinctBy { it.id } }

  private fun books(): Flow<List<Book>> =
    libkiwixBookOnDisk.books().map { bookOnDiskList ->
      bookOnDiskList
        .sortedBy { it.book.title }
        .mapNotNull { it.book.nativeBook }
    }

  private fun booksFromStorageNotIn(
    localBooksFromLibkiwix: Flow<List<Book>>,
    scanningProgressListener: ScanningProgressListener
  ): Flow<List<Book>> = flow {
    val scannedBooks = storageObserver.getBooksOnFileSystem(scanningProgressListener).first()
    val daoBookIds = localBooksFromLibkiwix.first().map { it.id }
    emit(removeBooksAlreadyInDao(scannedBooks, daoBookIds))
  }

  private fun removeBooksAlreadyInDao(
    booksFromFileSystem: Collection<Book>,
    idsInDao: List<String>
  ) = booksFromFileSystem.filterNot { idsInDao.contains(it.id) }

  private fun updateBookItems() =
    dataSource.booksOnDiskAsListItems()
      .catch { it.printStackTrace() }
      .onEach { newList ->
        updateState { current ->
          val updatedListState = current.fileSelectListState.let {
            if (it.bookOnDiskListItems.isEmpty()) {
              FileSelectListState(newList)
            } else {
              inheritSelections(it, newList.toMutableList())
            }
          }
          current.copy(
            fileSelectListState = updatedListState,
            noFileView = current.noFileView.copy(
              isVisible = updatedListState.bookOnDiskListItems.isEmpty() || current.permissionDeniedLayoutShowing,
              title = if (current.permissionDeniedLayoutShowing) {
                context.getString(string.grant_read_storage_permission)
              } else {
                context.getString(string.no_files_here)
              },
              buttonText = if (current.permissionDeniedLayoutShowing) {
                context.getString(string.go_to_settings_label)
              } else {
                context.getString(string.download_books)
              }
            )
          )
        }
      }.launchIn(viewModelScope)

  private fun inheritSelections(
    oldState: FileSelectListState,
    newList: MutableList<BooksOnDiskListItem>
  ): FileSelectListState {
    return oldState.copy(
      bookOnDiskListItems =
        newList.map { newBookOnDisk ->
          val firstOrNull =
            oldState.bookOnDiskListItems.firstOrNull { oldBookOnDisk ->
              oldBookOnDisk.id == newBookOnDisk.id
            }
          newBookOnDisk.apply { isSelected = firstOrNull?.isSelected == true }
        }
    )
  }

  private fun updateState(transform: (LocalLibraryScreenState) -> LocalLibraryScreenState) {
    _uiState.value = transform(_uiState.value)
  }

  fun processZimFileUri(zimFileUri: String) {
    viewModelScope.launch {
      if (zimFileUri.isNotEmpty()) {
        selectedZimFileUriList.clear()
        selectedZimFileUriList.add(zimFileUri.toUri())
        if (kiwixPermissionChecker.isManageExternalStoragePermissionGranted()) {
          // show dialog of manageExternalStoragePermission
        } else if (!kiwixPermissionChecker.hasWriteExternalStoragePermission()) {
          // ask for write permission.
        } else {
          handleSelectedFileUri(selectedZimFileUriList)
        }
      }
    }
  }

  private suspend fun handleSelectedFileUri(uris: List<Uri>) {
    selectedZimFileUriList.clear()
    selectedZimFileUriList.addAll(uris)
    when {
      // Process the ZIM file for standalone app.
      processSelectedZimFilesForStandalone.canHandleUris() ->
        processSelectedZimFilesForStandalone.processSelectedFiles(uris)

      // Process the ZIM file for PlayStore app.
      processSelectedZimFilesForPlayStore.canHandleUris() ->
        processSelectedZimFilesForPlayStore.processSelectedFiles(uris)
    }
  }

  override fun navigateToReaderFragment(file: File) {
  }

  override fun addBookToLibkiwixBookOnDisk(file: File) {
  }

  override fun showFileCopyMoveErrorDialog(
    errorMessage: String,
    callBack: suspend () -> Unit
  ) {
  }
}
