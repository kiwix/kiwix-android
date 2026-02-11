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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.MULTI
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.zimManager.DEFAULT_PROGRESS
import org.kiwix.kiwixmobile.zimManager.MAX_PROGRESS
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.DeleteFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.NavigateToDownloads
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.None
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.OpenFileWithNavigation
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ShareFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.StartMultiSelection
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ValidateZIMFiles
import org.kiwix.libkiwix.Book
import javax.inject.Inject

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
  val context: Application
) : ViewModel() {
  @Suppress("InjectDispatcher")
  private var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

  private lateinit var validateZimViewModel: ValidateZimViewModel
  private lateinit var alertDialogShower: AlertDialogShower

  val sideEffects: MutableSharedFlow<SideEffect<*>> = MutableSharedFlow()
  val fileSelectListStates: MutableLiveData<FileSelectListState> = MutableLiveData()
  val deviceListScanningProgress = MutableLiveData<Int>()
  val requestFileSystemCheck = MutableSharedFlow<Unit>(replay = 0)
  val fileSelectActions = MutableSharedFlow<FileSelectActions>()

  private val coroutineJobs: MutableList<Job> = mutableListOf()

  init {
    observeCoroutineFlows()
  }

  fun setValidateZimViewModel(validateZimViewModel: ValidateZimViewModel) {
    this.validateZimViewModel = validateZimViewModel
  }

  fun setAlertDialogShower(alertDialogShower: AlertDialogShower) {
    this.alertDialogShower = alertDialogShower
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
              is FileSelectActions.RequestNavigateTo ->
                OpenFileWithNavigation(action.bookOnDisk)
              is FileSelectActions.RequestMultiSelection ->
                startMultiSelectionAndSelectBook(action.bookOnDisk)
              FileSelectActions.RequestDeleteMultiSelection ->
                DeleteFiles(selectionsFromState(), alertDialogShower)
              FileSelectActions.RequestShareMultiSelection ->
                ShareFiles(selectionsFromState())
              FileSelectActions.RequestValidateZimFiles ->
                ValidateZIMFiles(selectionsFromState(), alertDialogShower, validateZimViewModel)
              FileSelectActions.MultiModeFinished ->
                noSideEffectAndClearSelectionState()
              is FileSelectActions.RequestSelect ->
                noSideEffectSelectBook(action.bookOnDisk)
              FileSelectActions.RestartActionMode ->
                StartMultiSelection(fileSelectActions)
              FileSelectActions.UserClickedDownloadBooksButton ->
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
    fileSelectListStates.value?.let {
      fileSelectListStates.postValue(
        it.copy(
          bookOnDiskListItems = selectBook(it, bookOnDisk),
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
    fileSelectListStates.value?.let {
      fileSelectListStates.postValue(
        it.copy(bookOnDiskListItems = selectBook(it, bookOnDisk))
      )
    }
    return None
  }

  private fun selectionsFromState() = fileSelectListStates.value?.selectedBooks.orEmpty()

  private fun noSideEffectAndClearSelectionState(): SideEffect<Unit> {
    fileSelectListStates.value?.let {
      fileSelectListStates.postValue(
        it.copy(
          bookOnDiskListItems =
            it.bookOnDiskListItems.map { booksOnDiskListItem ->
              booksOnDiskListItem.apply { isSelected = false }
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
      deviceListScanningProgress.postValue(DEFAULT_PROGRESS)
      booksFromStorageNotIn(
        booksFromDao,
        object : ScanningProgressListener {
          override fun onProgressUpdate(scannedDirectory: Int, totalDirectory: Int) {
            val overallProgress =
              (scannedDirectory.toDouble() / totalDirectory.toDouble() * MAX_PROGRESS).toInt()
            if (overallProgress != MAX_PROGRESS) {
              deviceListScanningProgress.postValue(overallProgress)
            }
          }
        }
      )
    }
    .onEach {
      deviceListScanningProgress.postValue(MAX_PROGRESS)
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
        val currentState = fileSelectListStates.value
        val updatedState = currentState?.let {
          inheritSelections(it, newList.toMutableList())
        } ?: FileSelectListState(newList)

        fileSelectListStates.postValue(updatedState)
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
}
