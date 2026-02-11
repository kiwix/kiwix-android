package org.kiwix.kiwixmobile.nav.destination.library.local

import android.app.Application
import org.kiwix.kiwixmobile.core.utils.files.ScanningProgressListener
import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.asFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.zimManager.BookTestWrapper
import org.kiwix.kiwixmobile.zimManager.testFlow
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.MULTI
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode.NORMAL
import org.kiwix.kiwixmobile.language.viewmodel.flakyTest
import org.kiwix.kiwixmobile.nav.destination.library.local.FileSelectActions.MultiModeFinished
import org.kiwix.kiwixmobile.nav.destination.library.local.FileSelectActions.RequestDeleteMultiSelection
import org.kiwix.kiwixmobile.nav.destination.library.local.FileSelectActions.RequestMultiSelection
import org.kiwix.kiwixmobile.nav.destination.library.local.FileSelectActions.RequestNavigateTo
import org.kiwix.kiwixmobile.nav.destination.library.local.FileSelectActions.RequestSelect
import org.kiwix.kiwixmobile.nav.destination.library.local.FileSelectActions.RequestShareMultiSelection
import org.kiwix.kiwixmobile.nav.destination.library.local.FileSelectActions.RequestValidateZimFiles
import org.kiwix.kiwixmobile.nav.destination.library.local.FileSelectActions.RestartActionMode
import org.kiwix.kiwixmobile.nav.destination.library.local.FileSelectActions.UserClickedDownloadBooksButton
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.DeleteFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.NavigateToDownloads
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.None
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.OpenFileWithNavigation
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ShareFiles
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.StartMultiSelection
import org.kiwix.kiwixmobile.zimManager.fileselectView.effects.ValidateZIMFiles
import org.kiwix.libkiwix.Book
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.bookOnDisk
import org.kiwix.sharedFunctions.libkiwixBook

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(InstantExecutorExtension::class)
class LocalLibraryViewModelTest {
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk = mockk()
  private val storageObserver: StorageObserver = mockk()
  private val dataSource: DataSource = mockk()
  private val application: Application = mockk()
  private val alertDialogShower: AlertDialogShower = mockk()
  private val validateZimViewModel: ValidateZimViewModel = mockk()

  lateinit var viewModel: LocalLibraryViewModel

  private val booksOnFileSystem = MutableStateFlow<List<Book>>(emptyList())
  private val books = MutableStateFlow<List<BookOnDisk>>(emptyList())
  private val booksOnDiskListItems = MutableStateFlow<List<BooksOnDiskListItem>>(emptyList())
  private val testDispatcher = StandardTestDispatcher()

  @AfterAll
  fun teardown() {
    Dispatchers.resetMain()
  }

  @BeforeEach
  fun init() {
    Dispatchers.setMain(testDispatcher)
    clearAllMocks()
    every { libkiwixBookOnDisk.books() } returns books
    every {
      storageObserver.getBooksOnFileSystem(
        any<ScanningProgressListener>()
      )
    } returns booksOnFileSystem
    every { dataSource.booksOnDiskAsListItems() } returns booksOnDiskListItems
    booksOnFileSystem.value = emptyList()
    books.value = emptyList()
    booksOnDiskListItems.value = emptyList()

    viewModel = LocalLibraryViewModel(
      libkiwixBookOnDisk,
      storageObserver,
      dataSource,
      application
    ).apply {
      setAlertDialogShower(alertDialogShower)
      setValidateZimViewModel(validateZimViewModel)
    }
    viewModel.fileSelectListStates.value = FileSelectListState(emptyList())
  }

  @Nested
  inner class Books {
    @Test
    fun `emissions from data source are observed`() = flakyTest {
      runTest {
        val expectedList = listOf(bookOnDisk())
        testFlow(
          viewModel.fileSelectListStates.asFlow(),
          triggerAction = { booksOnDiskListItems.emit(expectedList) },
          assert = {
            skipItems(1)
            assertThat(awaitItem()).isEqualTo(FileSelectListState(expectedList))
          }
        )
      }
    }

    @Test
    fun `books found on filesystem are filtered by books already in db`() = flakyTest {
      runTest {
        val expectedBook = bookOnDisk(1L, libkiwixBook("1", nativeBook = BookTestWrapper("1")))
        val bookToRemove = bookOnDisk(1L, libkiwixBook("2", nativeBook = BookTestWrapper("2")))
        advanceUntilIdle()
        viewModel.requestFileSystemCheck.emit(Unit)
        advanceUntilIdle()
        books.emit(listOf(bookToRemove))
        advanceUntilIdle()
        booksOnFileSystem.emit(
          listOfNotNull(
            expectedBook.book.nativeBook,
            expectedBook.book.nativeBook,
            bookToRemove.book.nativeBook
          )
        )
        advanceUntilIdle()
        coVerify(timeout = 1000L) {
          libkiwixBookOnDisk.insert(listOfNotNull(expectedBook.book.nativeBook))
        }
      }
    }
  }

