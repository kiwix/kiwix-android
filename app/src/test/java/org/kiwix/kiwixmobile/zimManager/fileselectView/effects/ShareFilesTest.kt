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

package org.kiwix.kiwixmobile.zimManager.fileselectView.effects

import android.net.Uri
import androidx.navigation.NavOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.ui.KiwixDestination
import org.kiwix.kiwixmobile.ui.toUriParam
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ShareFilesTest {
  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()

  private lateinit var activity: CoreMainActivity
  private val uri = mockk<Uri>()

  @BeforeEach
  fun setup() {
    activity = mockk(relaxed = true)
    mockkStatic(Uri::class)
    every { Uri.encode(any()) } answers { firstArg() }
    every { uri.toString() } returns "test-uri"
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun invokeWith_validUris_navigatesWithExpectedRoute() = runTest {
    val uri1 = mockk<Uri>()
    val uri2 = mockk<Uri>()

    every { uri1.toString() } returns "uri1"
    every { uri2.toString() } returns "uri2"

    val books = listOf(createDummyBook(uri1), createDummyBook(uri2))

    ShareFiles(
      selectedBooks = books,
      viewModelScope = this,
      ioDispatcher = mainDispatcherRule.dispatcher
    ).invokeWith(activity)

    advanceUntilIdle()

    val expectedRoute =
      KiwixDestination.LocalFileTransfer.createRoute(listOf(uri1, uri2).toUriParam())

    verify {
      activity.navigate(expectedRoute, any<NavOptions>())
    }
  }

  @Test
  fun invokeWith_nullUri_isFilteredFromRoute() = runTest {
    val validUri = mockk<Uri>()

    every { validUri.toString() } returns "valid-uri"

    val books = listOf(createDummyBook(validUri), createDummyBook(null))

    ShareFiles(
      selectedBooks = books,
      viewModelScope = this,
      ioDispatcher = mainDispatcherRule.dispatcher
    ).invokeWith(activity)

    advanceUntilIdle()

    val expectedRoute =
      KiwixDestination.LocalFileTransfer.createRoute(listOf(validUri).toUriParam())

    verify {
      activity.navigate(expectedRoute, any<NavOptions>())
    }
  }

  private fun createDummyBook(uri: Uri?) = BookOnDisk(
    book = LibkiwixBook(_id = "book-id"),
    zimReaderSource = mockk {
      every { getUri(any()) } returns uri
    }
  )
}
