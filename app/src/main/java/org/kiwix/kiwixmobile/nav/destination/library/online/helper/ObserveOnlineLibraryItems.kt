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
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.convertToLocal
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
    getSimpleString: (Int) -> String
  ): Flow<List<LibraryListItem>> {
    return combine(
      localBooks,
      downloads,
      networkBooks,
      fat32Checker.fileSystemStates,
      kiwixDataStore.selectedOnlineContentLanguage,
      kiwixDataStore.selectedOnlineContentCategory
    ) { args ->
      observeLibraryItems(
        booksOnFileSystem = args[0] as List<Book>,
        activeDownloads = args[1] as List<DownloadModel>,
        remoteBooks = args[2] as List<LibkiwixBook>,
        fileSystemState = args[3] as FileSystemState,
        selection = Selection(
          args[LANGUAGE_INDEX] as String,
          args[CATEGORY_INDEX] as String
        ),
        getString = getString,
        getSimpleString = getSimpleString
      )
    }.flowOn(ioDispatcher)
  }

  companion object {
    private const val LANGUAGE_INDEX = 4
    private const val CATEGORY_INDEX = 5
  }

  private data class Selection(val language: String, val category: String)

  private fun observeLibraryItems(
    booksOnFileSystem: List<Book>,
    activeDownloads: List<DownloadModel>,
    remoteBooks: List<LibkiwixBook>,
    fileSystemState: FileSystemState,
    selection: Selection,
    getString: (Int, Array<Any>) -> String,
    getSimpleString: (Int) -> String
  ): List<LibraryListItem> {
    val allBooks =
      remoteBooks - booksOnFileSystem.map { LibkiwixBook(it) }.toSet()

    val downloadingBooks =
      activeDownloads.map { download ->
        allBooks.firstOrNull { it.id == download.book.id } ?: download.book
      }

    val filteredBooks = allBooks - downloadingBooks.toSet()

    val sectionTitle = buildString {
      val languagePart = when {
        selection.language.isBlank() -> getSimpleString(R.string.all_languages)
        selection.language.contains(",") -> {
          val joinedLanguages = selection.language.split(",")
            .joinToString(", ") { it.trim().convertToLocal().displayLanguage }
          "${getSimpleString(R.string.your_languages)} $joinedLanguages"
        }

        else -> getString(
          R.string.your_language,
          arrayOf(selection.language.convertToLocal().displayLanguage)
        )
      }
      append(languagePart)

      val categoryPart = when {
        selection.category.isBlank() -> getSimpleString(R.string.all_categories)
        selection.category.contains(",") -> {
          val joinedCategories = selection.category.split(",")
            .joinToString(", ") { it.trim() }
          "${getSimpleString(R.string.your_categories)} $joinedCategories"
        }

        else -> getString(
          R.string.your_category,
          arrayOf(selection.category.trim())
        )
      }
      append("\n")
      append(categoryPart)
    }.toString()

    return createLibrarySection(
      downloadingBooks,
      activeDownloads,
      fileSystemState,
      getSimpleString(R.string.downloading),
      Long.MAX_VALUE
    ) +
      createLibrarySection(
        filteredBooks,
        emptyList(),
        fileSystemState,
        sectionTitle,
        Long.MIN_VALUE
      )
  }

  private fun createLibrarySection(
    books: List<LibkiwixBook>,
    activeDownloads: List<DownloadModel>,
    fileSystemState: FileSystemState,
    sectionTitle: String,
    sectionId: Long
  ) = if (books.isNotEmpty()) {
    listOf(DividerItem(sectionId, sectionTitle)) +
      books.asLibraryItems(activeDownloads, fileSystemState)
  } else {
    emptyList()
  }

  private fun List<LibkiwixBook>.asLibraryItems(
    activeDownloads: List<DownloadModel>,
    fileSystemState: FileSystemState
  ) = map { book ->
    activeDownloads.firstOrNull { download -> download.book == book }
      ?.let(::LibraryDownloadItem)
      ?: BookItem(book, fileSystemState)
  }

  fun dispose() {
    fat32Checker.dispose()
  }
}
