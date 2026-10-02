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
import dagger.hilt.android.qualifiers.ApplicationContext
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.convertToLocal
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.Companion.ITEMS_PER_PAGE
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.ui.components.ONE
import org.kiwix.kiwixmobile.core.utils.BookUtils
import org.kiwix.kiwixmobile.core.utils.LocaleHelper
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.LanguageTab
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryRequest
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.OnlineLibraryState.Success
import org.kiwix.kiwixmobile.nav.destination.library.online.viewmodel.OnlineLibraryViewModel.TabData
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

class OnlineLibraryTabsUseCase @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val kiwixDataStore: KiwixDataStore,
  private val bookUtils: BookUtils
) {
  internal val tabDataMap = ConcurrentHashMap<String, TabData>()

  data class FiltersChangedResult(
    val tabs: List<LanguageTab>,
    val selectedTabIndex: Int,
    val newRequest: OnlineLibraryRequest?
  )

  sealed interface TabSelectedResult {
    data class FromCache(
      val books: List<LibkiwixBook>,
      val totalPages: Int,
      val currentPage: Int,
      val isLoadingMore: Boolean
    ) : TabSelectedResult

    data class FetchNeeded(
      val newRequest: OnlineLibraryRequest
    ) : TabSelectedResult
  }

  data class SuccessResult(
    val updatedBooks: List<LibkiwixBook>,
    val totalPages: Int,
    val isCurrentTab: Boolean,
    val isLoadMore: Boolean
  )

  private suspend fun getAppChosenLanguageCode(): String {
    val appLocale = LocaleHelper.getAppLocale(context, kiwixDataStore)
    return try {
      appLocale.isO3Language.ifEmpty { appLocale.language }
    } catch (_: Exception) {
      appLocale.language
    }
  }

  private suspend fun getDisplayLanguage(languageCode: String): String {
    val mappedLocale = bookUtils.localeMap[languageCode] ?: languageCode.convertToLocal()
    return mappedLocale.getDisplayLanguage(LocaleHelper.getAppLocale(context, kiwixDataStore))
  }

  suspend fun createTabs(languageString: String): List<LanguageTab> {
    if (languageString.equals("all", ignoreCase = true)) {
      return listOf(
        LanguageTab(
          languageCode = null,
          displayName = context.getString(R.string.all_languages).uppercase()
        )
      )
    }
    val languageCodes = languageString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    return if (languageCodes.isEmpty()) {
      val appLang = getAppChosenLanguageCode()
      if (appLang.isNotEmpty()) {
        listOf(
          LanguageTab(
            languageCode = appLang,
            displayName = getDisplayLanguage(appLang).uppercase()
          )
        )
      } else {
        listOf(
          LanguageTab(
            languageCode = null,
            displayName = context.getString(R.string.all_languages).uppercase()
          )
        )
      }
    } else {
      languageCodes.map { code ->
        LanguageTab(
          languageCode = code,
          displayName = getDisplayLanguage(code).uppercase()
        )
      }
    }
  }

  suspend fun handleFiltersChanged(
    category: String,
    language: String,
    searchQuery: String,
    currentTabs: List<LanguageTab>,
    currentSelectedTabIndex: Int,
    currentRequest: OnlineLibraryRequest
  ): FiltersChangedResult {
    val newTabs = createTabs(language)
    val currentTabCode = currentTabs.getOrNull(currentSelectedTabIndex)?.languageCode
    val newIndex = newTabs.indexOfFirst { it.languageCode == currentTabCode }.coerceAtLeast(ZERO)

    val activeLanguage = newTabs.getOrNull(newIndex)?.languageCode.orEmpty()
    val activeCategory = category.takeUnless { it.isBlank() }.orEmpty()

    val newRequest = OnlineLibraryRequest(
      searchQuery.takeIf { it.isNotBlank() }.orEmpty(),
      activeCategory,
      activeLanguage,
      false,
      ZERO
    )

    val requestToTrigger = if (newRequest.query != currentRequest.query ||
      newRequest.category != currentRequest.category ||
      newRequest.lang != currentRequest.lang
    ) {
      tabDataMap.clear()
      newRequest
    } else {
      null
    }

    return FiltersChangedResult(
      tabs = newTabs,
      selectedTabIndex = newIndex,
      newRequest = requestToTrigger
    )
  }

  fun selectTab(
    index: Int,
    tabs: List<LanguageTab>,
    currentRequest: OnlineLibraryRequest
  ): TabSelectedResult? {
    if (index !in tabs.indices) return null
    val tab = tabs[index]
    val tabKey = tab.languageCode.orEmpty()
    val existingData = tabDataMap[tabKey]
    return if (existingData != null && existingData.isLoaded) {
      TabSelectedResult.FromCache(
        books = existingData.books,
        totalPages = existingData.totalPages,
        currentPage = existingData.currentPage,
        isLoadingMore = existingData.isLoadingMore
      )
    } else {
      val newRequest = currentRequest.copy(
        lang = tab.languageCode.orEmpty(),
        page = ZERO,
        isLoadMoreItem = false
      )
      TabSelectedResult.FetchNeeded(newRequest)
    }
  }

  fun handleSuccessState(
    state: Success,
    currentSelectedTab: LanguageTab?,
    currentBooks: List<LibkiwixBook>
  ): SuccessResult {
    val request = state.request
    val tabKey = runCatching { request.lang.orEmpty() }.getOrDefault("")
    val page = runCatching { request.page }.getOrDefault(ZERO)
    val isLoadMore = runCatching { request.isLoadMoreItem }.getOrDefault(false)
    val existingData = tabDataMap[tabKey] ?: TabData(books = currentBooks)
    val updatedBooks = if (isLoadMore) {
      existingData.books.ifEmpty { currentBooks } + state.books
    } else {
      state.books
    }
    val updatedData = TabData(
      books = updatedBooks,
      totalPages = state.totalPages,
      currentPage = page,
      isLoadingMore = false,
      isLoaded = true
    )
    tabDataMap[tabKey] = updatedData

    val currentTabKey = currentSelectedTab?.languageCode.orEmpty()
    val isCurrentTab = (tabKey == currentTabKey || currentSelectedTab == null)

    return SuccessResult(
      updatedBooks = updatedBooks,
      totalPages = state.totalPages,
      isCurrentTab = isCurrentTab,
      isLoadMore = isLoadMore
    )
  }

  fun handleLoadMore(
    count: Int,
    currentTab: LanguageTab?,
    fallbackTotalPages: Int,
    currentRequest: OnlineLibraryRequest,
    isCurrentlyLoadingMore: Boolean
  ): OnlineLibraryRequest? {
    val tabKey = currentTab?.languageCode.orEmpty()
    val existingData = tabDataMap[tabKey]
    if (isCurrentlyLoadingMore || existingData?.isLoadingMore == true) return null
    val totalPagesForTab = if (existingData != null && existingData.totalPages > 0) {
      existingData.totalPages
    } else {
      fallbackTotalPages
    }
    val currentPage = if (count > ZERO) (count - ONE) / ITEMS_PER_PAGE else ZERO
    val nextPage = currentPage + ONE
    return if (nextPage < totalPagesForTab) {
      existingData?.let { tabDataMap[tabKey] = it.copy(isLoadingMore = true) }
      currentRequest.copy(
        lang = currentTab?.languageCode,
        page = nextPage,
        isLoadMoreItem = true
      )
    } else {
      null
    }
  }

  suspend fun resolveLanguageForRequest(
    currentTab: LanguageTab?,
    configuredLanguage: String
  ): String =
    currentTab?.languageCode
      ?: if (configuredLanguage.equals("all", ignoreCase = true)) {
        ""
      } else {
        configuredLanguage.split(",")
          .map { it.trim() }.firstOrNull { it.isNotBlank() }
          ?: getAppChosenLanguageCode()
      }

  fun clear() {
    tabDataMap.clear()
  }
}
