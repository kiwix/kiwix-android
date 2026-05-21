package org.kiwix.kiwixmobile.nav.destination.library.local

import android.app.Application
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem
import org.kiwix.kiwixmobile.zimManager.fileselectView.FileSelectListState
import org.kiwix.sharedFunctions.InstantExecutorExtension

@ExperimentalCoroutinesApi
@ExtendWith(InstantExecutorExtension::class)
class LocalLibraryViewModelTest {
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk = mockk(relaxed = true)
  private val storageObserver: StorageObserver = mockk(relaxed = true)
  private val dataSource: DataSource = mockk(relaxed = true)
  private val application: Application = mockk(relaxed = true)

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var viewModel: LocalLibraryViewModel

  @BeforeEach
  fun setUp() {
    Dispatchers.setMain(testDispatcher)

    every { dataSource.booksOnDiskAsListItems() } returns flowOf(emptyList())
    every { libkiwixBookOnDisk.books() } returns flowOf(emptyList())
    coEvery { storageObserver.getBooksOnFileSystem(any()) } returns flowOf(emptyList())

    viewModel = LocalLibraryViewModel(
      libkiwixBookOnDisk,
      storageObserver,
      dataSource,
      application
    )
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state subscribes to datasource`() = runTest {
    advanceUntilIdle()
    assertEquals(FileSelectListState(emptyList()), viewModel.fileSelectListStates.value)
  }

  @Test
  fun `file system check scans books`() = runTest {
    every { libkiwixBookOnDisk.books() } returns flowOf(emptyList())
    coEvery { storageObserver.getBooksOnFileSystem(any()) } returns flowOf(emptyList())

    viewModel.requestFileSystemCheck.emit(Unit)
    advanceUntilIdle()

    coVerify { storageObserver.getBooksOnFileSystem(any()) }
  }

  @Test
  fun `state updates on datasource change`() = runTest {
    val bookItem = mockk<BooksOnDiskListItem>(relaxed = true)
    every { dataSource.booksOnDiskAsListItems() } returns flowOf(listOf(bookItem))

    viewModel = LocalLibraryViewModel(
      libkiwixBookOnDisk,
      storageObserver,
      dataSource,
      application
    )
    advanceUntilIdle()

    val state = viewModel.fileSelectListStates.value
    assertEquals(1, state?.bookOnDiskListItems?.size)
    assertEquals(bookItem, state?.bookOnDiskListItems?.first())
  }
}
