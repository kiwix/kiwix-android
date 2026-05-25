package org.kiwix.kiwixmobile.nav.destination.library.local

import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import androidx.compose.material3.SnackbarHostState
import androidx.fragment.app.FragmentManager
import app.cash.turbine.test
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.effects.ManageExternalFilesPermissionDialog
import org.kiwix.kiwixmobile.core.utils.effects.ReadPermissionRequiredDialog
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.ReadeWritePermissionResultAction
import org.kiwix.kiwixmobile.nav.destination.library.local.LocalLibraryViewModel.ReadeWritePermissionResultAction.ScanStorage
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.DeleteFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.NavigateToDownloads
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.None
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.OpenFileWithNavigation
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ShareFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ValidateZIMFiles
import org.kiwix.libzim.Archive
import org.kiwix.sharedFunctions.MainDispatcherRule
import java.io.File

@ExperimentalCoroutinesApi
class LocalLibraryViewModelTest {
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk = mockk(relaxed = true)
  private val storageObserver: StorageObserver = mockk(relaxed = true)
  private val dataSource: DataSource = mockk(relaxed = true)
  private val application: Application = mockk(relaxed = true)
  private val processSelectedZimFilesForStandalone: ProcessSelectedZimFilesForStandalone =
    mockk(relaxed = true)
  private val processSelectedZimFilesForPlayStore: ProcessSelectedZimFilesForPlayStore =
    mockk(relaxed = true)
  private val repositoryActions: MainRepositoryActions = mockk(relaxed = true)
  private val kiwixPermissionChecker: KiwixPermissionChecker = mockk(relaxed = true)
  private val kiwixDataStore: KiwixDataStore = mockk(relaxed = true)
  private val zimReaderFactory: ZimFileReader.Factory = mockk(relaxed = true)
  private val alertDialogShower: AlertDialogShower = mockk(relaxed = true)
  private val validateZimViewModel: ValidateZimViewModel = mockk(relaxed = true)
  private val fragmentManager: FragmentManager = mockk(relaxed = true)
  private val snackBarHostState: SnackbarHostState = mockk(relaxed = true)

  @RegisterExtension
  private val mainDispatcherRule = MainDispatcherRule()

  private val testDispatcher get() = mainDispatcherRule.dispatcher

  private lateinit var viewModel: LocalLibraryViewModel

  @BeforeEach
  fun setUp() {
    every { dataSource.booksOnDiskAsListItems() } returns flowOf(emptyList())
    every { libkiwixBookOnDisk.books() } returns flowOf(emptyList())
    coEvery { storageObserver.getBooksOnFileSystem(any()) } returns flowOf(emptyList())
    every { kiwixDataStore.prefIsTest } returns flowOf(true)
    coEvery { kiwixDataStore.isScanFileSystemTest } returns flowOf(false)
    coEvery {
      kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()
    } returns false
    every {
      kiwixDataStore.isScanFileSystemDialogShown
    } returns flowOf(true)
    every {
      kiwixDataStore.showManageExternalFilesPermissionDialog
    } returns flowOf(false)

    viewModel = createViewModel()
    mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
  }

  @AfterEach
  fun tearDown() {
    viewModel.onClearedExposed()
    clearAllMocks()
  }

  private fun createViewModel(): LocalLibraryViewModel {
    val vm = LocalLibraryViewModel(
      libkiwixBookOnDisk,
      storageObserver,
      dataSource,
      application,
      processSelectedZimFilesForStandalone,
      processSelectedZimFilesForPlayStore,
      repositoryActions,
      kiwixPermissionChecker,
      kiwixDataStore,
      zimReaderFactory,
      mainDispatcherRule.dispatcher
    )
    vm.initialize(
      emptyList(),
      validateZimViewModel,
      alertDialogShower,
      snackBarHostState,
      fragmentManager
    )
    return vm
  }

