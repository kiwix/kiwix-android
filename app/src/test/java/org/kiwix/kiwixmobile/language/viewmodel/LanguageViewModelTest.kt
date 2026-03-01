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
import android.os.Build
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.data.remote.CategoryFeed
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.LanguageFeed
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.language.composables.LanguageListItem
import org.kiwix.kiwixmobile.language.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.language.viewmodel.Action.Save
import org.kiwix.kiwixmobile.language.viewmodel.Action.Select
import org.kiwix.kiwixmobile.language.viewmodel.State.Content
import org.kiwix.kiwixmobile.language.viewmodel.State.Loading
import org.kiwix.kiwixmobile.zimManager.testFlow
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.language

fun languageItem(language: Language = language()) =
  LanguageListItem.LanguageItem(language)

@ExtendWith(InstantExecutorExtension::class)
class LanguageViewModelTest {
  private val application: Application = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val kiwixService: KiwixService = mockk()
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()
  private val networkStates = MutableStateFlow(NetworkState.CONNECTED)
  private lateinit var languageViewModel: LanguageViewModel
  private var languages: MutableStateFlow<List<Language>?> = MutableStateFlow(null)

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { application.getString(any()) } returns "Error"
    every { connectivityBroadcastReceiver.action } returns "test"
    every { connectivityBroadcastReceiver.networkStates } returns networkStates
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      every { application.registerReceiver(any(), any(), any()) } returns mockk()
    } else {
      @Suppress("UnspecifiedRegisterReceiverFlag")
      every { application.registerReceiver(any(), any()) } returns mockk()
    }
    every { application.unregisterReceiver(any()) } just Runs
    LanguageSessionCache.hasFetched = false
    every { kiwixDataStore.cachedLanguageList } returns flowOf(languages.value)
    every { kiwixDataStore.selectedOnlineContentLanguage } returns flowOf("eng")
    coEvery { kiwixService.getLanguages() } returns LanguageFeed()
    every { application.getString(any<Int>()) } returns "Error"
  }

  private fun createViewModel() {
    languageViewModel =
      LanguageViewModel(
        application,
        kiwixDataStore,
        kiwixService,
        connectivityBroadcastReceiver
      ).apply {
        setIsUnitTestCase()
      }
  }

  @Nested
  inner class Context {
    @Test
    fun `registers broadcastReceiver in init`() {
      createViewModel()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        verify {
          application.registerReceiver(connectivityBroadcastReceiver, any(), any())
        }
      } else {
        @Suppress("UnspecifiedRegisterReceiverFlag")
        verify {
          application.registerReceiver(connectivityBroadcastReceiver, any())
        }
      }
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
  }

  @Test
  fun `initial state is Loading`() = flakyTest {
    runTest {
      coEvery { kiwixService.getCategories() } returns CategoryFeed()
      createViewModel()
      assertThat(languageViewModel.state.value).isEqualTo(Loading)
    }
  }

  @Test
  fun `an empty languages emission does not send update action`() = flakyTest {
    runTest {
      createViewModel()
      testFlow(
        languageViewModel.actions,
        triggerAction = { languages.emit(listOf()) },
        assert = { expectNoEvents() }
      )
    }
  }

  @Test
  fun `online and api returns empty emits Error when no cache`() = flakyTest {
    runTest {
      coEvery { kiwixService.getLanguages() } returns LanguageFeed()
      coEvery { application.getString(R.string.no_language_available) } returns "No language available"

      createViewModel()
      languageViewModel.state.test {
        assertThat(awaitItem()).isEqualTo(Loading)
        val error = awaitItem() as State.Error
        assertThat(error.errorMessage).isEqualTo("No language available")
      }
    }
  }

  @Test
  fun `online api throws exception falls back to error`() = flakyTest {
    runTest {
      coEvery { kiwixService.getLanguages() } throws RuntimeException()
      createViewModel()
      languageViewModel.state.test {
        assertThat(awaitItem()).isEqualTo(Loading)
        val error = awaitItem() as State.Error
        assertThat(error.errorMessage).isEqualTo("Error")
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Save uses active language`() = flakyTest {
    runTest {
      every { application.getString(any()) } returns ""
      createViewModel()
      val activeLanguage = language(languageCode = "eng").copy(active = true)
      val inactiveLanguage = language(languageCode = "fr").copy(active = false)
      languageViewModel.effects.test {
        languageViewModel.state.value = Content(listOf(activeLanguage, inactiveLanguage))
        languageViewModel.actions.emit(Save)
        advanceUntilIdle()
        advanceTimeBy(100)
        val effect = awaitItem() as SaveLanguagesAndFinish
        assertThat(effect.languages).isEqualTo(activeLanguage)
      }
    }
  }

  @Test
  fun `offline uses cached languages`() = flakyTest {
    runTest {
      networkStates.value = NetworkState.NOT_CONNECTED

      val cached =
        listOf(
          Language(languageCode = "eng", active = true, occurrencesOfLanguage = 1)
        )

      coEvery { kiwixDataStore.cachedLanguageList } returns flowOf(cached)

      createViewModel()
      languageViewModel.state.test {
        assertThat(awaitItem()).isEqualTo(Loading)
        val content = awaitItem() as State.Content
        assertThat(content.items.first().languageCode)
          .isEqualTo("eng")
      }
    }
  }

  @Test
  fun `offline and no cache emits no network error`() = flakyTest {
    runTest {
      networkStates.value = NetworkState.NOT_CONNECTED
      createViewModel()
      languageViewModel.state.test {
        assertThat(awaitItem()).isEqualTo(Loading)
        val error = awaitItem() as State.Error
        assertThat(error.errorMessage).isEqualTo("Error")
      }
    }
  }

  @Test
  fun `session cache skips api call`() = flakyTest {
    runTest {
      LanguageSessionCache.hasFetched = true

      val cached =
        listOf(
          Language(languageCode = "eng", active = true, occurrencesOfLanguage = 1)
        )

      coEvery { kiwixDataStore.cachedLanguageList } returns flowOf(cached)

      createViewModel()
      verify(exactly = 0) {
        runBlocking { kiwixService.getCategories() }
      }

      assertThat(languageViewModel.state.value)
        .isInstanceOf(Content::class.java)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Filter updates content filter`() = flakyTest {
    runTest {
      val languages = listOf(language(), language(language = "eng"))

      createViewModel()
      languageViewModel.state.test {
        skipItems(1)
        languageViewModel.state.value = Content(languages)

        languageViewModel.actions.emit(Filter("eng"))
        advanceUntilIdle()
        val content = awaitItem() as Content
        print("content $content")
        val filteredItem: List<LanguageListItem.LanguageItem> =
          content.viewItems.filter {
            it is LanguageListItem.LanguageItem && it.language.language == "eng"
          } as List<LanguageListItem.LanguageItem>
        filteredItem.any { it.language.language == "eng" }
        cancelAndConsumeRemainingEvents()
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Select ignored when not in Content`() = flakyTest {
    runTest {
      createViewModel()

      languageViewModel.actions.emit(Select(languageItem()))
      advanceUntilIdle()

      assertThat(languageViewModel.state.value).isEqualTo(Loading)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Save ignored when not in Content`() = flakyTest {
    runTest {
      createViewModel()

      languageViewModel.actions.emit(Save)
      advanceUntilIdle()

      assertThat(languageViewModel.state.value).isEqualTo(Loading)
    }
  }
}

inline fun flakyTest(
  maxRetries: Int = 10,
  delayMillis: Long = 0,
  block: () -> Unit
) {
  var lastError: Throwable? = null

  repeat(maxRetries) { attempt ->
    try {
      block()
      return
    } catch (e: Throwable) {
      lastError = e
      println("Test attempt ${attempt + 1} failed: ${e.message}")
      if (delayMillis > 0) Thread.sleep(delayMillis)
    }
  }

  throw lastError ?: AssertionError("Test failed after $maxRetries attempts")
}
