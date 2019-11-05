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

import com.jraska.livedata.test
import com.tonyodev.fetch2.Error.NONE
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.processors.PublishProcessor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.FetchDownloadDao
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
import org.kiwix.kiwixmobile.custom.download.effects.FinishAndStartMain
import org.kiwix.kiwixmobile.custom.download.effects.SetPreferredStorageWithMostSpace
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.downloadItem

@ExtendWith(InstantExecutorExtension::class)
internal class CustomDownloadViewModelTest {
  private val fetchDownloadDao: FetchDownloadDao = mockk()
  private val setPreferredStorageWithMostSpace: SetPreferredStorageWithMostSpace = mockk()
  private val downloadCustom: DownloadCustom = mockk()
  private val finishAndStartMain: FinishAndStartMain = mockk()

  private val downloads: PublishProcessor<List<DownloadModel>> = PublishProcessor.create()
  private lateinit var customDownloadViewModel: CustomDownloadViewModel

  @BeforeEach
  internal fun setUp() {
    clearAllMocks()
    every { fetchDownloadDao.downloads() } returns downloads
    customDownloadViewModel = CustomDownloadViewModel(
      fetchDownloadDao,
      setPreferredStorageWithMostSpace,
      downloadCustom,
      finishAndStartMain
    )
  }

  @Test
  internal fun `effects emits SetPreferred on Subscribe`() {
    customDownloadViewModel.effects.test().assertValue(setPreferredStorageWithMostSpace)
  }

  @Test
  internal fun `initial State is DownloadRequired`() {
    customDownloadViewModel.state.test().assertValue(State.DownloadRequired)
  }

  @Nested
  inner class DownloadEmissions {
    @Test
    internal fun `Emission with data moves state from Required to InProgress`() {
      assertStateTransition(
        DownloadRequired,
        DatabaseEmission(listOf(downloadItem())),
        State.DownloadInProgress(listOf(downloadItem()))
      )
    }

    @Test
    internal fun `Emission without data moves state from Required to Required`() {
      assertStateTransition(DownloadRequired, DatabaseEmission(listOf()), DownloadRequired)
    }

    @Test
    internal fun `Emission with data moves state from Failed to InProgress`() {
      assertStateTransition(
        DownloadFailed(DownloadState.Pending),
        DatabaseEmission(listOf(downloadItem())),
        State.DownloadInProgress(listOf(downloadItem()))
      )
    }

    @Test
    internal fun `Emission without data moves state from Failed to Failed`() {
      assertStateTransition(
        DownloadFailed(DownloadState.Pending),
        DatabaseEmission(listOf()),
        DownloadFailed(DownloadState.Pending)
      )
    }

    @Test
    internal fun `Emission with data+failure moves state from InProgress to Failed`() {
      assertStateTransition(
        DownloadInProgress(listOf()),
        DatabaseEmission(listOf(downloadItem(state = Failed(NONE)))),
        DownloadFailed(Failed(NONE))
      )
    }

    @Test
    internal fun `Emission with data moves state from InProgress to InProgress`() {
      assertStateTransition(
        DownloadInProgress(listOf(downloadItem(downloadId = 1L))),
        DatabaseEmission(listOf(downloadItem(downloadId = 2L))),
        DownloadInProgress(listOf(downloadItem(downloadId = 2L)))
      )
    }

    @Test
    internal fun `Emission without data moves state from InProgress to Complete`() {
      val sideEffects = customDownloadViewModel.effects.test()
      assertStateTransition(
        DownloadInProgress(listOf()),
        DatabaseEmission(listOf()),
        DownloadComplete
      )
      sideEffects.assertValues(setPreferredStorageWithMostSpace, finishAndStartMain)
    }

    @Test
    internal fun `Any emission does not change state from Complete`() {
      assertStateTransition(
        DownloadComplete,
        DatabaseEmission(listOf(downloadItem())),
        DownloadComplete
      )
    }

    private fun assertStateTransition(
      initialState: State,
      action: DatabaseEmission,
      endState: State
    ) {
      customDownloadViewModel.state.value = initialState
      customDownloadViewModel.actions.offer(action)
      customDownloadViewModel.state.test().assertValue(endState)
    }
  }

  @Test
  internal fun `clicking Retry triggers DownloadCustom`() {
    val sideEffects = customDownloadViewModel.effects.test()
    customDownloadViewModel.actions.offer(ClickedRetry)
    sideEffects.assertValues(setPreferredStorageWithMostSpace, downloadCustom)
  }

  @Test
  internal fun `clicking Download triggers DownloadCustom`() {
    val sideEffects = customDownloadViewModel.effects.test()
    customDownloadViewModel.actions.offer(ClickedDownload)
    sideEffects.assertValues(setPreferredStorageWithMostSpace, downloadCustom)
  }
}
