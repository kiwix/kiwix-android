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

package org.kiwix.kiwixmobile.core.main.reader.helper.intent

import android.content.Intent
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReaderIntentManagerTest {
  private val parser = mockk<PendingIntentParser>()

  private lateinit var manager: ReaderIntentManager

  @BeforeEach
  fun setup() {
    manager = ReaderIntentManager(parser)
  }

  @Test
  fun `storePendingIntent stores parsed action`() {
    val intent = Intent()
    val action = PendingIntentParser.ReaderIntentAction.OpenBookmarks
    every { parser.parse(intent) } returns action
    manager.storePendingIntent(intent)

    assertEquals(action, manager.consumePendingAction())
  }

  @Test
  fun `null intent stores None`() {
    manager.storePendingIntent(null)

    assertEquals(
      PendingIntentParser.ReaderIntentAction.None,
      manager.consumePendingAction()
    )
  }

  @Test
  fun `openZimFileFromPath stores OpenZim`() {
    manager.openZimFileFromPath(
      path = "/storage/book.zim",
      pageUrl = "Main_Page"
    )

    assertEquals(
      PendingIntentParser.ReaderIntentAction.OpenZim(
        "/storage/book.zim",
        "Main_Page"
      ),
      manager.consumePendingAction()
    )
  }

  @Test
  fun `consumePendingAction clears stored action`() {
    manager.openZimFileFromPath("path", "page")

    manager.consumePendingAction()

    assertEquals(
      PendingIntentParser.ReaderIntentAction.None,
      manager.consumePendingAction()
    )
  }

  @Test
  fun `storePendingIntent emits event`() = runTest {
    val intent = Intent()

    every { parser.parse(intent) } returns
      PendingIntentParser.ReaderIntentAction.OpenBookmarks

    manager.events.test {
      manager.storePendingIntent(intent)

      awaitItem()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `openZimFileFromPath emits event`() = runTest {
    manager.events.test {
      manager.openZimFileFromPath("path", "page")

      awaitItem()
      cancelAndIgnoreRemainingEvents()
    }
  }
}
