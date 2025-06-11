/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.dao

import android.os.Build
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks.Companion.TAG
import org.kiwix.kiwixmobile.core.di.modules.LOCAL_BOOKS_LIBRARY
import org.kiwix.kiwixmobile.core.di.modules.LOCAL_BOOKS_MANAGER
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.libkiwix.Book
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class LibkiwixBookOnDisk @Inject constructor(
  @Named(LOCAL_BOOKS_LIBRARY) private val library: Library,
  @Named(LOCAL_BOOKS_MANAGER) private val manager: Manager,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) {
  private var libraryBooksList: List<String> = arrayListOf()
  private var localBooksList: List<LibkiwixBook> = arrayListOf()

  /**
   * Request new data from Libkiwix when changes occur inside it; otherwise,
   * return the previous data to avoid unnecessary data load on Libkiwix.
   */
  private var booksChanged: Boolean = false
  private val localBookFolderPath: String by lazy {
    if (Build.DEVICE.contains("generic")) {
      // Workaround for emulators: Emulators have limited memory and
      // restrictions on creating folders, so we will use the default
      // path for saving the bookmark file.
      sharedPreferenceUtil.context.filesDir.path
    } else {
      "${sharedPreferenceUtil.defaultStorage()}/ZIMFiles/"
    }
  }

  private val libraryFile: File by lazy {
    File("$localBookFolderPath/library.xml")
  }

  init {
    // Check if ZIM files folder exist if not then create the folder first.
    if (runBlocking { !File(localBookFolderPath).isFileExist() }) File(localBookFolderPath).mkdir()
    // Check if library file exist if not then create the file to save the library with book information.
    if (runBlocking { !libraryFile.isFileExist() }) libraryFile.createNewFile()
    // set up manager to read the library from this file
    manager.readFile(libraryFile.canonicalPath)
  }

  @Suppress("InjectDispatcher")
  private val localBooksFlow: MutableStateFlow<List<LibkiwixBook>> by lazy {
    MutableStateFlow<List<LibkiwixBook>>(emptyList()).also { flow ->
      CoroutineScope(Dispatchers.IO).launch {
        runCatching {
          flow.emit(getBooksList())
        }.onFailure { it.printStackTrace() }
      }
    }
  }

  private suspend fun getBooksList(dispatcher: CoroutineDispatcher = Dispatchers.IO): List<LibkiwixBook> =
    withContext(dispatcher) {
      if (!booksChanged && localBooksList.isNotEmpty()) {
        // No changes, return the cached data
        return@withContext localBooksList.distinctBy(LibkiwixBook::path)
      }
      // Retrieve the list of books from the library.
      val booksIds = library.booksIds.toList()

      // Create a list to store LibkiwixBook objects.
      localBooksList =
        booksIds.mapNotNull { bookId ->
          val book = library.getBookById(bookId)
          return@mapNotNull LibkiwixBook(book).also {
            // set the books change to false to avoid reloading the data from libkiwix
            booksChanged = false
          }
        }

      return@withContext localBooksList.distinctBy(LibkiwixBook::path)
    }

  @OptIn(ExperimentalCoroutinesApi::class)
  fun books(dispatcher: CoroutineDispatcher = Dispatchers.IO) =
    localBooksFlow
      .mapLatest { booksList ->
        removeBooksThatAreInTrashFolder(booksList)
        removeBooksThatDoNotExist(booksList.toMutableList())
        booksList.mapNotNull { book ->
          try {
            if (book.zimReaderSource.exists() &&
              !isInTrashFolder(book.zimReaderSource.toDatabase())
            ) {
              book
            } else {
              null
            }
          } catch (_: Exception) {
            null
          }
        }
      }
      .map { it.map(::BookOnDisk) }
      .flowOn(dispatcher)

  suspend fun getBooks() = getBooksList().map(::BookOnDisk)

  @Suppress("InjectDispatcher")
  suspend fun insert(libkiwixBooks: List<Book>) {
    withContext(Dispatchers.IO) {
      val existingBookIds = library.booksIds.toSet()
      val existingBookPaths = existingBookIds
        .mapNotNull { id -> library.getBookById(id)?.path }
        .toSet()

      val newBooks = libkiwixBooks.filterNot { book ->
        book.id in existingBookIds || book.path in existingBookPaths
      }
      newBooks.forEach { book ->
        runCatching {
          addBookToLibraryIfNotExist(book)
        }.onFailure {
          Log.e(TAG, "Failed to add book: ${book.title} - ${it.message}")
        }
      }

      if (newBooks.isNotEmpty()) {
        writeBookMarksAndSaveLibraryToFile()
        updateLocalBooksFlow()
      }
    }
  }

  private fun addBookToLibraryIfNotExist(libKiwixBook: Book?) {
    libKiwixBook?.let { book ->
      if (!isBookAlreadyExistInLibrary(book.id)) {
        library.addBook(libKiwixBook).also {
          // now library has changed so update our library list.
          libraryBooksList = library.booksIds.toList()
          Log.e(
            TAG,
            "Added Book to Library:\n" +
              "ZIM File Path: ${book.path}\n" +
              "Book ID: ${book.id}\n" +
              "Book Title: ${book.title}"
          )
        }
      }
    }
  }

  private fun isBookAlreadyExistInLibrary(bookId: String): Boolean {
    if (libraryBooksList.isEmpty()) {
      // store booksIds in a list to avoid multiple data call on libkiwix
      libraryBooksList = library.booksIds.toList()
    }
    return libraryBooksList.any { it == bookId }
  }

  private suspend fun removeBooksThatDoNotExist(books: MutableList<LibkiwixBook>) {
    delete(books.filterNot { it.zimReaderSource.exists() })
  }

  // Remove the existing books from database which are showing on the library screen.
  private suspend fun removeBooksThatAreInTrashFolder(books: List<LibkiwixBook>) {
    delete(books.filter { isInTrashFolder(it.zimReaderSource.toDatabase()) })
  }

  // Check if any existing ZIM file showing on the library screen which is inside the trash folder.
  private suspend fun isInTrashFolder(filePath: String) =
    Regex("/\\.Trash/").containsMatchIn(filePath)

  suspend fun delete(books: List<LibkiwixBook>) {
    runCatching {
      books.forEach {
        library.removeBookById(it.id)
      }
    }.onFailure { it.printStackTrace() }
    writeBookMarksAndSaveLibraryToFile()
    // TODO test when getting books it will not goes to circular dependencies mode.
    updateLocalBooksFlow()
  }

  suspend fun delete(bookId: String) {
    runCatching {
      library.removeBookById(bookId)
      writeBookMarksAndSaveLibraryToFile()
      updateLocalBooksFlow()
    }.onFailure { it.printStackTrace() }
  }

  suspend fun bookMatching(downloadTitle: String) =
    getBooks().firstOrNull {
      it.zimReaderSource.toDatabase().endsWith(downloadTitle, true)
    }

  /**
   * Asynchronously writes the library data to their respective file in a background thread
   * to prevent potential data loss and ensures that the library holds the updated ZIM file data.
   */
  private suspend fun writeBookMarksAndSaveLibraryToFile() {
    // Save the library, which contains ZIM file data.
    library.writeToFile(libraryFile.canonicalPath)
    // set the bookmark change to true so that libkiwix will return the new data.
    booksChanged = true
  }

  private suspend fun updateLocalBooksFlow() {
    localBooksFlow.emit(getBooksList())
  }
}
