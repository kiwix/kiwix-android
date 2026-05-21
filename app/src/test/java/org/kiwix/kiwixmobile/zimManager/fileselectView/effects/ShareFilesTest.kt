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
import androidx.appcompat.app.AppCompatActivity
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.sharedFunctions.MainDispatcherRule

class ShareFilesTest {
  @RegisterExtension
  private val mainDispatcherRule = MainDispatcherRule()
  private val scope = TestScope(mainDispatcherRule.dispatcher)

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `invokeWith should navigate with selected file URIs`() = runTest {
    mockkObject(ActivityExtensions)
    val activity = mockk<AppCompatActivity>(relaxed = true)
    val uri1 = mockk<Uri>()
    val uri2 = mockk<Uri>()

    every { uri1.toString() } returns "content://file1"
    every { uri2.toString() } returns "content://file2"

    val zimReaderSource1 = mockk<ZimReaderSource>()
    val zimReaderSource2 = mockk<ZimReaderSource>()
    every { zimReaderSource1.getUri(activity) } returns uri1
    every { zimReaderSource2.getUri(activity) } returns uri2

    val book1 = mockk<BookOnDisk>()
    val book2 = mockk<BookOnDisk>()

    every { book1.zimReaderSource } returns zimReaderSource1
    every { book2.zimReaderSource } returns zimReaderSource2
    val routeSlot = slot<String>()

    every {
      ActivityExtensions.run {
        activity.navigate(capture(routeSlot), null)
      }
    } just runs

    val effect = ShareFiles(
      selectedBooks = listOf(book1, book2),
      viewModelScope = scope,
      ioDispatcher = mainDispatcherRule.dispatcher
    )

    effect.invokeWith(activity)

    advanceUntilIdle()
    assertThat(routeSlot.captured)
      .contains("content://file1")
      .contains("content://file2")
      .doesNotContain("null")
    verify(exactly = 1) {
      ActivityExtensions.run {
        activity.navigate(any(), null)
      }
    }
  }
}
