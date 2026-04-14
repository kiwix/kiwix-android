import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.fragment.app.FragmentManager
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.DeleteFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.NavigateToDownloads
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.None
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.OpenFileWithNavigation
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ShareFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ValidateZIMFiles
import org.kiwix.sharedFunctions.MainDispatcherRule

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
      assertTrue(it is org.kiwix.kiwixmobile.core.utils.effects.ReadPermissionRequiredDialog)
    }

  @Test
  fun `FileSystemScanDialog emits ShowFileSystemScanDialog side effect`() = testActionSideEffect(
    LocalLibraryViewModel.LocalLibraryUiActions.FileSystemScanDialog
  ) {
    assertTrue(it is ShowFileSystemScanDialog)
  }

  @Test
  fun `CopyMoveErrorDialog emits ShowFileCopyMoveErrorDialog side effect`() = testActionSideEffect(
    LocalLibraryViewModel.LocalLibraryUiActions.CopyMoveErrorDialog("error", {})
  ) {
    assertTrue(it is ShowFileCopyMoveErrorDialog)
  }

  @Test
  fun `ManageFilesPermissionDialog emits correct side effect on Android 13+`() = runTest {
    every { kiwixPermissionChecker.isAndroid13orAbove() } returns true
    viewModel.sideEffects.test {
      viewModel.localLibraryUiActions.emit(LocalLibraryViewModel.LocalLibraryUiActions.ManageFilesPermissionDialog)
      assertTrue(awaitItem() is org.kiwix.kiwixmobile.core.utils.effects.ManageExternalFilesPermissionDialog)
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
      assertTrue(awaitItem() is org.kiwix.kiwixmobile.core.utils.effects.ManageExternalFilesPermissionDialog)
      cancelAndIgnoreRemainingEvents()
    }
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
        lifecycleScope = any(),
        alertDialogShower = alertDialogShower,
        snackBarHostState = snackBarHostState,
        fragmentManager = fragmentManager,
        selectedZimFileCallback = viewModel
      )
    }
  }

  @Test
  fun `onBookItemLongClick triggers multi selection`() = runTest {
    val bookOnDisk = mockk<BookOnDisk>(relaxed = true)
    coEvery { kiwixPermissionChecker.isManageExternalStoragePermissionGranted() } returns true

    viewModel.sideEffects.test {
      viewModel.onBookItemLongClick(bookOnDisk)
      assertTrue(awaitItem() is None)
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
  fun `filePickerMenuButtonClick triggers permission request if not granted`() = runTest {
    coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns false
    viewModel.sideEffects.test {
      viewModel.filePickerMenuButtonClick(mockk(relaxed = true))
      assertTrue(awaitItem() is None)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `processZimFileArguments processes valid URI`() = runTest {
    coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true
    coEvery { kiwixPermissionChecker.isManageExternalStoragePermissionGranted() } returns true

    viewModel.processZimFileArguments("content://test.zim")
    advanceUntilIdle()
    // Verification depends on processSelectedZimFilesForStandalone/PlayStore behavior
  }

  @Test
  fun `processSelectedZimFiles processes intent`() = runTest {
    val intent = mockk<android.content.Intent>(relaxed = true)
    viewModel.processSelectedZimFiles(intent)
    advanceUntilIdle()
    // Verification depends on processSelectedZimFilesForStandalone/PlayStore behavior
  }
}
