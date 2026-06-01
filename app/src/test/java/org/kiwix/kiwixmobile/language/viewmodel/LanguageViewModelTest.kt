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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.LanguageEntry
import org.kiwix.kiwixmobile.core.data.remote.LanguageFeed
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.language.composables.LanguageListItem
import org.kiwix.kiwixmobile.language.viewmodel.State.Loading
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class LanguageViewModelTest {
  @RegisterExtension
  private val ioDispatcher = MainDispatcherRule()
  private val application: Application = mockk(relaxed = true)
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val kiwixService: KiwixService = mockk()
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()
  private val networkStates = MutableStateFlow(NetworkState.NOT_CONNECTED)
  private lateinit var languageViewModel: LanguageViewModel
  private var languages: MutableStateFlow<List<Language>?> = MutableStateFlow(null)

  private fun createViewModel() {
    languageViewModel =
      LanguageViewModel(
        application,
        kiwixDataStore,
        kiwixService,
        connectivityBroadcastReceiver,
        ioDispatcher.dispatcher
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
    LanguageSessionCache.hasFetched = false
    networkStates.value = NetworkState.NOT_CONNECTED
    every { kiwixDataStore.cachedLanguageList } returns languages
    every { kiwixDataStore.selectedOnlineContentLanguage } returns MutableStateFlow("eng")
  }

  @Test
  fun `unregisters broadcastReceiver in onCleared`() {
    createViewModel()
    every { application.unregisterReceiver(any()) } returns mockk()
    languageViewModel.onClearedExposed()
    verify {
      application.unregisterReceiver(connectivityBroadcastReceiver)
    }
  }

  @Test
  fun init_whenInvoked_returnsDefaultLoadingState() = runTest {
    createViewModel()
    assertThat(languageViewModel.state.value).isEqualTo(Loading)
  }

  @Nested
  inner class Init {
    @Nested
    inner class ObserveLanguages {
      @Nested
      inner class SessionCache {
        @Test
        fun whenHasFetchedTrueAndCachedLanguageExists_returnsContent() = runTest {
          LanguageSessionCache.hasFetched = true

          val english = createLanguage()
          languages.value = listOf(english)

          createViewModel()

          advanceUntilIdle()

          assertEquals(State.Content(listOf(english)), languageViewModel.state.value)
        }

        @Test
        fun whenHasFetchedTrueAndCachedLanguageEmpty_returnsNoNetworkError() = runTest {
          LanguageSessionCache.hasFetched = true

          every {
            application.getString(
              R.string.no_network_connection
            )
          } returns "No network connection"

          languages.value = null

          createViewModel()

          advanceUntilIdle()

          assertEquals(
            State.Error("No network connection"),
            languageViewModel.state.value
          )
        }
      }

      @Nested
      inner class Online {
        @Test
        fun whenApiReturnsLanguages_returnsContent() = runTest {
          networkStates.value = NetworkState.CONNECTED

          val languageEntry =
            LanguageEntry().apply {
              languageCode = "eng"
              count = 10
            }

          val languageFeed =
            LanguageFeed().apply {
              entries = listOf(languageEntry)
            }

          coEvery { kiwixService.getLanguages() } returns languageFeed

          coEvery { kiwixDataStore.saveLanguageList(any()) } just Runs

          createViewModel()

          advanceUntilIdle()

          val expected =
            State.Content(
              listOf(
                createLanguage(
                  code = "",
                  active = false,
                  occurrences = 10,
                  id = 0
                ),
                createLanguage(
                  code = "eng",
                  active = true,
                  occurrences = 10,
                  id = 1
                )
              )
            )

          assertTrue(
            LanguageSessionCache.hasFetched
          )
          coVerify(exactly = 1) {
            kiwixDataStore.saveLanguageList(any())
          }
          assertEquals(
            expected,
            languageViewModel.state.value
          )
        }

        @Test
        fun whenApiReturnsEmptyAndCachedLanguageExists_returnsContent() = runTest {
          networkStates.value = NetworkState.CONNECTED

          val english = createLanguage()

          languages.value = listOf(english)

          coEvery {
            kiwixService.getLanguages()
          } returns LanguageFeed().apply {
            entries = emptyList()
          }

          createViewModel()

          advanceUntilIdle()

          assertEquals(
            State.Content(
              listOf(english)
            ),
            languageViewModel.state.value
          )
        }

        @Test
        fun whenApiReturnsEmptyAndCachedLanguageEmpty_returnsNoLanguageError() = runTest {
          networkStates.value = NetworkState.CONNECTED

          languages.value = null

          every {
            application.getString(R.string.no_language_available)
          } returns "No language available"

          coEvery {
            kiwixService.getLanguages()
          } returns LanguageFeed().apply {
            entries = emptyList()
          }

          createViewModel()

          advanceUntilIdle()

          assertEquals(
            State.Error("No language available"),
            languageViewModel.state.value
          )
        }

        @Test
        fun whenApiThrowsAndCachedLanguageExists_returnsContent() = runTest {
          networkStates.value = NetworkState.CONNECTED

          val english = createLanguage()

          languages.value = listOf(english)

          coEvery {
            kiwixService.getLanguages()
          } throws RuntimeException()

          createViewModel()

          advanceUntilIdle()

          assertEquals(
            State.Content(listOf(english)),
            languageViewModel.state.value
          )
        }

        @Test
        fun whenApiThrowsAndCachedLanguageEmpty_returnsNoLanguageError() = runTest {
          networkStates.value = NetworkState.CONNECTED

          languages.value = null

          every {
            application.getString(R.string.no_language_available)
          } returns "No language available"

          coEvery {
            kiwixService.getLanguages()
          } throws RuntimeException()

          createViewModel()

          advanceUntilIdle()

          assertEquals(
            State.Error("No language available"),
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

            assertFalse(
              updatedEnglish.active
            )

            assertTrue(
              updatedFrench.active
            )
          }
        }

        @Nested
        inner class ActionSave {
          @Test
          fun whenStateNotContent_returnsCurrentState() = runTest {
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
          fun whenStateContent_returnsSaving() = runTest {
            createViewModel()
            advanceUntilIdle()
            languageViewModel.state.value = State.Content(listOf(createLanguage()))

            languageViewModel.actions.emit(Action.Save)

            advanceUntilIdle()

            assertEquals(
              State.Saving,
              languageViewModel.state.value
            )
          }
        }
      }
    }
  }
}
