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

package org.kiwix.kiwixmobile.core.main.reader.helper

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.page.history.models.HistoryListItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import java.util.Locale

class ReaderHistoryManagerTest {
  private val context = mockk<Context>(relaxed = true)
  private val repository = mockk<MainRepositoryActions>(relaxed = true)

  private lateinit var readerHistoryManager: ReaderHistoryManager

  @Before
  fun setup() {
    val resources = mockk<Resources>()
    val configuration = mockk<Configuration>()
    val locales = mockk<LocaleList>()
    every { context.resources } returns resources
    every { resources.configuration } returns configuration
    every { configuration.locales } returns locales
    every { locales[0] } returns Locale.US

    readerHistoryManager = ReaderHistoryManager(
      context = context,
      mainRepositoryActions = repository
    )
  }

  @Test
  fun `saveHistory should do nothing when url is null`() = runTest {
    readerHistoryManager.saveHistory(
      url = null,
      title = "Title",
      zimFileReader = mockk()
    )

    coVerify(exactly = 0) {
      repository.saveHistory(any())
    }
  }

  @Test
  fun `saveHistory should do nothing when title is null`() = runTest {
    readerHistoryManager.saveHistory(
      url = "article",
      title = null,
      zimFileReader = mockk()
    )

    coVerify(exactly = 0) {
      repository.saveHistory(any())
    }
  }

  @Test
  fun `saveHistory should do nothing when zimFileReader is null`() = runTest {
    readerHistoryManager.saveHistory(
      url = "article",
      title = "Title",
      zimFileReader = null
    )

    coVerify(exactly = 0) {
      repository.saveHistory(any())
    }
  }

  @Test
  fun `saveHistory should save history when all parameters are provided`() = runTest {
    val reader = mockk<ZimFileReader>().apply {
      every { id } returns ""
      every { name } returns "DemoZim"
      every { favicon } returns ""
      every { zimReaderSource } returns mockk()
    }

    val slot = slot<HistoryListItem.HistoryItem>()

    coEvery { repository.saveHistory(capture(slot)) } returns Unit

    readerHistoryManager.saveHistory(
      url = "article",
      title = "My Title",
      zimFileReader = reader
    )

    coVerify(exactly = 1) {
      repository.saveHistory(any())
    }

    val history = slot.captured

    assertEquals("article", history.url)
    assertEquals("My Title", history.title)
    assertEquals(reader.zimReaderSource, history.zimReaderSource)
    assertEquals(reader.favicon, history.favicon)

    assertTrue(history.timeStamp > 0)
    assertFalse(history.dateString.isBlank())
  }
}
