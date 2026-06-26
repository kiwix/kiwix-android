/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.language.repository

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.LanguageEntry
import org.kiwix.kiwixmobile.core.data.remote.LanguageFeed
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class LanguageRepositoryImplTest {
  @RegisterExtension
  val dispatcherRule = MainDispatcherRule()

  private val kiwixService: KiwixService = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()

  private lateinit var repository: LanguageRepositoryImpl

  @BeforeEach
  fun setup() {
    repository = LanguageRepositoryImpl(
      kiwixService = kiwixService,
      kiwixDataStore = kiwixDataStore,
      ioDispatcher = dispatcherRule.dispatcher
    )
  }

  @Test
  fun `fetchLanguages emits correct list including all languages item`() = runTest {
    val languageFeed = LanguageFeed().apply {
      entries = listOf(
        LanguageEntry().apply {
          languageCode = "eng"
          count = 10
        },
        LanguageEntry().apply {
          languageCode = "fra"
          count = 5
        }
      )
    }

    coEvery { kiwixService.getLanguages() } returns languageFeed
    every { kiwixDataStore.selectedOnlineContentLanguage } returns flowOf("eng")

    val result = repository.fetchLanguages().toList()
    advanceUntilIdle()

    assertThat(result).hasSize(1)
    val languages = result[0]
    assertThat(languages).hasSize(3)

    // First language is the "all languages" dummy item
    assertThat(languages[0].languageCode).isEmpty()
    assertThat(languages[0].occurencesOfLanguage).isEqualTo(15) // 10 + 5
    assertThat(languages[0].active).isFalse()

    // Second language is eng
    assertThat(languages[1].languageCode).isEqualTo("eng")
    assertThat(languages[1].occurencesOfLanguage).isEqualTo(10)
    assertThat(languages[1].active).isTrue()

    // Third language is fra
    assertThat(languages[2].languageCode).isEqualTo("fra")
    assertThat(languages[2].occurencesOfLanguage).isEqualTo(5)
    assertThat(languages[2].active).isFalse()
  }

  @Test
  fun `fetchLanguages emits empty list when feed has no entries`() = runTest {
    val languageFeed = LanguageFeed().apply {
      entries = emptyList()
    }

    coEvery { kiwixService.getLanguages() } returns languageFeed
    every { kiwixDataStore.selectedOnlineContentLanguage } returns flowOf("")

    val result = repository.fetchLanguages().toList()
    advanceUntilIdle()

    assertThat(result).hasSize(1)
    assertThat(result[0]).isEmpty()
  }

  @Test
  fun `fetchLanguages retries and eventually emits emptyList on continuous failure`() = runTest {
    coEvery { kiwixService.getLanguages() } throws RuntimeException("Network Error")

    val result = repository.fetchLanguages().toList()
    advanceUntilIdle()

    assertThat(result).hasSize(1)
    assertThat(result[0]).isEmpty()
  }
}
