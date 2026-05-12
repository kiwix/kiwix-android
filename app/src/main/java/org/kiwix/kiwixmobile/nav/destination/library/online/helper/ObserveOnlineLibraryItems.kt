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
      kiwixDataStore.selectedOnlineContentLanguage
    ) { books, activeDownloads, remoteBooks, fsState, lang ->
      observeLibraryItems(
        booksOnFileSystem = books,
        activeDownloads = activeDownloads,
        remoteBooks = remoteBooks,
        fileSystemState = fsState,
        selectedLanguage = lang,
        getString = getString,
        getSimpleString = getSimpleString
      )
    }.flowOn(ioDispatcher)
  }

  private fun observeLibraryItems(
    booksOnFileSystem: List<Book>,
    activeDownloads: List<DownloadModel>,
    remoteBooks: List<LibkiwixBook>,
    fileSystemState: Fat32Checker.FileSystemState,
    selectedLanguage: String,
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

    val sectionTitle =
      if (selectedLanguage.isBlank()) {
        getSimpleString(R.string.all_languages)
      } else {
        getString(
          R.string.your_language,
          arrayOf(selectedLanguage.convertToLocal().displayLanguage)
        )
      }

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
