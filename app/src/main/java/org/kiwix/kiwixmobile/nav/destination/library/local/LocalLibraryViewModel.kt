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
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.annotation.VisibleForTesting
import androidx.compose.material3.SnackbarHostState
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.BuildConfig
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.extensions.canReadFile
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.extensions.navigateToAppSettings
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.ui.models.ActionMenuItem
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.effects.ManageExternalFilesPermissionDialog
import org.kiwix.kiwixmobile.core.utils.effects.ReadPermissionRequiredDialog
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.MULTI
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.CopyMoveErrorDialog
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.FileSystemScanDialog
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.ManageFilesPermissionDialog
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.MultiModeFinished
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.ReadPermissionDialog
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.RequestMultiSelection
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.RequestNavigateTo
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.RequestReadWritePermission
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.RequestSelect
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.RequestShareMultiSelection
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.RequestValidateZimFiles
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.LocalLibraryUiActions.UserClickedDownloadBooksButton
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.ReadeWritePermissionResultAction.OpenFilePicker
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.ReadeWritePermissionResultAction.ProcessZimFiles
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.ReadeWritePermissionResultAction.ScanStorage
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.DeleteFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.NavigateToDownloads
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.None
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.OpenFileWithNavigation
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ShareFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ValidateZIMFiles
import org.kiwix.libkiwix.Book
import java.io.File
import javax.inject.Inject

private const val DEFAULT_PROGRESS = 0
private const val MAX_PROGRESS = 100
private const val SHOW_SCAN_DIALOG_DELAY = 2000L

/**
 * ViewModel for the Local Library screen.
 *
 * This ViewModel manages local ZIM file operations including:
 * - File system scanning for ZIM files
 * - File selection and multi-selection
 * - Side effects for file operations (delete, share, validate, navigate)
 */
