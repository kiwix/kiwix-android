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
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ShowFileSystemScanDialogTest {
  private val dialogShower: AlertDialogShower = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val activity: AppCompatActivity = mockk(relaxed = true)

  @RegisterExtension
  private val mainDispatcherRule = MainDispatcherRule()
  private lateinit var testScope: TestScope

  @Before
  fun setup() {
    testScope = TestScope(mainDispatcherRule.dispatcher)
  }

  @Test
  fun `invokeWith should show file system scan dialog`() {
    val positiveCallbackSlot = slot<() -> Unit>()
    val negativeCallbackSlot = slot<() -> Unit>()
    val dialogSlot = slot<KiwixDialog>()

    every {
      dialogShower.show(
        capture(dialogSlot),
        capture(positiveCallbackSlot),
        capture(negativeCallbackSlot)
      )
    } just Runs

    val sideEffect = ShowFileSystemScanDialog(
      dialogShower = dialogShower,
      coroutineScope = testScope,
      kiwixDataStore = kiwixDataStore,
      scanFileSystem = {}
    )

    sideEffect.invokeWith(activity)

    verify(exactly = 1) {
      dialogShower.show(any(), any(), any())
    }

    assert(dialogSlot.captured == KiwixDialog.YesNoDialog.FileSystemScan)
  }

  @Test
  fun `positive button should set dialog shown and scan file system`() = runTest {
    val positiveCallbackSlot = slot<() -> Unit>()
    val scanFileSystem: suspend () -> Unit = mockk()

    every {
      dialogShower.show(
        any(),
        capture(positiveCallbackSlot),
        any()
      )
    } just Runs

    coEvery {
      kiwixDataStore.setIsScanFileSystemDialogShown(true)
    } just Runs

    coEvery {
      scanFileSystem.invoke()
    } just Runs

    val sideEffect = ShowFileSystemScanDialog(
      dialogShower = dialogShower,
      coroutineScope = testScope,
      kiwixDataStore = kiwixDataStore,
      scanFileSystem = scanFileSystem
    )

    sideEffect.invokeWith(activity)

    positiveCallbackSlot.captured.invoke()

    testScope.advanceUntilIdle()

    coVerify(exactly = 1) {
      kiwixDataStore.setIsScanFileSystemDialogShown(true)
    }

    coVerify(exactly = 1) {
      scanFileSystem.invoke()
    }
  }

  @Test
  fun `negative button should only set dialog shown`() = runTest {
    val negativeCallbackSlot = slot<() -> Unit>()
    val scanFileSystem: suspend () -> Unit = mockk()

    every {
      dialogShower.show(
        any(),
        any(),
        capture(negativeCallbackSlot)
      )
    } just Runs

    coEvery {
      kiwixDataStore.setIsScanFileSystemDialogShown(true)
    } just Runs

    val sideEffect = ShowFileSystemScanDialog(
      dialogShower = dialogShower,
      coroutineScope = testScope,
      kiwixDataStore = kiwixDataStore,
      scanFileSystem = scanFileSystem
    )

    sideEffect.invokeWith(activity)

    negativeCallbackSlot.captured.invoke()

    testScope.advanceUntilIdle()

    coVerify(exactly = 1) {
      kiwixDataStore.setIsScanFileSystemDialogShown(true)
    }

    coVerify(exactly = 0) {
      scanFileSystem.invoke()
    }
  }
}
