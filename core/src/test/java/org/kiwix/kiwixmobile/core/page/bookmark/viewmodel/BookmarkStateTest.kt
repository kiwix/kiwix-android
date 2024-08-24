/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.page.bookmark.viewmodel

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.page.bookmark
import org.kiwix.kiwixmobile.core.page.bookmarkState
import org.kiwix.kiwixmobile.core.page.libkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import java.util.UUID

internal class BookmarkStateTest {
  @Test
  internal fun `copyNewItems should set new items to pageItems`() {
    val zimReaderSource: ZimReaderSource = mockk()
    val databaseId = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE
    every { zimReaderSource.toDatabase() } returns ""
    assertThat(
      bookmarkState(emptyList()).copy(
        listOf(
          libkiwixBookmarkItem(
            databaseId,
            zimReaderSource = zimReaderSource
          )
        )
      ).pageItems
    ).isEqualTo(
      listOf(libkiwixBookmarkItem(databaseId, zimReaderSource = zimReaderSource))
    )
  }
}