@Suppress("LongParameterList")
class LocalLibraryViewModel @Inject constructor(
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk,
  private val storageObserver: StorageObserver,
  private val dataSource: DataSource,
  private val context: Application,
  private val processSelectedZimFilesForStandalone: ProcessSelectedZimFilesForStandalone,
  private val processSelectedZimFilesForPlayStore: ProcessSelectedZimFilesForPlayStore,
  private val repositoryActions: MainRepositoryActions,
  private val kiwixPermissionChecker: KiwixPermissionChecker,
  val kiwixDataStore: KiwixDataStore,
  private val zimReaderFactory: ZimFileReader.Factory,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel(), SelectedZimFileCallback {
  /**
   * Sealed class representing all file selection actions.
   */
  sealed class LocalLibraryUiActions {
    data class RequestNavigateTo(val zimReaderSource: ZimReaderSource) : LocalLibraryUiActions()
    data class RequestSelect(val bookOnDisk: BookOnDisk) : LocalLibraryUiActions()
    data class RequestMultiSelection(val bookOnDisk: BookOnDisk) : LocalLibraryUiActions()
    data object RequestValidateZimFiles : LocalLibraryUiActions()
    data object RequestDeleteMultiSelection : LocalLibraryUiActions()
    data object RequestShareMultiSelection : LocalLibraryUiActions()
    data object MultiModeFinished : LocalLibraryUiActions()
    data object UserClickedDownloadBooksButton : LocalLibraryUiActions()
    data object ManageFilesPermissionDialog : LocalLibraryUiActions()
    data object FileSystemScanDialog : LocalLibraryUiActions()
    data class CopyMoveErrorDialog(val errorMessage: String, val callBack: suspend () -> Unit) :
      LocalLibraryUiActions()

    data class RequestReadWritePermission(val resultAction: ReadeWritePermissionResultAction) :
      LocalLibraryUiActions()

    data object ReadPermissionDialog : LocalLibraryUiActions()
    data object RequestDrawerToggle : LocalLibraryUiActions()
  }

  sealed class ReadeWritePermissionResultAction {
    data object ScanStorage : ReadeWritePermissionResultAction()
    data class ProcessZimFiles(val uris: List<Uri>) : ReadeWritePermissionResultAction()
    data object None : ReadeWritePermissionResultAction()
    data class OpenFilePicker(val filePickerLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>) :
      ReadeWritePermissionResultAction()
  }

  data class LocalLibraryUiState(
    val fileSelectListState: FileSelectListState = FileSelectListState(emptyList()),
    val isSwipeRefreshing: Boolean = false,
    val scanning: ScanningState = ScanningState(false, ZERO),
    val noFileView: NoFileView = NoFileView("", "", false),
    val permissionDeniedLayoutShowing: Boolean = false,
    val actionMenuItems: List<ActionMenuItem> = emptyList()
  )

  data class ScanningState(val isScanning: Boolean, val progress: Int)
  data class NoFileView(val title: String, val buttonText: String, val isVisible: Boolean)

  private var shouldScanFileSystem = false
  private lateinit var validateZimViewModel: ValidateZimViewModel
  private lateinit var alertDialogShower: AlertDialogShower
  private val _uiState = MutableStateFlow(LocalLibraryUiState())
  val uiState = _uiState.asStateFlow()

  private val coroutineJobs: MutableList<Job> = mutableListOf()
  private var onResumeJob: Job? = null

  val sideEffects = MutableSharedFlow<SideEffect<*>>()
  val localLibraryUiActions = MutableSharedFlow<LocalLibraryUiActions>()

  @VisibleForTesting
  internal val requestFileSystemCheck = MutableSharedFlow<Unit>()

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

  fun onResume() {
    onResumeJob?.cancel()
    onResumeJob = viewModelScope.launch {
      when {
        shouldShowFileSystemDialog() -> {
          if (!kiwixDataStore.prefIsTest.first()) {
            delay(SHOW_SCAN_DIALOG_DELAY)
          }
          if (!isActive) return@launch
          sendAction(FileSystemScanDialog)
        }

        shouldScanFileSystem -> {
          // When user goes to settings for granting the `MANAGE_EXTERNAL_STORAGE` permission, and
          // came back to the application then initiate the scanning of file system.
          scanFileSystem()
        }

        !kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() &&
          !kiwixDataStore.prefIsTest.first() &&
          !uiState.value.permissionDeniedLayoutShowing -> {
          checkPermissions()
        }

        else -> {
          updateState {
            it.copy(
              noFileView = NoFileView(
                context.getString(string.no_files_here),
                context.getString(string.download_books),
                false
              )
            )
          }
        }
      }
    }
  }

  private suspend fun checkPermissions() {
    if (!kiwixPermissionChecker.hasReadExternalStoragePermission()) {
      sendAction(RequestReadWritePermission(ReadeWritePermissionResultAction.None))
    } else if (!kiwixPermissionChecker.isManageExternalStoragePermissionGranted() &&
      kiwixDataStore.showManageExternalFilesPermissionDialog.first()
    ) {
      // We should only ask for first time, If the users wants to revoke settings
      // then they can directly toggle this feature from settings screen
      kiwixDataStore.setShowManageExternalFilesPermissionDialog(false)
      // Show Dialog and  Go to settings to give permission
      sendAction(ManageFilesPermissionDialog)
    }
  }

  private fun observeCoroutineFlows() {
    clearObservers()
    coroutineJobs.apply {
      add(scanBooksFromStorage())
      add(updateBookItems())
      add(processFileSelectActions())
    }
  }

  private fun clearObservers() {
    coroutineJobs.forEach {
      it.cancel()
    }
    coroutineJobs.clear()
  }

  override fun onCleared() {
    clearObservers()
    processSelectedZimFilesForPlayStore.dispose()
    processSelectedZimFilesForStandalone.dispose()
    super.onCleared()
  }

  @VisibleForTesting
  fun onClearedExposed() {
    onCleared()
  }

  private fun scanBooksFromStorage() =
    checkFileSystemForBooksOnRequest(books())
      .catch { it.printStackTrace() }
      .onEach { books -> libkiwixBookOnDisk.insert(books) }
      .flowOn(ioDispatcher)
      .launchIn(viewModelScope)

  private fun processFileSelectActions() =
    localLibraryUiActions
      .onEach { action ->
        runCatching {
          sideEffects.emit(handleAction(action))
        }.onFailure {
          it.printStackTrace()
        }
      }.launchIn(viewModelScope)

  @Suppress("CyclomaticComplexMethod")
  private fun handleAction(action: LocalLibraryUiActions): SideEffect<*> =
    when (action) {
      is RequestMultiSelection -> noSideEffectSelectBook(action.bookOnDisk)
      is RequestSelect -> noSideEffectSelectBook(action.bookOnDisk)
      RequestDeleteMultiSelection -> DeleteFiles(selectionsFromState(), alertDialogShower)
      RequestShareMultiSelection -> ShareFiles(selectionsFromState(), viewModelScope, ioDispatcher)
      MultiModeFinished -> noSideEffectAndClearSelectionState()
      UserClickedDownloadBooksButton -> NavigateToDownloads
      is RequestReadWritePermission -> None // We handle this on UI.
      ReadPermissionDialog -> ReadPermissionRequiredDialog(alertDialogShower)
      RequestValidateZimFiles ->
        ValidateZIMFiles(selectionsFromState(), alertDialogShower, validateZimViewModel)

      ManageFilesPermissionDialog ->
        if (kiwixPermissionChecker.isAndroid13orAbove()) {
          ManageExternalFilesPermissionDialog(alertDialogShower)
        } else {
          None
        }

      is CopyMoveErrorDialog -> ShowFileCopyMoveErrorDialog(
        alertDialogShower,
        action.errorMessage,
        viewModelScope,
        action.callBack
      )

      FileSystemScanDialog -> ShowFileSystemScanDialog(
        alertDialogShower,
        viewModelScope,
        kiwixDataStore
      ) {
        shouldScanFileSystem = true
        scanFileSystem()
      }

      is RequestNavigateTo ->
        OpenFileWithNavigation(
          zimReaderSource = action.zimReaderSource,
          coroutineScope = viewModelScope,
          ioDispatcher = ioDispatcher
        )

      RequestDrawerToggle -> sideEffectDrawerToggle()
    }

  private fun sideEffectDrawerToggle(): SideEffect<Unit> = SideEffect { activity ->
    val coreMainActivity = activity as? CoreMainActivity
    if (coreMainActivity?.navigationDrawerIsOpen() == true) {
      coreMainActivity.closeNavigationDrawer()
    } else {
      coreMainActivity?.openNavigationDrawer()
    }
  }

  private fun selectBook(
    it: FileSelectListState,
    bookOnDisk: BookOnDisk
  ): List<BooksOnDiskListItem> {
    return it.bookOnDiskListItems.map { listItem ->
      if (listItem is BookOnDisk && listItem.id == bookOnDisk.id) {
        listItem.copy(isSelected = !listItem.isSelected)
      } else {
        listItem
      }
    }
  }

  private fun noSideEffectSelectBook(bookOnDisk: BookOnDisk): SideEffect<Unit> {
    updateState {
      val updatedList = selectBook(it.fileSelectListState, bookOnDisk)
      it.copy(
        fileSelectListState = it.fileSelectListState.copy(
          bookOnDiskListItems = updatedList,
          selectionMode =
            if (updatedList.filterIsInstance<BookOnDisk>().none(BookOnDisk::isSelected)) {
              NORMAL
            } else {
              MULTI
            }
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
              if (bookOnDisk is BookOnDisk) {
                bookOnDisk.copy(isSelected = false)
              } else {
                bookOnDisk
              }
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
      updateState { current ->
        current.copy(
          scanning = current.scanning.copy(isScanning = true, progress = DEFAULT_PROGRESS),
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
              updateState { current ->
                current.copy(
                  scanning = ScanningState(isScanning = true, progress = overallProgress)
                )
              }
            }
          }
        }
      )
    }
    .onEach {
      updateState { current ->
        current.copy(
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
            oldState.bookOnDiskListItems.filterIsInstance<BookOnDisk>()
              .firstOrNull { oldBookOnDisk ->
                oldBookOnDisk.id == newBookOnDisk.id
              }
          if (newBookOnDisk is BookOnDisk) {
            newBookOnDisk.copy(isSelected = firstOrNull?.isSelected == true)
          } else {
            newBookOnDisk
          }
        }
    )
  }

  private fun updateState(transform: (LocalLibraryUiState) -> LocalLibraryUiState) {
    _uiState.value = transform(_uiState.value)
  }

  /**
   * Scan the file system for ZIM files.
   * Checks:
   * 1. If our app has the storage permission. If not, it asks for the permission(if not running in test).
   * 2. Checks if app has the full scan permission. If not, then it asks for the permission.
   * 3. Then finally it scans the storage for ZIM files.
   */
  private suspend fun scanFileSystem() {
    when {
      !kiwixPermissionChecker.hasWriteExternalStoragePermission() && !kiwixDataStore.isScanFileSystemTest.first() ->
        sendAction(RequestReadWritePermission(ScanStorage))

      !kiwixPermissionChecker.isManageExternalStoragePermissionGranted() -> {
        // When user come back after giving the setting it will scan the storage.
        shouldScanFileSystem = true
        sendAction(ManageFilesPermissionDialog)
      }

      else -> {
        shouldScanFileSystem = false
        requestFileSystemCheck()
      }
    }
  }

  private suspend fun requestFileSystemCheck() {
    requestFileSystemCheck.emit(Unit)
  }

  fun onReadWritePermissionGranted(resultAction: ReadeWritePermissionResultAction) {
    updateState { it.copy(permissionDeniedLayoutShowing = false) }
    viewModelScope.launch {
      when (resultAction) {
        is OpenFilePicker -> showFileChooser(resultAction.filePickerLauncher)
        is ProcessZimFiles -> handleSelectedFileUri(resultAction.uris)
        ScanStorage -> scanFileSystem()
        ReadeWritePermissionResultAction.None -> Unit
      }
    }
  }

  fun onReadWriteRationalPermission() {
    updateState {
      it.copy(
        permissionDeniedLayoutShowing = true,
        noFileView = it.noFileView.copy(
          isVisible = true,
          title = context.getString(string.grant_read_storage_permission),
          buttonText = context.getString(string.go_to_settings_label)
        )
      )
    }
    sendAction(ReadPermissionDialog)
  }

  fun onSwipeRefresh() {
    viewModelScope.launch {
      if (uiState.value.permissionDeniedLayoutShowing) {
        // When permission denied layout is showing hide the "Swipe refresh".
        updateState { it.copy(isSwipeRefreshing = false) }
      } else if (!kiwixPermissionChecker.isManageExternalStoragePermissionGranted()) {
        sendAction(ManageFilesPermissionDialog)
        // Set loading to false since the dialog is currently being displayed.
        // If the user clicks on "No" in the permission dialog,
        // the loading icon remains visible infinitely.
        updateState { it.copy(isSwipeRefreshing = false) }
      } else {
        // hide the swipe refreshing because now we are showing the ContentLoadingProgressBar
        // to show the progress of how many files are scanned.
        // disable the swipe refresh layout until the ongoing scanning will not complete
        // to avoid multiple scanning.
        updateState { it.copy(isSwipeRefreshing = false) }
        // Scan the storage for ZIM files.
        requestFileSystemCheck()
      }
    }
  }

  fun onDownloadButtonClick() {
    if (uiState.value.permissionDeniedLayoutShowing) {
      updateState { it.copy(permissionDeniedLayoutShowing = false) }
      context.navigateToAppSettings()
    } else {
      sendAction(UserClickedDownloadBooksButton)
    }
  }

  fun onBookItemClick(bookOnDisk: BookOnDisk) {
    viewModelScope.launch {
      if (!kiwixPermissionChecker.isManageExternalStoragePermissionGranted()) {
        sendAction(ManageFilesPermissionDialog)
      } else {
        sendAction(RequestNavigateTo(bookOnDisk.zimReaderSource))
      }
    }
  }

  fun onBookItemLongClick(bookOnDisk: BookOnDisk) {
    viewModelScope.launch {
      if (!kiwixPermissionChecker.isManageExternalStoragePermissionGranted()) {
        sendAction(ManageFilesPermissionDialog)
      } else {
        sendAction(RequestMultiSelection(bookOnDisk))
      }
    }
  }

  fun onMultiSelect(bookOnDisk: BookOnDisk) {
    sendAction(RequestSelect(bookOnDisk))
  }

  fun deleteMenuIconClick() {
    sendAction(RequestDeleteMultiSelection)
    sendAction(MultiModeFinished)
  }

  fun shareMenuIconClick() {
    sendAction(RequestShareMultiSelection)
    sendAction(MultiModeFinished)
  }

  fun validateMenuIconClick() {
    sendAction(RequestValidateZimFiles)
    sendAction(MultiModeFinished)
  }

  fun finishMultiModeFinished() {
    sendAction(MultiModeFinished)
  }

  fun onNavigationIconClick() {
    if (uiState.value.fileSelectListState.selectionMode == MULTI) {
      finishMultiModeFinished()
    } else {
      sendAction(RequestDrawerToggle)
    }
  }

  fun handleUserBackPressed(): FragmentActivityExtensions.Super {
    return if (uiState.value.fileSelectListState.selectionMode == MULTI) {
      finishMultiModeFinished()
      FragmentActivityExtensions.Super.ShouldNotCall
    } else {
      FragmentActivityExtensions.Super.ShouldCall
    }
  }

  fun filePickerMenuButtonClick(filePickerLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
    viewModelScope.launch {
      if (!kiwixPermissionChecker.hasWriteExternalStoragePermission()) {
        sendAction(RequestReadWritePermission(OpenFilePicker(filePickerLauncher)))
      } else if (!kiwixPermissionChecker.isManageExternalStoragePermissionGranted()) {
        sendAction(ManageFilesPermissionDialog)
      } else {
        showFileChooser(filePickerLauncher)
      }
    }
  }

  private suspend fun showFileChooser(filePickerLauncher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
    val intent = Intent().apply {
      action = Intent.ACTION_OPEN_DOCUMENT
      type = "application/*"
      addCategory(Intent.CATEGORY_OPENABLE)
      putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
      if (kiwixDataStore.prefIsTest.first()) {
        putExtra(
          "android.provider.extra.INITIAL_URI",
          "content://com.android.externalstorage.documents/document/primary:Download".toUri()
        )
      }
    }
    try {
      filePickerLauncher.launch(Intent.createChooser(intent, "Select a zim file"))
    } catch (_: ActivityNotFoundException) {
      context.toast(
        context.getString(R.string.no_app_found_to_open),
        Toast.LENGTH_SHORT
      )
    }
  }

  private fun sendAction(action: LocalLibraryUiActions) {
    viewModelScope.launch {
      localLibraryUiActions.emit(action)
    }
  }

  fun processZimFileArguments(zimFileUri: String) {
    viewModelScope.launch {
      if (zimFileUri.isNotEmpty()) {
        val selectedUris = listOf(zimFileUri.toUri())
        if (!kiwixPermissionChecker.hasWriteExternalStoragePermission()) {
          sendAction(RequestReadWritePermission(ProcessZimFiles(selectedUris)))
        } else if (!kiwixPermissionChecker.isManageExternalStoragePermissionGranted()) {
          sendAction(ManageFilesPermissionDialog)
        } else {
          handleSelectedFileUri(selectedUris)
        }
      }
    }
  }

  fun processSelectedZimFiles(intent: Intent?) {
    viewModelScope.launch {
      val uriList = extractUrisFromIntent(intent)
      if (uriList.isNotEmpty()) {
        handleSelectedFileUri(uriList)
      }
    }
  }

  private fun extractUrisFromIntent(intent: Intent?): List<Uri> {
    val urisList = arrayListOf<Uri>()
    when {
      intent?.clipData != null -> {
        // Handle multiple files.
        val count: Int = intent.clipData?.itemCount ?: ZERO
        for (i in ZERO..count - ONE) {
          intent.clipData?.getItemAt(i)?.uri?.let {
            takePersistableUriPermission(it)
            urisList.add(it)
          }
        }
      }

      intent?.data != null -> {
        // Handle single file.
        intent.data?.let {
          takePersistableUriPermission(it)
          urisList.add(it)
        }
      }
    }
    return urisList
  }

  private fun takePersistableUriPermission(uri: Uri) {
    runCatching {
      context.applicationContext?.contentResolver?.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or
          Intent.FLAG_GRANT_WRITE_URI_PERMISSION
      )
    }.onFailure {
      Log.e(TAG_KIWIX, "Could not take persistable permission for uri = $uri")
    }
  }

  private suspend fun handleSelectedFileUri(uris: List<Uri>) {
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
    viewModelScope.launch {
      if (!file.canReadFile()) {
        context.toast(string.unable_to_read_zim_file)
      } else {
        // Save the ZIM file to the libkiwix to display it on the local library screen.
        // This is particularly useful when storage is slow or contains a large number of files.
        // In such cases, scanning may take some time to show all the files on the
        // local library screen. Since our application is already aware of this opened ZIM file,
        // we can directly add it to the libkiwix.
        // See https://github.com/kiwix/kiwix-android/issues/3650
        addBookToLibkiwixBookOnDisk(file)
        // Open the ZIM file in reader screen.
        sendAction(RequestNavigateTo(ZimReaderSource(file)))
      }
    }
  }

  override fun addBookToLibkiwixBookOnDisk(file: File) {
    viewModelScope.launch(ioDispatcher) {
      runCatching {
        zimReaderFactory.create(ZimReaderSource(file), false)
          ?.let { zimFileReader ->
            val book = Book().apply { update(zimFileReader.jniKiwixReader) }
            repositoryActions.saveBook(book)
            zimFileReader.dispose()
          }
      }.onFailure {
        Log.e("LocalLibraryViewModel", "Failed to save book. Original Exception = ", it)
      }
    }
  }

  override fun showFileCopyMoveErrorDialog(
    errorMessage: String,
    callBack: suspend () -> Unit
  ) {
    sendAction(CopyMoveErrorDialog(errorMessage, callBack))
  }

  /**
   * Determines whether the file system scan dialog should be shown.
   * Conditions:
   *  1. The scan dialog has not already been shown.
   *  2. This is not the Play Store build.
   *  3. There are no ZIM files showing on the library screen.
   */
  private suspend fun shouldShowFileSystemDialog(): Boolean =
    !kiwixDataStore.isScanFileSystemDialogShown.first() &&
      !BuildConfig.IS_PLAYSTORE &&
      uiState.value.fileSelectListState.bookOnDiskListItems.isEmpty()
}
