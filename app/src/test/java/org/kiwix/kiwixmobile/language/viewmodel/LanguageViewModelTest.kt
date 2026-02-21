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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.LanguageFeed
import org.kiwix.kiwixmobile.core.data.remote.LanguageEntry
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.language.composables.LanguageListItem
import org.kiwix.kiwixmobile.language.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.language.viewmodel.Action.Save
import org.kiwix.kiwixmobile.language.viewmodel.Action.Select
import org.kiwix.kiwixmobile.language.viewmodel.Action.ClearAll
import org.kiwix.kiwixmobile.language.viewmodel.Action.Cancel
import org.kiwix.kiwixmobile.core.base.SideEffect
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

  @OptIn(ExperimentalCoroutinesApi::class)
  private val testDispatcher = UnconfinedTestDispatcher()

  @OptIn(ExperimentalCoroutinesApi::class)
  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
    LanguageSessionCache.hasFetched = false
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @BeforeEach
  fun init() {
    Dispatchers.setMain(testDispatcher)
    clearAllMocks()
    networkStates.value = NetworkState.CONNECTED
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
    coEvery { kiwixDataStore.cachedLanguageList } returns flowOf(languages.value)
    every { kiwixDataStore.selectedOnlineContentLanguage } returns flowOf("")
    coEvery { kiwixDataStore.saveLanguageList(any()) } just Runs
  }

  private fun createViewModel() {
    languageViewModel =
      TestLanguageViewModel(
        application,
        kiwixDataStore,
        kiwixService,
        connectivityBroadcastReceiver
      )
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
      coEvery { kiwixService.getLanguages() } coAnswers { awaitCancellation() }
      createViewModel()
      assertThat(languageViewModel.state.value).isEqualTo(Loading)
    }
  }

  @Test
  fun `an empty languages emission does not send update action`() = flakyTest {
    runTest {
      coEvery { kiwixService.getLanguages() } returns LanguageFeed()
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
        val item = awaitItem()
        val error = if (item is Loading) awaitItem() as State.Error else item as State.Error
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
        val error = awaitItem() as State.Error
        assertThat(error.errorMessage).isEqualTo("Error")
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Save uses all active languages`() = flakyTest {
    runTest {
      every { application.getString(any()) } returns ""
      val activeLanguage1 = language(languageCode = "eng")
      val activeLanguage2 = language(languageCode = "fra")
      val inactiveLanguage = language(languageCode = "deu")

      val entries = listOf(activeLanguage1, activeLanguage2, inactiveLanguage).map { lang ->
        LanguageEntry().apply {
          title = lang.language
          languageCode = lang.languageCode
          count = 1
        }
      }
      coEvery { kiwixService.getLanguages() } returns LanguageFeed().apply { this.entries = entries }
      every { kiwixDataStore.selectedOnlineContentLanguage } returns flowOf("eng,fra")

      createViewModel()
      advanceUntilIdle()

      languageViewModel.effects.test {
        languageViewModel.actions.emit(Save)
        val effect = awaitItem() as SaveLanguagesAndFinish
        assertThat(effect.languages.map { it.languageCode }).containsExactlyInAnyOrder("eng", "fra")
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Select toggles active state`() = flakyTest {
    runTest {
      val testLanguage = language(languageCode = "eng").copy(active = false)
      val entry = LanguageEntry().apply {
        title = testLanguage.language
        languageCode = testLanguage.languageCode
        count = 1
      }
      coEvery { kiwixService.getLanguages() } returns LanguageFeed().apply { entries = listOf(entry) }
      every { application.getString(any()) } returns "Error"

      createViewModel()

      languageViewModel.state.test {
        val content = awaitItem() as Content

        assertThat(content.items[1].active).isFalse()

        languageViewModel.actions.emit(Select(languageItem(testLanguage.copy(id = content.items[1].id))))

        val content2 = awaitItem() as Content
        assertThat(content2.items[1].active).isTrue()

        languageViewModel.actions.emit(Select(languageItem(testLanguage.copy(id = content.items[1].id.toLong()))))
        val content3 = awaitItem() as Content
        assertThat(content3.items[1].active).isFalse()
      }
    }
  }

  @Test
  fun `ClearAll clears all selections`() = flakyTest {
    runTest {
      val activeLanguage = language(languageCode = "eng")
      val entry = LanguageEntry().apply {
        title = activeLanguage.language
        languageCode = activeLanguage.languageCode
        count = 1
      }
      coEvery { kiwixService.getLanguages() } returns LanguageFeed().apply { entries = listOf(entry) }
      every { kiwixDataStore.selectedOnlineContentLanguage } returns flowOf("eng")
      every { application.getString(any()) } returns "Error"

      createViewModel()

      languageViewModel.state.test {
        val item = awaitItem()
        val content = if (item is Loading) awaitItem() as Content else item as Content

        languageViewModel.actions.emit(ClearAll)

        val content2 = awaitItem() as Content
        assertThat(content2.items.none { it.active }).isTrue()
      }
    }
  }

  @Test
  fun `Cancel restores state (mocks back press)`() = flakyTest {
    runTest {
      val language = language().copy(active = true)
      val entry = LanguageEntry().apply {
        title = language.language
        languageCode = language.languageCode
      }
      coEvery { kiwixService.getLanguages() } returns LanguageFeed().apply { entries = listOf(entry) }

      createViewModel()

      advanceUntilIdle()

      languageViewModel.effects.test {
        languageViewModel.actions.emit(Cancel)
        val effect = awaitItem() // SideEffect
        assertThat(effect).isInstanceOf(SideEffect::class.java)
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
        val item = awaitItem()
        val content = if (item is Loading) awaitItem() as State.Content else item as State.Content

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
        val item = awaitItem()
        val error = if (item is Loading) awaitItem() as State.Error else item as State.Error
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
        runBlocking { kiwixService.getLanguages() }
      }

      assertThat(languageViewModel.state.value)
        .isInstanceOf(Content::class.java)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Filter updates content filter`() = flakyTest {
    runTest {
      coEvery { kiwixService.getLanguages() } returns LanguageFeed()
      createViewModel()
      val languages = listOf(language(), language(language = "eng"))

      languageViewModel.state.test {
        awaitItem() // consume initial state (Loading or Content)

        languageViewModel.state.value = Content(languages)

        assertThat(languageViewModel.state.value).isInstanceOf(Content::class.java)

        languageViewModel.actions.emit(Filter("eng"))

        val content = awaitItem() as Content
        val filteredItem: List<LanguageListItem.LanguageItem> =
          content.viewItems.filter {
            it.language.language == "eng"
          }
        assertThat(filteredItem).isNotEmpty
        cancelAndConsumeRemainingEvents()
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Select ignored when not in Content`() = flakyTest {
    runTest {
      coEvery { kiwixService.getLanguages() } returns LanguageFeed()
      createViewModel()

      languageViewModel.actions.emit(Select(languageItem()))
      advanceUntilIdle()

      val state = languageViewModel.state.value
      assertThat(state).isNotInstanceOf(Content::class.java)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Save ignored when not in Content`() = flakyTest {
    runTest {
      coEvery { kiwixService.getLanguages() } coAnswers { awaitCancellation() }
      createViewModel()

      languageViewModel.actions.emit(Save)
      advanceUntilIdle()

      assertThat(languageViewModel.state.value).isEqualTo(Loading)
    }
  }
}

class TestLanguageViewModel(
  context: Application,
  kiwixDataStore: KiwixDataStore,
  kiwixService: KiwixService,
  connectivityBroadcastReceiver: ConnectivityBroadcastReceiver
) : LanguageViewModel(
    context,
    kiwixDataStore,
    kiwixService,
    connectivityBroadcastReceiver
  ) {
  override var isUnitTestCase: Boolean
    get() = true
    set(value) {}
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