  @Nested
  inner class SideEffects {
    @Test
    fun `RequestNavigateTo offers OpenFileWithNavigation with selected books`() = flakyTest {
      runTest {
        val selectedBook = bookOnDisk().apply { isSelected = true }
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestNavigateTo(selectedBook)) },
          assert = { assertThat(awaitItem()).isEqualTo(OpenFileWithNavigation(selectedBook)) }
        )
      }
    }

    @Test
    fun `RequestMultiSelection offers StartMultiSelection and selects a book`() = flakyTest {
      runTest {
        val bookToSelect = bookOnDisk(databaseId = 0L)
        val unSelectedBook = bookOnDisk(databaseId = 1L)
        viewModel.fileSelectListStates.value =
          FileSelectListState(
            listOf(
              bookToSelect,
              unSelectedBook
            ),
            NORMAL
          )
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestMultiSelection(bookToSelect)) },
          assert = { assertThat(awaitItem()).isEqualTo(StartMultiSelection(viewModel.fileSelectActions)) }
        )
        viewModel.fileSelectListStates.test()
          .assertValue(
            FileSelectListState(
              listOf(bookToSelect.apply { isSelected = !isSelected }, unSelectedBook),
              MULTI
            )
          )
      }
    }

    @Test
    fun `RequestDeleteMultiSelection offers DeleteFiles with selected books`() = flakyTest {
      runTest {
        val selectedBook = bookOnDisk().apply { isSelected = true }
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestDeleteMultiSelection) },
          assert = {
            assertThat(awaitItem()).isEqualTo(
              DeleteFiles(
                listOf(selectedBook),
                alertDialogShower
              )
            )
          }
        )
      }
    }

    @Test
    fun `RequestShareMultiSelection offers ShareFiles with selected books`() = flakyTest {
      runTest {
        val selectedBook = bookOnDisk().apply { isSelected = true }
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestShareMultiSelection) },
          assert = { assertThat(awaitItem()).isEqualTo(ShareFiles(listOf(selectedBook))) }
        )
      }
    }

    @Test
    fun `RequestValidateZimFiles offers ValidateZIMFiles with selected books`() = flakyTest {
      runTest {
        val selectedBook = bookOnDisk().apply { isSelected = true }
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestValidateZimFiles) },
          assert = {
            assertThat(awaitItem())
              .isEqualTo(
                ValidateZIMFiles(
                  listOf(selectedBook),
                  alertDialogShower,
                  validateZimViewModel
                )
              )
          }
        )
      }
    }

    @Test
    fun `MultiModeFinished offers None`() = flakyTest {
      runTest {
        val selectedBook = bookOnDisk().apply { isSelected = true }
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk()), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(MultiModeFinished) },
          assert = { assertThat(awaitItem()).isEqualTo(None) }
        )
        viewModel.fileSelectListStates.test().assertValue(
          FileSelectListState(
            listOf(
              selectedBook.apply { isSelected = false },
              bookOnDisk()
            )
          )
        )
      }
    }

    @Test
    fun `RequestSelect offers None and inverts selection`() = flakyTest {
      runTest {
        val selectedBook = bookOnDisk(0L).apply { isSelected = true }
        viewModel.fileSelectListStates.value =
          FileSelectListState(listOf(selectedBook, bookOnDisk(1L)), NORMAL)
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RequestSelect(selectedBook)) },
          assert = { assertThat(awaitItem()).isEqualTo(None) }
        )
        viewModel.fileSelectListStates.test().assertValue(
          FileSelectListState(
            listOf(
              selectedBook.apply { isSelected = false },
              bookOnDisk(1L)
            )
          )
        )
      }
    }

    @Test
    fun `RestartActionMode offers StartMultiSelection`() = flakyTest {
      runTest {
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(RestartActionMode) },
          assert = { assertThat(awaitItem()).isEqualTo(StartMultiSelection(viewModel.fileSelectActions)) }
        )
      }
    }

    @Test
    fun `UserClickedDownloadBooksButton offers NavigateToDownloads`() = flakyTest {
      runTest {
        testFlow(
          flow = viewModel.sideEffects,
          triggerAction = { viewModel.fileSelectActions.emit(UserClickedDownloadBooksButton) },
          assert = { assertThat(awaitItem()).isEqualTo(NavigateToDownloads) }
        )
      }
    }
  }
}
