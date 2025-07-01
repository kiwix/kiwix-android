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

import androidx.lifecycle.viewModelScope
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.core.dao.LanguageRoomDao
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.kiwix.kiwixmobile.language.composables.LanguageListItem
import org.kiwix.kiwixmobile.language.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.language.viewmodel.Action.SaveAll
import org.kiwix.kiwixmobile.language.viewmodel.Action.Select
import org.kiwix.kiwixmobile.language.viewmodel.Action.UpdateLanguages
import org.kiwix.kiwixmobile.language.viewmodel.State.Content
import org.kiwix.kiwixmobile.language.viewmodel.State.Loading
import org.kiwix.kiwixmobile.zimManager.testFlow
import org.kiwix.sharedFunctions.InstantExecutorExtension
import org.kiwix.sharedFunctions.language

fun languageItem(language: Language = language()) =
  LanguageListItem.LanguageItem(language)

@ExtendWith(InstantExecutorExtension::class)
class LanguageViewModelTest {
  private val languageRoomDao: LanguageRoomDao = mockk()
  private lateinit var languageViewModel: LanguageViewModel
  private lateinit var languages: MutableStateFlow<List<Language>>

  @BeforeEach
  fun init() {
    clearAllMocks()
    languages = MutableStateFlow(emptyList())
    every { languageRoomDao.languages() } returns languages
    languageViewModel =
      LanguageViewModel(languageRoomDao)
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
  fun `a languages emission sends update action`() = runTest {
    val expectedList = listOf(language())
    testFlow(
      languageViewModel.actions,
      triggerAction = { languages.emit(expectedList) },
      assert = {
        assertThat(awaitItem()).isEqualTo(UpdateLanguages(expectedList))
      }
    )
  }

  @Test
  fun `UpdateLanguages Action changes state to Content when Loading`() = runTest {
    testFlow(
      languageViewModel.state,
      triggerAction = { languageViewModel.actions.emit(UpdateLanguages(listOf())) },
      assert = {
        assertThat(awaitItem()).isEqualTo(Loading)
        assertThat(awaitItem()).isEqualTo(Content(listOf()))
      }
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
    testFlow(
      languageViewModel.state,
      triggerAction = {
        languageViewModel.actions.emit(UpdateLanguages(listOf(language())))
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
  fun `SaveAll changes Content to Saving with SideEffect SaveLanguagesAndFinish`() = runTest {
    val languages = listOf<Language>()
    testFlow(
      flow = languageViewModel.effects,
      triggerAction = {
        languageViewModel.actions.emit(UpdateLanguages(languages))
        languageViewModel.actions.emit(SaveAll)
      },
      assert = {
        assertThat(awaitItem()).isEqualTo(
          SaveLanguagesAndFinish(
            languages,
            languageRoomDao,
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
  fun `SaveAll has no effect on other states`() = runTest {
    testFlow(
      languageViewModel.state,
      triggerAction = { languageViewModel.actions.emit(SaveAll) },
      { assertThat(awaitItem()).isEqualTo(Loading) }
    )
  }
}
