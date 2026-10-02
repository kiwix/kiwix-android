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

package org.kiwix.kiwixmobile.nav.destination.library.online.usecase

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.nav.destination.library.online.usecase.OnlineLibraryTabsUseCase.TabSelectedResult.FetchNeeded
import org.kiwix.kiwixmobile.nav.destination.library.online.usecase.OnlineLibraryTabsUseCase.TabSelectedResult.FromCache
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.LanguageTab
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryRequest
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Success
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.TabData
import java.util.Locale

class OnlineLibraryTabsUseCaseTest {
  private val context: Context = mockk(relaxed = true)
  private val kiwixDataStore: KiwixDataStore = mockk(relaxed = true)
  private val bookUtils: BookUtils = mockk(relaxed = true)
  private lateinit var useCase: OnlineLibraryTabsUseCase

  @BeforeEach
  fun setUp() {
    val configuration = Configuration()
    configuration.setLocales(LocaleList(Locale.ENGLISH))
    val resources = mockk<Resources>(relaxed = true)
    every { resources.configuration } returns configuration
    every { context.resources } returns resources
    every { context.getString(R.string.all_languages) } returns "All Languages"
    every { kiwixDataStore.prefLanguage } returns MutableStateFlow("")
    every { bookUtils.localeMap } returns mapOf(
      "eng" to Locale.ENGLISH,
      "fra" to Locale.FRENCH
    )
    useCase = OnlineLibraryTabsUseCase(context, kiwixDataStore, bookUtils)
  }

  @Nested
  inner class CreateTabsTests {
    @Test
    fun `when language string is all createTabs returns single all languages tab`() = runTest {
      val tabs = useCase.createTabs("all")
      assertThat(tabs).hasSize(1)
      assertThat(tabs.first().languageCode).isNull()
      assertThat(tabs.first().displayName).isEqualTo("ALL LANGUAGES")
    }

    @Test
    fun `createTabs preserves order of selected languages`() = runTest {
      val tabs = useCase.createTabs("eng,fra")
      assertThat(tabs).hasSize(2)
      assertThat(tabs[0].languageCode).isEqualTo("eng")
      assertThat(tabs[1].languageCode).isEqualTo("fra")
    }
  }

  @Nested
  inner class TabSelectionTests {
    @Test
    fun `selectTab returns FromCache when tab data exists and is loaded`() {
      val book = mockk<LibkiwixBook>(relaxed = true)
      useCase.tabDataMap["eng"] = TabData(
        books = listOf(book),
        totalPages = 2,
        currentPage = 1,
        isLoaded = true
      )
      val tabs = listOf(LanguageTab("eng", "ENGLISH"))
      val currentRequest = OnlineLibraryRequest(isLoadMoreItem = false, page = 0)

      val result = useCase.selectTab(0, tabs, currentRequest)

      assertThat(result).isInstanceOf(FromCache::class.java)
      val cacheResult = result as FromCache
      assertThat(cacheResult.books).containsExactly(book)
      assertThat(cacheResult.totalPages).isEqualTo(2)
      assertThat(cacheResult.currentPage).isEqualTo(1)
    }

    @Test
    fun `selectTab returns FetchNeeded when tab data is not in cache`() {
      val tabs = listOf(LanguageTab("fra", "FRENCH"))
      val currentRequest = OnlineLibraryRequest(isLoadMoreItem = false, page = 0)

      val result = useCase.selectTab(0, tabs, currentRequest)

      assertThat(result).isInstanceOf(FetchNeeded::class.java)
      val fetchResult = result as FetchNeeded
      assertThat(fetchResult.newRequest.lang).isEqualTo("fra")
      assertThat(fetchResult.newRequest.page).isEqualTo(0)
    }

    @Test
    fun `selectTab returns null when index is out of bounds`() {
      val result = useCase.selectTab(
        5,
        emptyList(),
        OnlineLibraryRequest(isLoadMoreItem = false, page = 0)
      )
      assertThat(result).isNull()
    }
  }

  @Nested
  inner class SuccessHandlingTests {
    @Test
    fun `handleSuccessState updates tabDataMap and returns isCurrentTab true`() {
      val book = mockk<LibkiwixBook>(relaxed = true)
      val request = OnlineLibraryRequest(lang = "eng", isLoadMoreItem = false, page = 0)
      val state = Success(books = listOf(book), totalPages = 3, request = request)
      val currentTab = LanguageTab("eng", "ENGLISH")

      val result = useCase.handleSuccessState(state, currentTab, emptyList())

      assertThat(result.isCurrentTab).isTrue()
      assertThat(result.updatedBooks).containsExactly(book)
      assertThat(result.totalPages).isEqualTo(3)
      assertThat(useCase.tabDataMap["eng"]?.isLoaded).isTrue()
      assertThat(useCase.tabDataMap["eng"]?.books).containsExactly(book)
    }

    @Test
    fun `handleSuccessState appends books when isLoadMore is true`() {
      val book1 = mockk<LibkiwixBook>(relaxed = true)
      val book2 = mockk<LibkiwixBook>(relaxed = true)
      useCase.tabDataMap["eng"] = TabData(books = listOf(book1), isLoaded = true)
      val request = OnlineLibraryRequest(lang = "eng", isLoadMoreItem = true, page = 1)
      val state = Success(books = listOf(book2), totalPages = 3, request = request)
      val currentTab = LanguageTab("eng", "ENGLISH")

      val result = useCase.handleSuccessState(state, currentTab, listOf(book1))

      assertThat(result.isLoadMore).isTrue()
      assertThat(result.updatedBooks).containsExactly(book1, book2)
      assertThat(useCase.tabDataMap["eng"]?.books).containsExactly(book1, book2)
    }
  }

  @Nested
  inner class LoadMoreTests {
    @Test
    fun `handleLoadMore returns next request when page is available`() {
      val currentTab = LanguageTab("eng", "ENGLISH")
      useCase.tabDataMap["eng"] = TabData(totalPages = 5, currentPage = 0, isLoaded = true)
      val currentRequest = OnlineLibraryRequest(isLoadMoreItem = false, page = 0)

      val nextRequest = useCase.handleLoadMore(
        count = 10,
        currentTab = currentTab,
        fallbackTotalPages = 5,
        currentRequest = currentRequest,
        isCurrentlyLoadingMore = false
      )

      assertThat(nextRequest).isNotNull
      assertThat(nextRequest?.page).isEqualTo(1)
      assertThat(nextRequest?.isLoadMoreItem).isTrue()
    }

    @Test
    fun `handleLoadMore returns null when already loading more`() {
      val currentTab = LanguageTab("eng", "ENGLISH")
      useCase.tabDataMap["eng"] = TabData(totalPages = 5, isLoadingMore = true)
      val currentRequest = OnlineLibraryRequest(isLoadMoreItem = false, page = 0)

      val nextRequest = useCase.handleLoadMore(
        count = 10,
        currentTab = currentTab,
        fallbackTotalPages = 5,
        currentRequest = currentRequest,
        isCurrentlyLoadingMore = true
      )

      assertThat(nextRequest).isNull()
    }
  }

  @Test
  fun `clear empties tabDataMap`() {
    useCase.tabDataMap["eng"] = TabData()
    useCase.clear()
    assertThat(useCase.tabDataMap).isEmpty()
  }
}
