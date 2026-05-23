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

package org.kiwix.kiwixmobile.nav.destination.library.online.helper

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import org.kiwix.kiwixmobile.R as AppR
import org.kiwix.kiwixmobile.core.R

import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.downloader.model.DownloadModel
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.zimManager.Fat32Checker
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.BookItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.DividerItem
import org.kiwix.kiwixmobile.zimManager.libraryView.LibraryListItem.LibraryDownloadItem
import org.kiwix.libkiwix.Book
import javax.inject.Inject

class ObserveOnlineLibraryItems @Inject constructor(
  private val kiwixDataStore: KiwixDataStore,
  private val fat32Checker: Fat32Checker,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  operator fun invoke(
    localBooks: Flow<List<Book>>,
    downloads: Flow<List<DownloadModel>>,
    networkBooks: Flow<List<LibkiwixBook>>,
    getString: (Int, Array<out Any>) -> String,
    getSimpleString: (Int) -> String,
    getDisplayLanguage: (String) -> String
  ): Flow<List<LibraryListItem>> {
    return combine(
      localBooks,
      downloads,
      networkBooks,
      fat32Checker.fileSystemStates,
      combine(
        kiwixDataStore.selectedOnlineContentLanguage,
        kiwixDataStore.selectedOnlineContentCategory
      ) { lang, cat -> Selection(lang, cat) }
    ) { booksOnFileSystem, activeDownloads, remoteBooks, fileSystemState, selection ->
      observeLibraryItems(
        booksOnFileSystem = booksOnFileSystem,
        activeDownloads = activeDownloads,
        remoteBooks = remoteBooks,
        fileSystemState = fileSystemState,
        selection = selection,
        localizationHelper = LocalizationHelper(getString, getSimpleString, getDisplayLanguage)
      )
    }.flowOn(ioDispatcher)
  }

  private class LocalizationHelper(
    val getString: (Int, Array<out Any>) -> String,
    val getSimpleString: (Int) -> String,
    val getDisplayLanguage: (String) -> String
  )

  private data class Selection(val language: String, val category: String)

  private fun observeLibraryItems(
    booksOnFileSystem: List<Book>,
    activeDownloads: List<DownloadModel>,
    remoteBooks: List<LibkiwixBook>,
    fileSystemState: FileSystemState,
    selection: Selection,
    localizationHelper: LocalizationHelper
  ): List<LibraryListItem> {
    val localBookIds = booksOnFileSystem.map { it.id }.toSet()
    val activeDownloadsById = activeDownloads.associateBy { it.book.id }
    val activeDownloadingIds = activeDownloadsById.keys

    val allBooks = remoteBooks.filterNot { it.id in localBookIds }
    val allBooksById = allBooks.associateBy { it.id }
    val downloadingBooks = activeDownloads.map { download ->
      allBooksById[download.book.id] ?: download.book
    }
    val availableBooks = allBooks.filterNot { it.id in activeDownloadingIds }
    val sectionTitle = buildSectionTitle(selection, localizationHelper)
    return buildList {
      addAll(
        createLibrarySection(
          downloadingBooks,
          activeDownloadsById,
          fileSystemState,
          localizationHelper.getSimpleString(R.string.downloading),
          Long.MAX_VALUE
        )
      )

      addAll(
        createLibrarySection(
          availableBooks,
          emptyMap(),
          fileSystemState,
          sectionTitle,
          Long.MIN_VALUE
        )
      )
    }
  }

  private fun buildSectionTitle(
    selection: Selection,
    localizationHelper: LocalizationHelper
  ): String {
    val languages = selection.language.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    val categories = selection.category.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    val languagePart = when {
      languages.isEmpty() -> localizationHelper.getSimpleString(R.string.all_languages)
      languages.size > 1 -> {
        val joined = languages.joinToString(", ") {
          localizationHelper.getDisplayLanguage(it)
        }
        "${localizationHelper.getSimpleString(R.string.your_languages)} $joined"
      }

      else -> localizationHelper.getString(
        R.string.your_language,
        arrayOf(localizationHelper.getDisplayLanguage(languages.first()))
      )
    }

    val categoryPart = when {
      categories.isEmpty() -> localizationHelper.getSimpleString(AppR.string.all_categories)
      categories.size > 1 -> {
        val joined = categories.joinToString(", ")
        "${localizationHelper.getSimpleString(AppR.string.your_categories)} $joined"
      }

      else -> localizationHelper.getString(
        AppR.string.your_category,
        arrayOf(categories.first())
      )
    }

    return "$languagePart\n$categoryPart"
  }

  private fun createLibrarySection(
    books: List<LibkiwixBook>,
    downloadsById: Map<String, DownloadModel>,
    fileSystemState: FileSystemState,
    sectionTitle: String,
    sectionId: Long
  ): List<LibraryListItem> {
    if (books.isEmpty()) return emptyList()

    return buildList {
      add(DividerItem(sectionId, sectionTitle))
      addAll(books.asLibraryItems(downloadsById, fileSystemState))
    }
  }

  private fun List<LibkiwixBook>.asLibraryItems(
    downloadsById: Map<String, DownloadModel>,
    fileSystemState: FileSystemState
  ) = map { book ->
    downloadsById[book.id]
      ?.let(::LibraryDownloadItem)
      ?: BookItem(book, fileSystemState)
  }

  fun dispose() {
    fat32Checker.dispose()
  }
}
