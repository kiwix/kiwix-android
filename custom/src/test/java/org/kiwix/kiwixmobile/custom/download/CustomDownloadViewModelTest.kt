/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.custom.download

import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.tonyodev.fetch2.Error.NONE
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.DownloadRoomDao
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState
import org.kiwix.kiwixmobile.core.downloader.model.DownloadState.Failed
import org.kiwix.kiwixmobile.custom.download.Action.ClickedDownload
import org.kiwix.kiwixmobile.custom.download.Action.ClickedRetry
import org.kiwix.kiwixmobile.custom.download.Action.DatabaseEmission
import org.kiwix.kiwixmobile.custom.download.State.DownloadComplete
import org.kiwix.kiwixmobile.custom.download.State.DownloadFailed
import org.kiwix.kiwixmobile.custom.download.State.DownloadInProgress
import org.kiwix.kiwixmobile.custom.download.State.DownloadRequired
import org.kiwix.kiwixmobile.custom.download.effects.DownloadCustom
import org.kiwix.kiwixmobile.custom.download.effects.NavigateToCustomReader
import org.kiwix.kiwixmobile.custom.download.effects.SetPreferredStorageWithMostSpace
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.downloadItem

@ExtendWith(InstantExecutorExtension::class)
internal class CustomDownloadViewModelTest {
  private val downloadRoomDao: DownloadRoomDao = mockk()
  private val setPreferredStorageWithMostSpace: SetPreferredStorageWithMostSpace = mockk()
  private val downloadCustom: DownloadCustom = mockk()
  private val navigateToCustomReader: NavigateToCustomReader = mockk()

  private val downloads = MutableStateFlow<List<DownloadModel>>(emptyList())
  private lateinit var customDownloadViewModel: CustomDownloadViewModel

  @BeforeEach
  internal fun setUp() {
    clearAllMocks()
    every { downloadRoomDao.downloads() } returns downloads
    customDownloadViewModel = CustomDownloadViewModel(
      downloadRoomDao,
      setPreferredStorageWithMostSpace,
      downloadCustom,
      navigateToCustomReader
    )
  }

  @Test
  internal fun `effects emits SetPreferred on Subscribe`() = runTest {
    testFlow(
      flow = customDownloadViewModel.effects,
      triggerAction = {},
      assert = { assertThat(awaitItem()).isEqualTo(setPreferredStorageWithMostSpace) }
    )
  }

  @Test
  internal fun `initial State is DownloadRequired`() = runTest {
    testFlow(
      flow = customDownloadViewModel.state,
      triggerAction = {},
      assert = { assertThat(awaitItem()).isEqualTo(State.DownloadRequired) }
    )
  }

  @Nested
  inner class DownloadEmissions {
    @Test
    internal fun `Emission with data moves state from Required to InProgress`() = runTest {
      assertStateTransition(
        this,
        DownloadRequired,
        DatabaseEmission(listOf(downloadItem())),
        State.DownloadInProgress(listOf(downloadItem())),
        2
      )
    }

    @Test
    internal fun `Emission without data moves state from Required to Required`() = runTest {
      assertStateTransition(this, DownloadRequired, DatabaseEmission(listOf()), DownloadRequired)
    }

    @Test
    internal fun `Emission with data moves state from Failed to InProgress`() = runTest {
      assertStateTransition(
        this,
        DownloadFailed(DownloadState.Pending),
        DatabaseEmission(listOf(downloadItem())),
        State.DownloadInProgress(listOf(downloadItem())),
        2
      )
    }

    @Test
    internal fun `Emission without data moves state from Failed to Failed`() = runTest {
      assertStateTransition(
        this,
        DownloadFailed(DownloadState.Pending),
        DatabaseEmission(listOf()),
        DownloadFailed(DownloadState.Pending)
      )
    }

    @Test
    internal fun `Emission with data+failure moves state from InProgress to Failed`() = runTest {
      assertStateTransition(
        this,
        DownloadInProgress(listOf()),
        DatabaseEmission(listOf(downloadItem(state = Failed(NONE, null)))),
        DownloadFailed(Failed(NONE, null)),
        2
      )
    }

    @Test
    internal fun `Emission with data moves state from InProgress to InProgress`() = runTest {
      assertStateTransition(
        this,
        DownloadInProgress(listOf(downloadItem(downloadId = 1L))),
        DatabaseEmission(listOf(downloadItem(downloadId = 2L))),
        DownloadInProgress(listOf(downloadItem(downloadId = 2L))),
        2
      )
    }

    @Test
    internal fun `Emission without data moves state from InProgress to Complete`() = runTest {
      testFlow(
        flow = customDownloadViewModel.effects,
        triggerAction = {
          assertStateTransition(
            this,
            DownloadInProgress(listOf()),
            DatabaseEmission(listOf()),
            DownloadComplete,
            2
          )
        },
        assert = {
          assertThat(awaitItem()).isEqualTo(setPreferredStorageWithMostSpace)
          assertThat(awaitItem()).isEqualTo(navigateToCustomReader)
        }
      )
    }

    @Test
    internal fun `Any emission does not change state from Complete`() = runTest {
      assertStateTransition(
        this,
        DownloadComplete,
        DatabaseEmission(listOf(downloadItem())),
        DownloadComplete
      )
    }

    private suspend fun assertStateTransition(
      testScope: TestScope,
      initialState: State,
      action: DatabaseEmission,
      endState: State,
      awaitItemCount: Int = 1
    ) {
      customDownloadViewModel.getStateForTesting().value = initialState
      testScope.testFlow(
        flow = customDownloadViewModel.state,
        triggerAction = { customDownloadViewModel.actions.emit(action) },
        assert = {
          val items = (1..awaitItemCount).map { awaitItem() }
          assertThat(items.last()).isEqualTo(endState)
        }
      )
    }
  }

  @Test
  internal fun `clicking Retry triggers DownloadCustom`() = runTest {
    testFlow(
      flow = customDownloadViewModel.effects,
      triggerAction = { customDownloadViewModel.actions.emit(ClickedRetry) },
      assert = {
        assertThat(awaitItem()).isEqualTo(setPreferredStorageWithMostSpace)
        assertThat(awaitItem()).isEqualTo(downloadCustom)
      }
    )
  }

  @Test
  internal fun `clicking Download triggers DownloadCustom`() = runTest {
    testFlow(
      flow = customDownloadViewModel.effects,
      triggerAction = { customDownloadViewModel.actions.emit(ClickedDownload) },
      assert = {
        assertThat(awaitItem()).isEqualTo(setPreferredStorageWithMostSpace)
        assertThat(awaitItem()).isEqualTo(downloadCustom)
      }
    )
  }
}

suspend fun <T> TestScope.testFlow(
  flow: Flow<T>,
  triggerAction: suspend () -> Unit,
  assert: suspend TurbineTestContext<T>.() -> Unit
) {
  val job = launch {
    flow.test {
      triggerAction()
      assert()
      cancelAndIgnoreRemainingEvents()
    }
  }
  job.join()
}
