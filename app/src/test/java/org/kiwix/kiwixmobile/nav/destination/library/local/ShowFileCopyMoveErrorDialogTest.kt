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

package org.kiwix.kiwixmobile.nav.destination.library.local

import androidx.appcompat.app.AppCompatActivity
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ShowFileCopyMoveErrorDialogTest {
  private val dialogShower: AlertDialogShower = mockk()
  private val activity: AppCompatActivity = mockk(relaxed = true)

  private lateinit var testScope: TestScope

  @RegisterExtension
  private val mainDispatcherRule = MainDispatcherRule()

  @Before
  fun setup() {
    testScope = TestScope(mainDispatcherRule.dispatcher)
  }

  @Test
  fun `invokeWith should show file copy move error dialog`() {
    val errorMessage = "Failed to copy file"

    val dialogSlot = slot<KiwixDialog>()
    val callbackSlot = slot<() -> Unit>()

    every {
      dialogShower.show(
        capture(dialogSlot),
        capture(callbackSlot)
      )
    } just Runs

    val sideEffect = ShowFileCopyMoveErrorDialog(
      dialogShower = dialogShower,
      errorMessage = errorMessage,
      coroutineScope = testScope,
      callBack = {}
    )

    sideEffect.invokeWith(activity)

    verify(exactly = 1) {
      dialogShower.show(any(), any())
    }

    val dialog = dialogSlot.captured
    assert(dialog is KiwixDialog.FileCopyMoveError)
    val fileCopyMoveErrorDialog = dialog as KiwixDialog.FileCopyMoveError
    assert(fileCopyMoveErrorDialog.args[0] == errorMessage)
  }

  @Test
  fun `invokeWith should execute callback when dialog action is clicked`() = runTest {
    val callback: suspend () -> Unit = mockk()

    every { dialogShower.show(any(), any()) } just Runs
    coEvery { callback.invoke() } just Runs

    val callbackSlot: CapturingSlot<() -> Unit> = slot()

    every {
      dialogShower.show(
        any(),
        capture(callbackSlot)
      )
    } just Runs

    val sideEffect = ShowFileCopyMoveErrorDialog(
      dialogShower = dialogShower,
      errorMessage = "Error",
      coroutineScope = testScope,
      callBack = callback
    )

    sideEffect.invokeWith(activity)

    callbackSlot.captured.invoke()

    testScope.advanceUntilIdle()

    coVerify(exactly = 1) {
      callback.invoke()
    }
  }
}