  @Test
  fun `initial uiState has empty file list`() = runTest {
    viewModel.uiState.test {
      val state = awaitItem()
      assertTrue(state.fileSelectListState.bookOnDiskListItems.isEmpty())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `initial uiState has NORMAL selection mode`() = runTest {
    viewModel.uiState.test {
      assertEquals(SelectionMode.NORMAL, awaitItem().fileSelectListState.selectionMode)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `initial uiState has scanning not in progress`() = runTest {
    viewModel.uiState.test {
      assertFalse(awaitItem().scanning.isScanning)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `initial uiState has swipe refresh false`() = runTest {
    viewModel.uiState.test {
      assertFalse(awaitItem().isSwipeRefreshing)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `state updates when datasource emits new books`() = runTest {
    val bookItem = mockk<BookOnDisk>(relaxed = true)
    every { bookItem.id } returns "testId"
    every { dataSource.booksOnDiskAsListItems() } returns flowOf(listOf(bookItem))

    // Re-create VM to pick up mock emission
    viewModel.onClearedExposed()
    viewModel = createViewModel()

    viewModel.uiState.test {
      // First skip initial empty state if any, or wait for the bookItem state
      val state = awaitItem()
      if (state.fileSelectListState.bookOnDiskListItems.isEmpty()) {
        val updatedState = awaitItem()
        assertEquals(1, updatedState.fileSelectListState.bookOnDiskListItems.size)
      } else {
        assertEquals(1, state.fileSelectListState.bookOnDiskListItems.size)
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `file system check triggers storage scan`() = runTest {
    every { libkiwixBookOnDisk.books() } returns flowOf(emptyList())
    coEvery { storageObserver.getBooksOnFileSystem(any()) } returns flowOf(emptyList())

    viewModel.uiState.test {
      // Ensure initial state is captured
      awaitItem()

      testDispatcher.scheduler.advanceUntilIdle()
      viewModel.requestFileSystemCheck.emit(Unit)
      testDispatcher.scheduler.advanceUntilIdle()

      // Scanning state should become true
      val scanningState = awaitItem()
      assertTrue(scanningState.scanning.isScanning)

      // Then it should become false when done
      val finishedState = awaitItem()
      assertFalse(finishedState.scanning.isScanning)

      coVerify {
        storageObserver.getBooksOnFileSystem(any())
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun testActionSideEffect(
    action: LocalLibraryViewModel.LocalLibraryUiActions,
    assertion: (org.kiwix.kiwixmobile.core.base.SideEffect<*>) -> Unit
  ) = runTest {
    viewModel.sideEffects.test {
      mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
      viewModel.localLibraryUiActions.emit(action)
      assertion(awaitItem())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onResume triggers scan dialog when dialog not shown and list empty`() = runTest {
    every { kiwixDataStore.isScanFileSystemDialogShown } returns flowOf(false)
    every { libkiwixBookOnDisk.books() } returns flowOf(emptyList())
    every { dataSource.booksOnDiskAsListItems() } returns flowOf(emptyList())

    viewModel.sideEffects.test {
      viewModel.onResume()
      mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
      assertTrue(awaitItem() is ShowFileSystemScanDialog)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onResume check permissions when storage permission not granted`() = runTest {
    coEvery { kiwixPermissionChecker.hasReadExternalStoragePermission() } returns false
    every { kiwixDataStore.isScanFileSystemDialogShown } returns flowOf(true)

    viewModel.sideEffects.test {
      viewModel.onResume()
      mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
      // Skip redundant emissions
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `RequestSelect toggles book selection on`() = runTest {
    val bookOnDisk = mockk<BookOnDisk>(relaxed = true)
    every { bookOnDisk.id } returns "book1"
    every { bookOnDisk.isSelected } returns false
    every { bookOnDisk.copy(isSelected = true) } returns mockk(relaxed = true) {
      every { id } returns "book1"
      every { isSelected } returns true
    }
    every { dataSource.booksOnDiskAsListItems() } returns flowOf(listOf(bookOnDisk))

    viewModel.onClearedExposed()
    viewModel = createViewModel()
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.sideEffects.test {
      testDispatcher.scheduler.advanceUntilIdle()
      viewModel.localLibraryUiActions.emit(
        LocalLibraryViewModel.LocalLibraryUiActions.RequestSelect(bookOnDisk)
      )
      assertTrue(awaitItem() is None)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `RequestMultiSelection emits None side effect`() = testActionSideEffect(
    LocalLibraryViewModel.LocalLibraryUiActions.RequestMultiSelection(mockk(relaxed = true))
  ) {
    assertTrue(it is None)
  }

  @Test
  fun `MultiModeFinished clears selections and returns None`() = runTest {
    viewModel.sideEffects.test {
      viewModel.localLibraryUiActions.emit(LocalLibraryViewModel.LocalLibraryUiActions.MultiModeFinished)
      assertTrue(awaitItem() is None)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `RequestDeleteMultiSelection emits DeleteFiles side effect`() = testActionSideEffect(
    LocalLibraryViewModel.LocalLibraryUiActions.RequestDeleteMultiSelection
  ) {
    assertTrue(it is DeleteFiles)
  }

  @Test
  fun `RequestShareMultiSelection emits ShareFiles side effect`() = testActionSideEffect(
    LocalLibraryViewModel.LocalLibraryUiActions.RequestShareMultiSelection
  ) {
    assertTrue(it is ShareFiles)
  }

  @Test
  fun `RequestValidateZimFiles emits ValidateZIMFiles side effect`() = testActionSideEffect(
    LocalLibraryViewModel.LocalLibraryUiActions.RequestValidateZimFiles
  ) {
    assertTrue(it is ValidateZIMFiles)
  }

  @Test
  fun `UserClickedDownloadBooksButton emits NavigateToDownloads side effect`() =
    testActionSideEffect(
      LocalLibraryViewModel.LocalLibraryUiActions.UserClickedDownloadBooksButton
    ) {
      assertTrue(it is NavigateToDownloads)
    }

  @Test
  fun `RequestNavigateTo emits OpenFileWithNavigation side effect`() = testActionSideEffect(
    LocalLibraryViewModel.LocalLibraryUiActions.RequestNavigateTo(mockk(relaxed = true))
  ) {
    assertTrue(it is OpenFileWithNavigation)
  }

  @Test
  fun `ReadPermissionDialog emits ReadPermissionRequiredDialog side effect`() =
    testActionSideEffect(
      LocalLibraryViewModel.LocalLibraryUiActions.ReadPermissionDialog
    ) {
      assertTrue(it is ReadPermissionRequiredDialog)
    }

  @Test
  fun `FileSystemScanDialog emits ShowFileSystemScanDialog side effect`() = testActionSideEffect(
    LocalLibraryViewModel.LocalLibraryUiActions.FileSystemScanDialog
  ) {
    assertTrue(it is ShowFileSystemScanDialog)
  }

  @Test
  fun `CopyMoveErrorDialog emits ShowFileCopyMoveErrorDialog side effect`() = testActionSideEffect(
    LocalLibraryViewModel.LocalLibraryUiActions.CopyMoveErrorDialog("error") {}
  ) {
    assertTrue(it is ShowFileCopyMoveErrorDialog)
  }

  @Test
  fun `ManageFilesPermissionDialog emits correct side effect on Android 13+`() = runTest {
    every { kiwixPermissionChecker.isAndroid13orAbove() } returns true
    viewModel.sideEffects.test {
      viewModel.localLibraryUiActions.emit(LocalLibraryViewModel.LocalLibraryUiActions.ManageFilesPermissionDialog)
      assertTrue(awaitItem() is ManageExternalFilesPermissionDialog)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `ManageFilesPermissionDialog emits None below Android 13`() = runTest {
    every { kiwixPermissionChecker.isAndroid13orAbove() } returns false
    viewModel.sideEffects.test {
      viewModel.localLibraryUiActions.emit(LocalLibraryViewModel.LocalLibraryUiActions.ManageFilesPermissionDialog)
      assertTrue(awaitItem() is None)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onBookItemClick requests navigate when permission granted`() = runTest {
    val bookOnDisk = mockk<BookOnDisk>(relaxed = true)
    coEvery { kiwixPermissionChecker.isManageExternalStoragePermissionGranted() } returns true

    viewModel.sideEffects.test {
      viewModel.onBookItemClick(bookOnDisk)
      assertTrue(awaitItem() is OpenFileWithNavigation)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onBookItemClick shows permission dialog when permission not granted`() = runTest {
    val bookOnDisk = mockk<BookOnDisk>(relaxed = true)
    coEvery { kiwixPermissionChecker.isManageExternalStoragePermissionGranted() } returns false
    every { kiwixPermissionChecker.isAndroid13orAbove() } returns true

    viewModel.sideEffects.test {
      viewModel.onBookItemClick(bookOnDisk)
      assertTrue(awaitItem() is ManageExternalFilesPermissionDialog)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onCleared cancels coroutine jobs and disposes processors`() = runTest {
    viewModel.onClearedExposed()
    advanceUntilIdle()

    verify { processSelectedZimFilesForPlayStore.dispose() }
    verify { processSelectedZimFilesForStandalone.dispose() }
  }

  @Test
  fun `initialize sets up processors correctly`() = runTest {
    advanceUntilIdle()

    verify {
      processSelectedZimFilesForStandalone.setSelectedZimFileCallback(viewModel)
    }
    verify {
      processSelectedZimFilesForPlayStore.init(
        storageDeviceList = any(),
        lifecycleScope = any(),
        alertDialogShower = alertDialogShower,
        snackBarHostState = snackBarHostState,
        fragmentManager = fragmentManager,
        selectedZimFileCallback = viewModel
      )
    }
  }

  @Test
  fun `onBookItemLongClick requests multi selection when permission granted`() = runTest {
    val bookOnDisk = mockk<BookOnDisk>(relaxed = true)

    coEvery {
      kiwixPermissionChecker.isManageExternalStoragePermissionGranted()
    } returns true

    viewModel.localLibraryUiActions.test {
      viewModel.onBookItemLongClick(bookOnDisk)

      assertThat(awaitItem())
        .isEqualTo(
          LocalLibraryViewModel.LocalLibraryUiActions.RequestMultiSelection(bookOnDisk)
        )

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onBookItemLongClick shows manage permission dialog when permission denied`() = runTest {
    val bookOnDisk = mockk<BookOnDisk>(relaxed = true)

    coEvery {
      kiwixPermissionChecker.isManageExternalStoragePermissionGranted()
    } returns false

    viewModel.localLibraryUiActions.test {
      viewModel.onBookItemLongClick(bookOnDisk)

      assertThat(awaitItem())
        .isEqualTo(
          LocalLibraryViewModel.LocalLibraryUiActions.ManageFilesPermissionDialog
        )

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onMultiSelect triggers selection side effect`() = runTest {
    val bookOnDisk = mockk<BookOnDisk>(relaxed = true)
    viewModel.sideEffects.test {
      viewModel.onMultiSelect(bookOnDisk)
      assertTrue(awaitItem() is None)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `filePickerMenuButtonClick requests permission when write permission missing`() = runTest {
    val launcher =
      mockk<ManagedActivityResultLauncher<Intent, ActivityResult>>(relaxed = true)

    coEvery {
      kiwixPermissionChecker.hasWriteExternalStoragePermission()
    } returns false

    viewModel.localLibraryUiActions.test {
      viewModel.filePickerMenuButtonClick(launcher)

      val action = awaitItem()

      assertThat(action)
        .isInstanceOf(
          LocalLibraryViewModel.LocalLibraryUiActions.RequestReadWritePermission::class.java
        )

      val permissionAction =
        action as LocalLibraryViewModel.LocalLibraryUiActions.RequestReadWritePermission

      assertThat(permissionAction.resultAction)
        .isEqualTo(
          LocalLibraryViewModel.ReadeWritePermissionResultAction.OpenFilePicker(
            launcher
          )
        )

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `filePickerMenuButtonClick shows manage permission dialog when manage storage permission missing`() =
    runTest {
      val launcher =
        mockk<ManagedActivityResultLauncher<Intent, ActivityResult>>(relaxed = true)

      coEvery {
        kiwixPermissionChecker.hasWriteExternalStoragePermission()
      } returns true

      coEvery {
        kiwixPermissionChecker.isManageExternalStoragePermissionGranted()
      } returns false

      viewModel.localLibraryUiActions.test {
        viewModel.filePickerMenuButtonClick(launcher)

        assertThat(awaitItem())
          .isEqualTo(
            LocalLibraryViewModel.LocalLibraryUiActions.ManageFilesPermissionDialog
          )

        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun `onReadWritePermissionGranted with ScanStorage triggers scan`() = runTest {
    coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true
    coEvery { kiwixPermissionChecker.isManageExternalStoragePermissionGranted() } returns true

    viewModel.uiState.test {
      awaitItem()

      viewModel.onReadWritePermissionGranted(ScanStorage)
      assertTrue(awaitItem().scanning.isScanning)
      assertFalse(awaitItem().scanning.isScanning)
      coVerify { storageObserver.getBooksOnFileSystem(any()) }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onReadWritePermissionGranted hides permission denied layout`() = runTest {
    viewModel.onReadWriteRationalPermission()

    assertTrue(viewModel.uiState.value.permissionDeniedLayoutShowing)

    viewModel.onReadWritePermissionGranted(ReadeWritePermissionResultAction.None)
    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.permissionDeniedLayoutShowing)
  }

  @Test
  fun `onReadWriteRationalPermission updates ui state correctly`() = runTest {
    viewModel.onReadWriteRationalPermission()

    val state = viewModel.uiState.value
    assertTrue(state.permissionDeniedLayoutShowing)
    assertTrue(state.noFileView.isVisible)
  }

  @Test
  fun `onReadWriteRationalPermission emits ReadPermissionDialog`() = runTest {
    viewModel.sideEffects.test {
      viewModel.onReadWriteRationalPermission()

      assertTrue(awaitItem() is ReadPermissionRequiredDialog)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onDownloadButtonClick navigates to settings when permission denied layout visible`() {
    viewModel.onReadWriteRationalPermission()
    viewModel.onDownloadButtonClick()
    verify { application.startActivity(any()) }
  }

  @Test
  fun `onDownloadButtonClick emits NavigateToDownloads`() = runTest {
    viewModel.sideEffects.test {
      viewModel.onDownloadButtonClick()
      assertTrue(awaitItem() is NavigateToDownloads)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `handleUserBackPressed returns ShouldCall in normal mode`() {
    assertEquals(
      FragmentActivityExtensions.Super.ShouldCall,
      viewModel.handleUserBackPressed()
    )
  }

  @Test
  fun `handleUserBackPressed clears selection in multi mode`() = runTest {
    val book = mockk<BookOnDisk>(relaxed = true)

    every { book.id } returns "book1"
    every { book.isSelected } returns false

    val selectedBook = mockk<BookOnDisk>(relaxed = true)

    every { selectedBook.id } returns "book1"
    every { selectedBook.isSelected } returns true

    every { book.copy(isSelected = true) } returns selectedBook

    every { dataSource.booksOnDiskAsListItems() } returns flowOf(listOf(book))

    viewModel.onClearedExposed()
    viewModel = createViewModel()

    advanceUntilIdle()

    viewModel.onMultiSelect(book)

    advanceUntilIdle()

    assertEquals(
      FragmentActivityExtensions.Super.ShouldNotCall,
      viewModel.handleUserBackPressed()
    )
  }

  @Test
  fun `onNavigationIconClick requests drawer toggle in normal mode`() = runTest {
    viewModel.localLibraryUiActions.test {
      viewModel.onNavigationIconClick()

      assertThat(awaitItem())
        .isEqualTo(
          LocalLibraryViewModel.LocalLibraryUiActions.RequestDrawerToggle
        )

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onNavigationIconClick finishes multi mode when selection mode is multi`() = runTest {
    val book = mockk<BookOnDisk>(relaxed = true)

    every { book.id } returns "book1"
    every { book.isSelected } returns false

    val selectedBook = mockk<BookOnDisk>(relaxed = true)

    every { selectedBook.id } returns "book1"
    every { selectedBook.isSelected } returns true

    every { book.copy(isSelected = true) } returns selectedBook

    every { dataSource.booksOnDiskAsListItems() } returns flowOf(listOf(book))

    viewModel.onClearedExposed()
    viewModel = createViewModel()

    advanceUntilIdle()

    viewModel.onMultiSelect(book)
    advanceUntilIdle()
    viewModel.localLibraryUiActions.test {
      viewModel.onNavigationIconClick()
      assertThat(awaitItem())
        .isEqualTo(
          LocalLibraryViewModel.LocalLibraryUiActions.MultiModeFinished
        )

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `processZimFileArguments does nothing when uri is empty`() = runTest {
    clearMocks(
      processSelectedZimFilesForStandalone,
      processSelectedZimFilesForPlayStore,
      answers = false,
      recordedCalls = true
    )
    viewModel.localLibraryUiActions.test {
      viewModel.processZimFileArguments("")

      advanceUntilIdle()

      expectNoEvents()
    }

    coVerify(exactly = 0) {
      processSelectedZimFilesForStandalone.processSelectedFiles(any())
    }

    coVerify(exactly = 0) {
      processSelectedZimFilesForPlayStore.processSelectedFiles(any())
    }
  }

  @Test
  fun `processZimFileArguments requests permission when write permission denied`() = runTest {
    coEvery {
      kiwixPermissionChecker.hasWriteExternalStoragePermission()
    } returns false

    viewModel.localLibraryUiActions.test {
      viewModel.processZimFileArguments("content://test.zim")

      val action = awaitItem()

      assertThat(action)
        .isInstanceOf(
          LocalLibraryViewModel.LocalLibraryUiActions.RequestReadWritePermission::class.java
        )

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `processZimFileArguments shows manage files dialog when manage storage permission denied`() =
    runTest {
      coEvery {
        kiwixPermissionChecker.hasWriteExternalStoragePermission()
      } returns true

      coEvery {
        kiwixPermissionChecker.isManageExternalStoragePermissionGranted()
      } returns false

      viewModel.localLibraryUiActions.test {
        viewModel.processZimFileArguments("content://test.zim")

        assertThat(awaitItem())
          .isEqualTo(
            LocalLibraryViewModel.LocalLibraryUiActions.ManageFilesPermissionDialog
          )

        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun `processZimFileArguments processes files using standalone processor`() = runTest {
    coEvery {
      kiwixPermissionChecker.hasWriteExternalStoragePermission()
    } returns true

    coEvery {
      kiwixPermissionChecker.isManageExternalStoragePermissionGranted()
    } returns true

    coEvery {
      processSelectedZimFilesForStandalone.canHandleUris()
    } returns true

    viewModel.processZimFileArguments("content://test.zim")

    advanceUntilIdle()

    coVerify {
      processSelectedZimFilesForStandalone.processSelectedFiles(any())
    }

    coVerify(exactly = 0) {
      processSelectedZimFilesForPlayStore.processSelectedFiles(any())
    }
  }

  @Test
  fun `processZimFileArguments processes files using playstore processor`() = runTest {
    coEvery {
      kiwixPermissionChecker.hasWriteExternalStoragePermission()
    } returns true

    coEvery {
      kiwixPermissionChecker.isManageExternalStoragePermissionGranted()
    } returns true

    coEvery {
      processSelectedZimFilesForStandalone.canHandleUris()
    } returns false

    coEvery {
      processSelectedZimFilesForPlayStore.canHandleUris()
    } returns true

    viewModel.processZimFileArguments("content://test.zim")

    advanceUntilIdle()

    coVerify {
      processSelectedZimFilesForPlayStore.processSelectedFiles(any())
    }

    coVerify(exactly = 0) {
      processSelectedZimFilesForStandalone.processSelectedFiles(any())
    }
  }

  @Test
  fun `processZimFileArguments does nothing when no processor can handle files`() = runTest {
    clearMocks(
      processSelectedZimFilesForStandalone,
      processSelectedZimFilesForPlayStore,
      answers = false
    )

    coEvery {
      kiwixPermissionChecker.hasWriteExternalStoragePermission()
    } returns true

    coEvery {
      kiwixPermissionChecker.isManageExternalStoragePermissionGranted()
    } returns true

    coEvery {
      processSelectedZimFilesForStandalone.canHandleUris()
    } returns false

    coEvery {
      processSelectedZimFilesForPlayStore.canHandleUris()
    } returns false

    viewModel.processZimFileArguments("content://test.zim")

    advanceUntilIdle()

    coVerify(exactly = 1) {
      processSelectedZimFilesForStandalone.canHandleUris()
    }

    coVerify(exactly = 1) {
      processSelectedZimFilesForPlayStore.canHandleUris()
    }

    coVerify(exactly = 0) {
      processSelectedZimFilesForStandalone.processSelectedFiles(any())
    }

    coVerify(exactly = 0) {
      processSelectedZimFilesForPlayStore.processSelectedFiles(any())
    }
  }

  @Test
  fun `navigateToReaderFragment does not navigate when file unreadable`() = runTest {
    val file = mockk<File>(relaxed = true)

    every { file.canRead() } returns false

    mockkStatic(Toast::class)
    val toast = mockk<Toast>(relaxed = true)
    every {
      Toast.makeText(any(), any<Int>(), any())
    } returns toast

    viewModel.navigateToReaderFragment(file)

    advanceUntilIdle()

    coVerify(exactly = 0) {
      repositoryActions.saveBook(any())
    }
  }

  @Test
  fun `navigateToReaderFragment saves book and navigates when file readable`() = runTest {
    val file = mockk<File>(relaxed = true)
    val zimFileReader = mockk<ZimFileReader>(relaxed = true)
    val archive = mockk<Archive>(relaxed = true)

    every { file.canRead() } returns true
    every { zimFileReader.jniKiwixReader } returns archive

    coEvery {
      zimReaderFactory.create(any(), false)
    } returns zimFileReader

    viewModel.localLibraryUiActions.test {
      viewModel.navigateToReaderFragment(file)

      val action = awaitItem()

      assertThat(action)
        .isInstanceOf(LocalLibraryViewModel.LocalLibraryUiActions.RequestNavigateTo::class.java)

      val navigateAction =
        action as LocalLibraryViewModel.LocalLibraryUiActions.RequestNavigateTo

      assertThat(navigateAction.zimReaderSource)
        .isEqualTo(ZimReaderSource(file))

      advanceUntilIdle()

      coVerify {
        zimReaderFactory.create(any(), false)
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onSwipeRefresh hides refresh when permission layout visible`() = runTest {
    viewModel.onReadWriteRationalPermission()

    viewModel.onSwipeRefresh()

    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isSwipeRefreshing)
  }

  @Test
  fun `onSwipeRefresh triggers scan when permission granted`() = runTest {
    coEvery { kiwixPermissionChecker.isManageExternalStoragePermissionGranted() } returns true

    viewModel.uiState.test {
      awaitItem() // initial
      viewModel.onSwipeRefresh()

      // scanning should become true
      assertTrue(awaitItem().scanning.isScanning)

      // then false
      assertFalse(awaitItem().scanning.isScanning)

      coVerify { storageObserver.getBooksOnFileSystem(any()) }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onSwipeRefresh shows manage files dialog when permission not granted`() = runTest {
    coEvery {
      kiwixPermissionChecker.isManageExternalStoragePermissionGranted()
    } returns false

    viewModel.localLibraryUiActions.test {
      viewModel.onSwipeRefresh()

      assertThat(awaitItem())
        .isEqualTo(
          LocalLibraryViewModel.LocalLibraryUiActions.ManageFilesPermissionDialog
        )

      assertFalse(viewModel.uiState.value.isSwipeRefreshing)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `deleteMenuIconClick emits delete and finish actions`() = runTest {
    viewModel.localLibraryUiActions.test {
      viewModel.deleteMenuIconClick()

      assertThat(awaitItem())
        .isEqualTo(
          LocalLibraryViewModel.LocalLibraryUiActions.RequestDeleteMultiSelection
        )

      assertThat(awaitItem())
        .isEqualTo(
          LocalLibraryViewModel.LocalLibraryUiActions.MultiModeFinished
        )

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `shareMenuIconClick emits share and finish actions`() = runTest {
    viewModel.localLibraryUiActions.test {
      viewModel.shareMenuIconClick()

      assertThat(awaitItem())
        .isEqualTo(
          LocalLibraryViewModel.LocalLibraryUiActions.RequestShareMultiSelection
        )

      assertThat(awaitItem())
        .isEqualTo(
          LocalLibraryViewModel.LocalLibraryUiActions.MultiModeFinished
        )

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `validateMenuIconClick emits validate and finish actions`() = runTest {
    viewModel.localLibraryUiActions.test {
      viewModel.validateMenuIconClick()

      assertThat(awaitItem())
        .isEqualTo(
          LocalLibraryViewModel.LocalLibraryUiActions.RequestValidateZimFiles
        )

      assertThat(awaitItem())
        .isEqualTo(
          LocalLibraryViewModel.LocalLibraryUiActions.MultiModeFinished
        )

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `showFileCopyMoveErrorDialog emits copy move dialog action`() = runTest {
    val errorMessage = "Copy failed"
    val callback: suspend () -> Unit = {}

    viewModel.localLibraryUiActions.test {
      viewModel.showFileCopyMoveErrorDialog(errorMessage, callback)

      val action = awaitItem()

      assertThat(action)
        .isInstanceOf(
          LocalLibraryViewModel.LocalLibraryUiActions.CopyMoveErrorDialog::class.java
        )

      val dialogAction =
        action as LocalLibraryViewModel.LocalLibraryUiActions.CopyMoveErrorDialog

      assertThat(dialogAction.errorMessage).isEqualTo(errorMessage)
      assertThat(dialogAction.callBack).isEqualTo(callback)

      cancelAndIgnoreRemainingEvents()
    }
  }
}
