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

package org.kiwix.kiwixmobile.language.viewmodel

import android.app.Application
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.language.composables.LanguageListItem
import org.kiwix.kiwixmobile.language.helper.ObserveLanguages
import org.kiwix.kiwixmobile.language.viewmodel.State.Loading
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class LanguageViewModelTest {
  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()
  private val application: Application = mockk(relaxed = true)
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val observeLanguages: ObserveLanguages = mockk()
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()
  private val networkStates = MutableStateFlow(NetworkState.NOT_CONNECTED)
  private lateinit var languageViewModel: LanguageViewModel

  private fun createViewModel() {
    languageViewModel =
      LanguageViewModel(
        application,
        kiwixDataStore,
        observeLanguages,
        connectivityBroadcastReceiver
      )
  }

  private fun createLanguage(
    code: String = "eng",
    active: Boolean = true,
    occurrences: Int = 10,
    id: Long = 1
  ): Language {
    return Language(
      languageCode = code,
      active = active,
      occurrencesOfLanguage = occurrences,
      id = id
    )
  }

  @BeforeEach
  fun init() {
    every { connectivityBroadcastReceiver.action } returns "test"
    every { connectivityBroadcastReceiver.networkStates } returns networkStates
    every { kiwixDataStore.prefLanguage } returns MutableStateFlow("")
    every { kiwixDataStore.selectedOnlineContentLanguage } returns MutableStateFlow("eng")
  }

  @Test
  fun `unregisters broadcastReceiver in onCleared`() {
    coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
    createViewModel()
    every { application.unregisterReceiver(any()) } returns mockk()
    languageViewModel.onClearedExposed()
    verify {
      application.unregisterReceiver(connectivityBroadcastReceiver)
    }
  }

  @Nested
  inner class Init {
    @Test
    fun whenObserveLanguagesReturnsSuccess_returnsContent() = runTest {
      val english = createLanguage()
      coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(listOf(english))

      createViewModel()
      advanceUntilIdle()

      val expected = State.Content(listOf(english))
      assertEquals(expected, languageViewModel.state.value)
    }

    @Test
    fun whenObserveLanguagesReturnsError_returnsError() = runTest {
      coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Error("No network connection")

      createViewModel()
      advanceUntilIdle()

      assertEquals(
        State.Error("No network connection"),
        languageViewModel.state.value
      )
    }
  }

  @Nested
  inner class ObserveActions {
    @Nested
    inner class ActionError {
      @Test
      fun whenErrorAction_returnsErrorMessage() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()

        languageViewModel.actions.emit(
          Action.Error("No internet connection")
        )

        advanceUntilIdle()

        assertEquals(
          State.Error("No internet connection"),
          languageViewModel.state.value
        )
      }
    }

    @Nested
    inner class ActionUpdateLanguages {
      @Test
      fun whenStateLoading_returnsContent() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()
        languageViewModel.state.value = Loading

        val english = createLanguage()

        languageViewModel.actions.emit(
          Action.UpdateLanguages(
            listOf(english)
          )
        )

        advanceUntilIdle()

        assertEquals(
          State.Content(
            listOf(english)
          ),
          languageViewModel.state.value
        )
      }

      @Test
      fun whenStateNotLoading_returnsCurrentState() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()
        languageViewModel.state.value = Loading

        languageViewModel.state.value =
          State.Error("Error")

        languageViewModel.actions.emit(
          Action.UpdateLanguages(
            listOf(createLanguage())
          )
        )

        advanceUntilIdle()

        assertEquals(
          State.Error("Error"),
          languageViewModel.state.value
        )
      }
    }

    @Nested
    inner class ActionFilter {
      @Test
      fun whenStateNotContent_returnsCurrentState() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()
        languageViewModel.state.value = Loading

        languageViewModel.actions.emit(
          Action.Filter("eng")
        )

        advanceUntilIdle()

        assertEquals(
          Loading,
          languageViewModel.state.value
        )
      }

      @Test
      fun whenStateContent_returnsFilteredContent() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()

        val english =
          createLanguage(
            code = "eng"
          )

        val french =
          createLanguage(
            code = "fr",
            active = false,
            id = 2
          )

        languageViewModel.state.value = State.Content(listOf(english, french))

        languageViewModel.actions.emit(Action.Filter("eng"))

        advanceUntilIdle()

        val content = languageViewModel.state.value as State.Content

        assertEquals(
          "eng",
          content.filter
        )

        assertEquals(
          2,
          content.items.size
        )

        assertEquals(
          2,
          content.viewItems.size
        )
      }
    }

    @Nested
    inner class ActionSelect {
      @Test
      fun whenStateNotContent_returnsCurrentState() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()
        languageViewModel.state.value = Loading

        languageViewModel.actions.emit(
          Action.Select(
            LanguageListItem.LanguageItem(
              createLanguage()
            )
          )
        )

        advanceUntilIdle()

        assertEquals(
          Loading,
          languageViewModel.state.value
        )
      }

      @Test
      fun whenStateContent_updatesSelectedLanguage() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()
        val english =
          createLanguage(
            code = "eng",
            active = true
          )

        val french =
          createLanguage(
            code = "fr",
            active = false,
            id = 2
          )

        languageViewModel.state.value =
          State.Content(listOf(english, french))

        languageViewModel.actions.emit(
          Action.Select(LanguageListItem.LanguageItem(french))
        )

        advanceUntilIdle()

        val content =
          languageViewModel.state.value as State.Content

        val updatedEnglish =
          content.items.first {
            it.languageCode == english.languageCode
          }

        val updatedFrench =
          content.items.first {
            it.languageCode == french.languageCode
          }

        assertTrue(
          updatedEnglish.active
        )

        assertTrue(
          updatedFrench.active
        )
      }
    }

    @Nested
    inner class ActionClearAll {
      @Test
      fun whenStateNotContent_returnsCurrentState() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()
        languageViewModel.state.value = Loading

        languageViewModel.actions.emit(Action.ClearAll)

        advanceUntilIdle()

        assertEquals(
          Loading,
          languageViewModel.state.value
        )
      }

      @Test
      fun whenStateContent_clearsAllSelections() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()
        val english = createLanguage(code = "eng", active = true)
        val french = createLanguage(code = "fr", active = true, id = 2)

        languageViewModel.state.value = State.Content(listOf(english, french))

        languageViewModel.actions.emit(Action.ClearAll)

        advanceUntilIdle()

        val content = languageViewModel.state.value as State.Content
        assertTrue(content.items.none { it.active })
      }
    }

    @Nested
    inner class ActionSelectAll {
      @Test
      fun whenStateNotContent_returnsCurrentState() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()
        languageViewModel.state.value = Loading

        languageViewModel.actions.emit(Action.SelectAll)

        advanceUntilIdle()

        assertEquals(
          Loading,
          languageViewModel.state.value
        )
      }

      @Test
      fun whenStateContent_selectAllSelections() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()
        val english = createLanguage(code = "eng", active = false)
        val french = createLanguage(code = "fr", active = false, id = 2)

        languageViewModel.state.value = State.Content(listOf(english, french))

        languageViewModel.actions.emit(Action.SelectAll)

        advanceUntilIdle()

        val content = languageViewModel.state.value as State.Content
        assertTrue(content.items.all { it.active })
      }
    }

    @Nested
    inner class ActionCancel {
      @Test
      fun whenStateNotContent_returnsCurrentState() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()
        languageViewModel.state.value = Loading

        languageViewModel.actions.emit(Action.Cancel)

        advanceUntilIdle()

        assertEquals(
          Loading,
          languageViewModel.state.value
        )
      }

      @Test
      fun whenStateContent_emitsCancelSideEffect() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()
        languageViewModel.state.value = State.Content(listOf(createLanguage()))

        var sideEffect: SideEffect<*>? = null
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
          languageViewModel.effects.collect {
            sideEffect = it
          }
        }

        languageViewModel.actions.emit(Action.Cancel)
        advanceUntilIdle()

        assertThat(sideEffect).isNotNull
        collectJob.cancel()
      }
    }

    @Nested
    inner class ActionSave {
      @Test
      fun whenStateNotContent_returnsCurrentState() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()
        languageViewModel.state.value = Loading

        languageViewModel.actions.emit(Action.Save)

        advanceUntilIdle()

        assertEquals(
          Loading,
          languageViewModel.state.value
        )
      }

      @Test
      fun whenStateContent_returnsSavingAndEmitsSideEffect() = runTest {
        coEvery { observeLanguages(any(), any()) } returns ObserveLanguages.Result.Success(emptyList())
        createViewModel()
        advanceUntilIdle()
        val english = createLanguage(code = "eng", active = true)
        val french = createLanguage(code = "fr", active = false, id = 2)
        languageViewModel.state.value = State.Content(listOf(english, french))

        var sideEffect: SideEffect<*>? = null
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
          languageViewModel.effects.collect {
            sideEffect = it
          }
        }

        languageViewModel.actions.emit(Action.Save)
        advanceUntilIdle()

        assertEquals(
          State.Saving,
          languageViewModel.state.value
        )

        assertThat(sideEffect).isInstanceOf(SaveLanguagesAndFinish::class.java)
        val saveEffect = sideEffect as SaveLanguagesAndFinish
        assertThat(saveEffect.languages.map { it.languageCode }).containsExactly("eng")

        collectJob.cancel()
      }
    }
  }
}
