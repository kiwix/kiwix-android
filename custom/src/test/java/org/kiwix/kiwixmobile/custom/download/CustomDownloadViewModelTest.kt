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

import com.tonyodev.fetch2.Error.NONE
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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
    customDownloadViewModel.effects.test(this)
      .assertValues(mutableListOf(setPreferredStorageWithMostSpace))
      .finish()
  }

  @Test
  internal fun `initial State is DownloadRequired`() = runTest {
    customDownloadViewModel.state.test(this)
      .assertValues(mutableListOf(State.DownloadRequired))
      .finish()
  }

  @Nested
  inner class DownloadEmissions {
    @Test
    internal fun `Emission with data moves state from Required to InProgress`() = runTest {
      assertStateTransition(
        this,
        2,
        DownloadRequired,
        DatabaseEmission(listOf(downloadItem())),
        State.DownloadInProgress(listOf(downloadItem()))
      )
    }

    @Test
    internal fun `Emission without data moves state from Required to Required`() = runTest {
      assertStateTransition(this, 1, DownloadRequired, DatabaseEmission(listOf()), DownloadRequired)
    }

    @Test
    internal fun `Emission with data moves state from Failed to InProgress`() = runTest {
      assertStateTransition(
        this,
        2,
        DownloadFailed(DownloadState.Pending),
        DatabaseEmission(listOf(downloadItem())),
        State.DownloadInProgress(listOf(downloadItem()))
      )
    }

    @Test
    internal fun `Emission without data moves state from Failed to Failed`() = runTest {
      assertStateTransition(
        this,
        1,
        DownloadFailed(DownloadState.Pending),
        DatabaseEmission(listOf()),
        DownloadFailed(DownloadState.Pending)
      )
    }

    @Test
    internal fun `Emission with data+failure moves state from InProgress to Failed`() = runTest {
      assertStateTransition(
        this,
        2,
        DownloadInProgress(listOf()),
        DatabaseEmission(listOf(downloadItem(state = Failed(NONE, null)))),
        DownloadFailed(Failed(NONE, null))
      )
    }

    @Test
    internal fun `Emission with data moves state from InProgress to InProgress`() = runTest {
      assertStateTransition(
        this,
        1,
        DownloadInProgress(listOf(downloadItem(downloadId = 1L))),
        DatabaseEmission(listOf(downloadItem(downloadId = 2L))),
        DownloadInProgress(listOf(downloadItem(downloadId = 2L)))
      )
    }

    @Disabled("TODO fix in upcoming issue when properly migrated to coroutines")
    internal fun `Emission without data moves state from InProgress to Complete`() = runTest {
      val sideEffects = customDownloadViewModel.effects.test(this)
      assertStateTransition(
        this,
        1,
        DownloadInProgress(listOf()),
        DatabaseEmission(listOf()),
        DownloadComplete
      )
      sideEffects.assertValues(
        mutableListOf(
          setPreferredStorageWithMostSpace,
          navigateToCustomReader
        )
      ).finish()
    }

    @Test
    internal fun `Any emission does not change state from Complete`() = runTest {
      assertStateTransition(
        this,
        1,
        DownloadComplete,
        DatabaseEmission(listOf(downloadItem())),
        DownloadComplete
      )
    }

    private fun assertStateTransition(
      testScope: TestScope,
      flowCount: Int,
      initialState: State,
      action: DatabaseEmission,
      endState: State
    ) {
      customDownloadViewModel.getStateForTesting().value = initialState
      testScope.launch {
        customDownloadViewModel.actions.emit(action)
        customDownloadViewModel.state.test(testScope, flowCount)
          .assertLastValues(mutableListOf(endState)).finish()
      }
    }
  }

  @Disabled("TODO fix in upcoming issue when properly migrated to coroutines")
  internal fun `clicking Retry triggers DownloadCustom`() = runTest {
    val sideEffects = customDownloadViewModel.effects.test(this)
    customDownloadViewModel.actions.emit(ClickedRetry)
    sideEffects.assertValues(mutableListOf(setPreferredStorageWithMostSpace, downloadCustom))
      .finish()
  }

  @Disabled("TODO fix in upcoming issue when properly migrated to coroutines")
  internal fun `clicking Download triggers DownloadCustom`() = runTest {
    val sideEffects = customDownloadViewModel.effects.test(this)
    customDownloadViewModel.actions.emit(ClickedDownload)
    sideEffects.assertValues(mutableListOf(setPreferredStorageWithMostSpace, downloadCustom))
      .finish()
  }
}

fun <T> Flow<T>.test(scope: TestScope, itemCountsToEmitInFlow: Int = 1): TestObserver<T> =
  TestObserver(scope, this, itemCountsToEmitInFlow).also { it.startCollecting() }

class TestObserver<T>(
  private val scope: TestScope,
  private val flow: Flow<T>,
  private val itemCountsToEmitInFlow: Int
) {
  private val values = mutableListOf<T>()
  private val completionChannel = Channel<Unit>()
  private var job: Job? = null

  fun startCollecting() {
    job = scope.launch {
      flow.collect {
        print("RECIVING $it")
        values.add(it)
        completionChannel.trySend(Unit)
      }
    }
  }

  private suspend fun awaitCompletion() {
    repeat(itemCountsToEmitInFlow) {
      completionChannel.receive()
    }
  }

  suspend fun assertValues(listValues: MutableList<T>): TestObserver<T> {
    awaitCompletion()
    assertThat(listValues).containsExactlyElementsOf(values)
    return this
  }

  suspend fun assertLastValues(listValues: MutableList<T>): TestObserver<T> {
    awaitCompletion()
    assertThat(listValues).containsExactlyElementsOf(mutableListOf(values.last()))
    return this
  }

  suspend fun finish() {
    job?.cancelAndJoin()
  }
}
