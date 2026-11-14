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

package org.kiwix.kiwixmobile.main

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.files.LegacyBooksMover

class LegacyBooksPromptTest {
  private val mover: LegacyBooksMover = mockk(relaxed = true)
  private val dialogShower: AlertDialogShower = mockk(relaxed = true)
  private lateinit var prompt: LegacyBooksPrompt

  @BeforeEach
  fun setUp() {
    clearAllMocks()
    coEvery { mover.offer() } returns null
    prompt = LegacyBooksPrompt(mockk(relaxed = true), mover, mockk(relaxed = true))
  }

  @Test
  fun `legacy books are offered a move and declining is remembered`() = runTest {
    coEvery { mover.offer() } returns LegacyBooksMover.Offer(listOf(mockk()), 2048)
    val dialog = slot<KiwixDialog>()
    val listeners = slot<Array<() -> Unit>>()
    every { dialogShower.show(capture(dialog), *anyVararg(), uri = any()) } answers {
      @Suppress("UNCHECKED_CAST")
      listeners.captured = (args[1] as Array<() -> Unit>)
    }

    prompt.show(dialogShower, this)
    assertThat(dialog.captured).isEqualTo(KiwixDialog.MoveLegacyBooks(1, "2 KB"))
    listeners.captured[1].invoke()
    advanceUntilIdle()
    coVerify { mover.decline() }
  }

  @Test
  fun `nothing to move shows nothing but points storage at Documents`() = runTest {
    prompt.show(dialogShower, this)
    coVerify { mover.useDocumentsIfNothingToMove() }
    verify(exactly = 0) { dialogShower.show(any(), *anyVararg(), uri = any()) }
  }

  @Test
  fun `prompts at most once per process`() = runTest {
    prompt.show(dialogShower, this)
    prompt.show(dialogShower, this)
    coVerify(exactly = 1) { mover.offer() }
  }
}
