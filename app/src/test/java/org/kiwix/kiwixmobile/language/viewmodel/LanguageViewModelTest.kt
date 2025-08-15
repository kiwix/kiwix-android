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
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.LanguageEntry
import org.kiwix.kiwixmobile.core.data.remote.LanguageFeed
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.zim_manager.ConnectivityBroadcastReceiver
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.core.zim_manager.NetworkState
import org.kiwix.kiwixmobile.language.composables.LanguageListItem
import org.kiwix.kiwixmobile.language.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.language.viewmodel.Action.Save
import org.kiwix.kiwixmobile.language.viewmodel.Action.Select
import org.kiwix.kiwixmobile.language.viewmodel.Action.UpdateLanguages
import org.kiwix.kiwixmobile.language.viewmodel.State.Content
import org.kiwix.kiwixmobile.language.viewmodel.State.Loading
import org.kiwix.kiwixmobile.zimManager.TURBINE_TIMEOUT
import org.kiwix.kiwixmobile.zimManager.testFlow
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.language

fun languageItem(language: Language = language()) =
  LanguageListItem.LanguageItem(language)

@ExtendWith(InstantExecutorExtension::class)
class LanguageViewModelTest {
  private val application: Application = mockk()
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
  private val kiwixService: KiwixService = mockk()
  private val connectivityBroadcastReceiver: ConnectivityBroadcastReceiver = mockk()
  private val networkStates = MutableStateFlow(NetworkState.CONNECTED)
  private lateinit var languageViewModel: LanguageViewModel
  private var languages: MutableStateFlow<List<Language>?> = MutableStateFlow(null)

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { application.getString(any()) } returns ""
    every { connectivityBroadcastReceiver.action } returns "test"
    every { connectivityBroadcastReceiver.networkStates } returns networkStates
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      every { application.registerReceiver(any(), any(), any()) } returns mockk()
    } else {
      @Suppress("UnspecifiedRegisterReceiverFlag")
      every { application.registerReceiver(any(), any()) } returns mockk()
    }
    languages.value = null
    networkStates.value = NetworkState.CONNECTED
    LanguageSessionCache.hasFetched = true
    every { sharedPreferenceUtil.getCachedLanguageList() } returns languages.value
    every { sharedPreferenceUtil.selectedOnlineContentLanguage } returns "eng"
    languageViewModel =
      LanguageViewModel(
        application,
        sharedPreferenceUtil,
        kiwixService,
        connectivityBroadcastReceiver
      )
    runBlocking { languageViewModel.state.emit(Loading) }
  }

  @Nested
  inner class Context {
    @Test
    fun `registers broadcastReceiver in init`() {
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
      every { application.unregisterReceiver(any()) } returns mockk()
      languageViewModel.onClearedExposed()
      verify {
        application.unregisterReceiver(connectivityBroadcastReceiver)
      }
    }
  }

  @Test
  fun `initial state is Loading`() = runTest {
    testFlow(
      flow = languageViewModel.state,
      triggerAction = {},
      assert = { assertThat(awaitItem()).isEqualTo(Loading) }
    )
  }

  @Test
  fun `an empty languages emission does not send update action`() = runTest {
    testFlow(
      languageViewModel.actions,
      triggerAction = { languages.emit(listOf()) },
      assert = { expectNoEvents() }
    )
  }

  @Test
  fun `observeLanguages uses network when no cache and online`() = flakyTest {
    runTest {
      every { application.getString(any()) } returns ""
      val fetchedLanguages = listOf(language(languageCode = "eng"))
      LanguageSessionCache.hasFetched = false
      languages.value = emptyList()

      every { sharedPreferenceUtil.getCachedLanguageList() } returns null
      coEvery { kiwixService.getLanguages() } returns LanguageFeed().apply {
        entries = fetchedLanguages.map {
          LanguageEntry().apply {
            languageCode = it.languageCode
            count = 1
            title = "English"
          }
        }
      }
      every { sharedPreferenceUtil.selectedOnlineContentLanguage } returns ""
      every { sharedPreferenceUtil.saveLanguageList(any()) } just Runs

      testFlow(
        languageViewModel.actions,
        triggerAction = {},
        assert = {
          val result = awaitItem()
          assertThat(result).isInstanceOf(UpdateLanguages::class.java)
          verify { sharedPreferenceUtil.saveLanguageList(any()) }
        }
      )
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Save uses active language`() = flakyTest {
    runTest {
      every { application.getString(any()) } returns ""
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
  fun `UpdateLanguages Action changes state to Content when Loading`() = runTest {
    every { application.getString(any()) } returns ""
    testFlow(
      languageViewModel.state,
      triggerAction = { languageViewModel.actions.emit(UpdateLanguages(listOf())) },
      assert = {
        assertThat(awaitItem()).isEqualTo(Loading)
        assertThat(awaitItem()).isEqualTo(Content(listOf()))
      },
      TURBINE_TIMEOUT
    )
  }

  @Test
  fun `UpdateLanguages Action has no effect on other states`() = runTest {
    testFlow(
      languageViewModel.state,
      triggerAction = {
        languageViewModel.actions.emit(UpdateLanguages(listOf()))
        languageViewModel.actions.emit(UpdateLanguages(listOf()))
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(Loading)
        assertThat(awaitItem()).isEqualTo(Content(listOf()))
      }
    )
  }

  @Test
  fun `Filter Action updates Content state `() = runTest {
    every { application.getString(any()) } returns ""
    languages.value = listOf()
    testFlow(
      languageViewModel.state,
      triggerAction = {
        languageViewModel.actions.tryEmit(UpdateLanguages(listOf()))
        languageViewModel.actions.tryEmit(Filter("filter"))
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(Loading)
        assertThat(awaitItem()).isEqualTo(Content(items = listOf(), filter = ""))
        assertThat(awaitItem()).isEqualTo(Content(listOf(), filter = "filter"))
      }
    )
  }

  @Test
  fun `Filter Action has no effect on other states`() = runTest {
    every { application.getString(any()) } returns ""
    testFlow(
      languageViewModel.state,
      triggerAction = { languageViewModel.actions.emit(Filter("")) },
      assert = {
        assertThat(awaitItem()).isEqualTo(Loading)
      }
    )
  }

  @Test
  fun `Select Action updates Content state`() = runTest {
    val languageList = listOf(language())
    languages.value = languageList
    testFlow(
      languageViewModel.state,
      triggerAction = {
        languageViewModel.actions.emit(UpdateLanguages(languageList))
        languageViewModel.actions.emit(Select(languageItem()))
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(Loading)
        assertThat(awaitItem()).isEqualTo(Content(listOf(language())))
        assertThat(awaitItem()).isEqualTo(Content(listOf(language(isActive = true))))
      }
    )
  }

  @Test
  fun `Select Action has no effect on other states`() = runTest {
    testFlow(
      languageViewModel.state,
      triggerAction = { languageViewModel.actions.emit(Select(languageItem())) },
      assert = {
        assertThat(awaitItem()).isEqualTo(Loading)
      }
    )
  }

  @Test
  fun `Save changes Content to Saving with SideEffect SaveLanguagesAndFinish`() = runTest {
    every { application.getString(any()) } returns ""
    val languages = arrayListOf<Language>().apply {
      add(Language(languageCode = "eng", active = true, occurrencesOfLanguage = 1))
    }
    testFlow(
      flow = languageViewModel.effects,
      triggerAction = {
        languageViewModel.actions.emit(UpdateLanguages(languages))
        languageViewModel.actions.emit(Save)
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(
          SaveLanguagesAndFinish(
            languages.first(),
            sharedPreferenceUtil,
            languageViewModel.viewModelScope
          )
        )
      }
    )
    testFlow(flow = languageViewModel.state, triggerAction = {}, assert = {
      assertThat(awaitItem()).isEqualTo(State.Saving)
    })
  }

  @Test
  fun `Save has no effect on other states`() = runTest {
    languageViewModel.state.emit(Loading)
    testFlow(
      languageViewModel.state,
      triggerAction = { languageViewModel.actions.emit(Save) },
      { assertThat(awaitItem()).isEqualTo(Loading) }
    )
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
