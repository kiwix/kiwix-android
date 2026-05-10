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

package org.kiwix.kiwixmobile.zimManager.libraryView

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.zimManager.Fat32Checker

class AvailableSpaceCalculatorTest {
  private val downloadRoomDao: DownloadRoomDao = mockk()
  private val storageCalculator: StorageCalculator = mockk()
  private lateinit var availableSpaceCalculator: AvailableSpaceCalculator

  @Before
  fun setUp() {
    availableSpaceCalculator = AvailableSpaceCalculator(downloadRoomDao, storageCalculator)
  }

  @Test
  fun `hasAvailableSpaceFor calls successAction when enough space is available`() = runTest {
    val bookSize = 1000L
    val availableBytes = 2000L
    val bytesToBeDownloaded = 500L

    val book = LibkiwixBook().apply {
      size = bookSize.toString()
      id = "test_id"
    }
    val bookItem = LibraryListItem.BookItem(book, Fat32Checker.FileSystemState.CanWrite4GbFile)

    val downloadModel: DownloadModel = mockk {
      every { bytesRemaining } returns bytesToBeDownloaded
    }

    every { downloadRoomDao.allDownloads() } returns flowOf(listOf(downloadModel))
    coEvery { storageCalculator.availableBytes() } returns availableBytes

    var successCalled = false
    var failureCalled = false

    availableSpaceCalculator.hasAvailableSpaceFor(
      bookItem,
      successAction = { successCalled = true },
      failureAction = { failureCalled = true }
    )

    assertThat(successCalled).isTrue
    assertThat(failureCalled).isFalse
  }

  @Test
  fun `hasAvailableSpaceFor calls failureAction when not enough space is available`() = runTest {
    val bookSize = 2000L
    val availableBytes = 2000L
    val bytesToBeDownloaded = 500L
    // trueAvailableBytes = 2000 - 500 = 1500
    // bookSize (2000) > trueAvailableBytes (1500) -> failure

    val book = LibkiwixBook().apply {
      size = bookSize.toString()
      id = "test_id"
    }
    val bookItem = LibraryListItem.BookItem(book, Fat32Checker.FileSystemState.CanWrite4GbFile)

    val downloadModel: DownloadModel = mockk {
      every { bytesRemaining } returns bytesToBeDownloaded
    }

    every { downloadRoomDao.allDownloads() } returns flowOf(listOf(downloadModel))
    coEvery { storageCalculator.availableBytes() } returns availableBytes

    var successCalled = false
    var failureMessage: String? = null

    availableSpaceCalculator.hasAvailableSpaceFor(
      bookItem,
      successAction = { successCalled = true },
      failureAction = { failureMessage = it }
    )

    assertThat(successCalled).isFalse
    assertThat(failureMessage).isEqualTo("1.5 KB") // 1500 bytes is 1.5 KB in human readable
  }

  @Test
  fun `hasAvailableSpaceForBook returns true when enough space`() = runTest {
    val bookSize = 1000L
    val availableBytes = 2000L

    val book = LibkiwixBook().apply {
      size = bookSize.toString()
    }

    coEvery { storageCalculator.availableBytes() } returns availableBytes

    val result = availableSpaceCalculator.hasAvailableSpaceForBook(book)

    assertThat(result).isTrue
  }

  @Test
  fun `hasAvailableSpaceForBook returns false when not enough space`() = runTest {
    val bookSize = 3000L
    val availableBytes = 2000L

    val book = LibkiwixBook().apply {
      size = bookSize.toString()
    }

    coEvery { storageCalculator.availableBytes() } returns availableBytes

    val result = availableSpaceCalculator.hasAvailableSpaceForBook(book)

    assertThat(result).isFalse
  }

  @Test
  fun `hasAvailableSpaceFor handles multiple ongoing downloads correctly`() = runTest {
    val bookSize = 1000L
    val availableBytes = 5000L
    // Sum of remaining bytes: 1500 + 2000 = 3500
    // trueAvailableBytes = 5000 - 3500 = 1500
    // bookSize (1000) < 1500 -> success

    val book = LibkiwixBook().apply {
      size = bookSize.toString()
      id = "test_id"
    }
    val bookItem = LibraryListItem.BookItem(book, Fat32Checker.FileSystemState.CanWrite4GbFile)

    val download1: DownloadModel = mockk { every { bytesRemaining } returns 1500L }
    val download2: DownloadModel = mockk { every { bytesRemaining } returns 2000L }

    every { downloadRoomDao.allDownloads() } returns flowOf(listOf(download1, download2))
    coEvery { storageCalculator.availableBytes() } returns availableBytes

    var successCalled = false
    availableSpaceCalculator.hasAvailableSpaceFor(bookItem, { successCalled = true }, {})

    assertThat(successCalled).isTrue
  }

  @Test
  fun `hasAvailableSpaceFor calls failureAction when book size is exactly equal to available space`() =
    runTest {
      val expectedSize = 2000L
      val book = LibkiwixBook().apply {
        size = expectedSize.toString()
        id = "test_id"
      }
      val bookItem = LibraryListItem.BookItem(book, Fat32Checker.FileSystemState.CanWrite4GbFile)

      every { downloadRoomDao.allDownloads() } returns flowOf(emptyList())
      coEvery { storageCalculator.availableBytes() } returns expectedSize // Exactly equal

      var failureCalled = false
      availableSpaceCalculator.hasAvailableSpaceFor(bookItem, {}, { failureCalled = true })

      assertThat(failureCalled).isTrue
    }
}
