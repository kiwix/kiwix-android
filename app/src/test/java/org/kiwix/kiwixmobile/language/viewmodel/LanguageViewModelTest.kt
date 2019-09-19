/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */

package org.kiwix.kiwixmobile.language.viewmodel

import com.jraska.livedata.test
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kiwix.kiwixmobile.InstantExecutorExtension
import org.kiwix.kiwixmobile.database.newdb.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.language
import org.kiwix.kiwixmobile.language.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.language.viewmodel.Action.SaveAll
import org.kiwix.kiwixmobile.language.viewmodel.Action.Select
import org.kiwix.kiwixmobile.language.viewmodel.Action.UpdateLanguages
import org.kiwix.kiwixmobile.language.viewmodel.State.Content
import org.kiwix.kiwixmobile.language.viewmodel.State.Loading
import org.kiwix.kiwixmobile.language.viewmodel.State.Saving
import org.kiwix.kiwixmobile.languageItem
import org.kiwix.kiwixmobile.resetSchedulers
import org.kiwix.kiwixmobile.setScheduler
import org.kiwix.kiwixmobile.zim_manager.Language

@ExtendWith(InstantExecutorExtension::class)
class LanguageViewModelTest {
  init {
    setScheduler(Schedulers.trampoline())
  }

  @AfterAll
  fun teardown() {
    resetSchedulers()
  }

  private val newLanguagesDao: NewLanguagesDao = mockk()
  private lateinit var languageViewModel: LanguageViewModel

  private val languages: PublishProcessor<List<Language>> = PublishProcessor.create()

  @BeforeEach
  fun init() {
    clearAllMocks()
    every { newLanguagesDao.languages() } returns languages
    languageViewModel = LanguageViewModel(newLanguagesDao)
  }

  @Test
  fun `initial state is Loading`() {
    languageViewModel.state.test()
      .assertValueHistory(Loading)
  }

  @Test
  fun `an empty languages emission does not send update action`() {
    languageViewModel.actions.test()
      .also {
        languages.offer(listOf())
      }
      .assertValues()
  }

  @Test
  fun `a languages emission sends update action`() {
    val expectedList = listOf(language())
    languageViewModel.actions.test()
      .also {
        languages.offer(expectedList)
      }
      .assertValues(UpdateLanguages(expectedList))
  }

  @Test
  fun `UpdateLanguages Action changes state to Content when Loading`() {
    languageViewModel.actions.offer(UpdateLanguages(listOf()))
    languageViewModel.state.test()
      .assertValueHistory(Content(listOf()))
  }

  @Test
  fun `UpdateLanguages Action has no effect on other states`() {
    languageViewModel.actions.offer(UpdateLanguages(listOf()))
    languageViewModel.actions.offer(UpdateLanguages(listOf()))
    languageViewModel.state.test()
      .assertValueHistory(Content(listOf()))
  }

  @Test
  fun `Filter Action updates Content state `() {
    languageViewModel.actions.offer(UpdateLanguages(listOf()))
    languageViewModel.actions.offer(Filter("filter"))
    languageViewModel.state.test()
      .assertValueHistory(Content(listOf(), filter = "filter"))
  }

  @Test
  fun `Filter Action has no effect on other states`() {
    languageViewModel.actions.offer(Filter(""))
    languageViewModel.state.test()
      .assertValueHistory(Loading)
  }

  @Test
  fun `Select Action updates Content state`() {
    languageViewModel.actions.offer(UpdateLanguages(listOf(language())))
    languageViewModel.actions.offer(Select(languageItem()))
    languageViewModel.state.test()
      .assertValueHistory(Content(listOf(language(isActive = true))))
  }

  @Test
  fun `Select Action has no effect on other states`() {
    languageViewModel.actions.offer(Select(languageItem()))
    languageViewModel.state.test()
      .assertValueHistory(Loading)
  }

  @Test
  fun `SaveAll changes Content to Saving with SideEffect SaveLanguagesAndFinish`() {
    languageViewModel.actions.offer(UpdateLanguages(listOf()))
    languageViewModel.effects.test()
      .also {
        languageViewModel.actions.offer(SaveAll)
      }
      .assertValues(SaveLanguagesAndFinish(listOf(), newLanguagesDao))
    languageViewModel.state.test()
      .assertValueHistory(Saving)
  }

  @Test
  fun `SaveAll has no effect on other states`() {
    languageViewModel.actions.offer(SaveAll)
    languageViewModel.state.test()
      .assertValueHistory(Loading)
  }
}
