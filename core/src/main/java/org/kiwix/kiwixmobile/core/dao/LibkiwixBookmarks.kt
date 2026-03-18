/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.di.modules.BOOKMARK_LIBRARY
import org.kiwix.kiwixmobile.core.di.modules.BOOKMARK_MANAGER
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isCustomApp
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.extensions.getFavicon
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.bookmark.models.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.EXPORT_BOOK_MARK_PATH
import org.kiwix.libkiwix.Book
import org.kiwix.libkiwix.Bookmark
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import org.kiwix.libzim.Archive
import org.kiwix.libzim.SuggestionSearcher
import java.io.File
import javax.inject.Inject
import javax.inject.Named

class LibkiwixBookmarks @Inject constructor(
  @Named(BOOKMARK_LIBRARY) private val library: Library,
  @Named(BOOKMARK_MANAGER) private val manager: Manager,
  private val kiwixDataStore: KiwixDataStore,
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk,
  private val zimReaderContainer: ZimReaderContainer?
) : PageDao {
  /**
   * Request new data from Libkiwix when changes occur inside it; otherwise,
   * return the previous data to avoid unnecessary data load on Libkiwix.
   */
  private var bookmarksChanged: Boolean = false
  private var bookmarkList: List<LibkiwixBookmarkItem> = arrayListOf()
  private var libraryBooksList: List<String> = arrayListOf()
  private val initMutex = Mutex()
  private var initialized: Boolean = false

  @Suppress("InjectDispatcher")
  private val bookmarkListFlow: MutableStateFlow<List<LibkiwixBookmarkItem>> by lazy {
    MutableStateFlow<List<LibkiwixBookmarkItem>>(emptyList()).also { flow ->
      CoroutineScope(Dispatchers.IO).launch {
        runCatching {
          val bookmarks = getBookmarksList()
          flow.emit(bookmarks)
        }.onFailure { it.printStackTrace() }
      }
    }
  }

  private suspend fun bookmarksFolderPath(): String =
    if (Build.DEVICE.contains("generic")) {
      // Workaround for emulators: Emulators have limited memory and
      // restrictions on creating folders, so we will use the default
      // path for saving the bookmark file.
      kiwixDataStore.context.filesDir.path
    } else {
      "${kiwixDataStore.defaultStorage()}/Bookmarks/"
    }

  private suspend fun bookmarkFile(): File =
    File("${bookmarksFolderPath()}/bookmark.xml")

  private suspend fun libraryFile(): File =
    File("${bookmarksFolderPath()}/library.xml")

  /**
   * Ensure initialization runs once. This method performs all file I/O and manager setup
   * on Dispatchers.IO so it won't block the main thread. It is safe to call multiple times.
   */
  private suspend fun ensureInitialized(dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    if (initialized) return

    initMutex.withLock {
      if (initialized) return
      withContext(dispatcher) {
        // Check if bookmark folder exist if not then create the folder first.
        if (!File(bookmarksFolderPath()).isFileExist()) File(bookmarksFolderPath()).mkdir()
        // Check if library file exist if not then create the file to save the library with book information.
        if (!libraryFile().isFileExist()) libraryFile().createNewFile()
        // set up manager to read the library from this file
        manager.readFile(libraryFile().canonicalPath)
        // Check if bookmark file exist if not then create the file to save the bookmarks.
        if (!bookmarkFile().isFileExist()) bookmarkFile().createNewFile()
        // set up manager to read the bookmarks from this file
        manager.readBookmarkFile(bookmarkFile().canonicalPath)
        initialized = true
      }
    }
  }

  fun bookmarks(): Flow<List<Page>> =
    bookmarkListFlow
      .map { it }

  override fun pages(): Flow<List<Page>> = bookmarks()

  override fun deletePages(pagesToDelete: List<Page>) =
    deleteBookmarks(pagesToDelete as List<LibkiwixBookmarkItem>)

  @Suppress("InjectDispatcher")
  suspend fun getCurrentZimBookmarksUrl(zimFileReader: ZimFileReader?): List<String> {
    ensureInitialized()
    return withContext(Dispatchers.IO) {
      zimFileReader?.let { reader ->
        getBookmarksList()
          .filter { it.zimId == reader.id }
          .map(LibkiwixBookmarkItem::bookmarkUrl)
      }.orEmpty()
    }
  }

  @Suppress("InjectDispatcher")
  fun bookmarkUrlsForCurrentBook(zimId: String): Flow<List<String>> =
    bookmarkListFlow
      .map { bookmarksList ->
        bookmarksList.filter { it.zimId == zimId }
          .map(LibkiwixBookmarkItem::bookmarkUrl)
      }.flowOn(Dispatchers.IO)

  /**
   * Saves bookmarks in libkiwix. The use of `shouldWriteBookmarkToFile` is primarily
   * during data migration, where data is written to the file only once after all bookmarks
   * have been added to libkiwix to optimize the process.
   */
  suspend fun saveBookmark(
    libkiwixBookmarkItem: LibkiwixBookmarkItem,
    shouldWriteBookmarkToFile: Boolean = true
  ) {
    ensureInitialized()
    if (!isBookMarkExist(libkiwixBookmarkItem)) {
      addBookToLibraryIfNotExist(libkiwixBookmarkItem.libKiwixBook)
      val bookmark =
        Bookmark().apply {
          bookId = libkiwixBookmarkItem.zimId
          title = libkiwixBookmarkItem.title
          url = libkiwixBookmarkItem.url
          bookTitle =
            when {
              libkiwixBookmarkItem.libKiwixBook?.title != null ->
                libkiwixBookmarkItem.libKiwixBook.title

              libkiwixBookmarkItem.zimName.isNotBlank() -> libkiwixBookmarkItem.zimName
              else -> libkiwixBookmarkItem.zimId
            }
        }
      library.addBookmark(bookmark).also {
        if (shouldWriteBookmarkToFile) {
          writeBookMarksAndSaveLibraryToFile()
          updateFlowBookmarkList()
        }
        // dispose the bookmark
        bookmark.dispose()
      }
    }
  }

  suspend fun addBookToLibrary(file: File? = null, archive: Archive? = null) {
    ensureInitialized()
    runCatching {
      bookmarksChanged = true
      val book =
        Book().apply {
          archive?.let {
            update(archive)
          } ?: run {
            update(Archive(file?.canonicalPath))
          }
        }
      addBookToLibraryIfNotExist(book)
      updateFlowBookmarkList()
    }.onFailure {
      Log.e(
        TAG,
        "Error: Couldn't add the book to library.\nOriginal exception = $it"
      )
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

  fun deleteBookmarks(
    bookmarks: List<LibkiwixBookmarkItem>,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
  ) {
    CoroutineScope(dispatcher).launch {
      ensureInitialized()
      bookmarks.map { library.removeBookmark(it.zimId, it.bookmarkUrl) }
      writeBookMarksAndSaveLibraryToFile()
      updateFlowBookmarkList()
      removeBookFromLibraryIfNoRelatedBookmarksAreStored(dispatcher, bookmarks)
    }
  }

  fun deleteBookmark(bookId: String, bookmarkUrl: String) {
    deleteBookmarks(listOf(LibkiwixBookmarkItem(zimId = bookId, bookmarkUrl = bookmarkUrl)))
  }

  /**
   * Removes books from the library that no longer have any associated bookmarks.
   *
   * This function checks if any of the books associated with the given deleted bookmarks
   * are still referenced by other existing bookmarks. If not, those books are removed from the library.
   *
   * @param dispatcher The coroutine dispatcher to run the operation on (typically Dispatchers.IO).
   * @param deletedBookmarks The list of bookmarks that were just deleted.
   */
  private suspend fun removeBookFromLibraryIfNoRelatedBookmarksAreStored(
    dispatcher: CoroutineDispatcher,
    deletedBookmarks: List<LibkiwixBookmarkItem>
  ) {
    ensureInitialized()
    withContext(dispatcher) {
      val currentBookmarks = getBookmarksList()
      val deletedZimIds = deletedBookmarks.map { it.zimId }.distinct()

      deletedZimIds.forEach { zimId ->
        val stillExists = currentBookmarks.any { it.zimId == zimId }
        if (!stillExists) {
          library.removeBookById(zimId)
          libraryBooksList = library.booksIds.toList()
          Log.d(TAG, "Removed book from library since no bookmarks exist for: $zimId")
        }
      }
    }
    writeBookMarksAndSaveLibraryToFile()
  }

  /**
   * Asynchronously writes the library and bookmarks data to their respective files in a background thread
   * to prevent potential data loss and ensures that the library holds the updated ZIM file paths and favicons.
   */
  private suspend fun writeBookMarksAndSaveLibraryToFile() {
    ensureInitialized()
    // Save the library, which contains ZIM file paths and favicons, to a file.
    library.writeToFile(libraryFile().canonicalPath)

    // Save the bookmarks data to a separate file.
    library.writeBookmarksToFile(bookmarkFile().canonicalPath)
    // set the bookmark change to true so that libkiwix will return the new data.
    bookmarksChanged = true
  }

  @Suppress("ReturnCount")
  private suspend fun getBookmarksList(): List<LibkiwixBookmarkItem> {
    ensureInitialized()
    if (!bookmarksChanged && bookmarkList.isNotEmpty()) {
      // No changes, return the cached data
      return bookmarkList.distinctBy(LibkiwixBookmarkItem::bookmarkUrl)
    }
    // Retrieve the list of bookmarks from the library, or return an empty list if it's null.
    val bookmarkArray =
      library.getBookmarks(false)?.toList()
        ?: return bookmarkList.distinctBy(LibkiwixBookmarkItem::bookmarkUrl)

    // Create a list to store LibkiwixBookmarkItem objects.
    bookmarkList =
      bookmarkArray.mapNotNull { bookmark ->
        // Check if the library contains the book associated with the bookmark.
        val book =
          runCatching {
            if (isBookAlreadyExistInLibrary(bookmark.bookId)) {
              library.getBookById(bookmark.bookId)
            } else {
              Log.d(
                TAG,
                "Library does not contain the book for this bookmark:\n" +
                  "Book Title: ${bookmark.bookTitle}\n" +
                  "Bookmark URL: ${bookmark.url}"
              )
              null
            }
          }.getOrNull()

        // Check if the book has an illustration of the specified size and encode it to Base64.
        val favicon = book?.getFavicon()

        val zimReaderSource = book?.path?.let { ZimReaderSource(File(it)) }
        // Return the LibkiwixBookmarkItem, filtering out null results.
        return@mapNotNull LibkiwixBookmarkItem(
          bookmark,
          favicon,
          zimReaderSource
        )
      }
    bookmarksChanged = false

    // Delete duplicates bookmarks if any exist
    deleteDuplicateBookmarks()

    return bookmarkList.distinctBy { it.zimId to it.bookmarkUrl }
  }

  @Suppress("NestedBlockDepth")
  private suspend fun deleteDuplicateBookmarks() {
    bookmarkList.groupBy { it.bookmarkUrl to it.zimReaderSource }
      .filter { it.value.size > 1 }
      .forEach { (_, value) ->
        value.drop(1).forEach { bookmarkItem ->
          deleteBookmark(bookmarkItem.zimId, bookmarkItem.bookmarkUrl)
        }
      }
    // Fixes #3890
    bookmarkList.groupBy { it.title to it.zimReaderSource }
      .filter { it.value.size > 1 }
      .forEach { (_, value) ->
        value.forEach { bookmarkItem ->
          // This is a special case where two urls have the same title in a zim file.
          val coreApp = kiwixDataStore.context as CoreApp
          val zimFileReader = getZimFileReaderFromBookmark(bookmarkItem, coreApp)
          // get the redirect entry so that we can delete the other bookmark.
          zimFileReader?.getPageUrlFrom(bookmarkItem.title)?.let {
            // check if the bookmark url is not equals to redirect entry,
            // then delete the duplicate bookmark. It will keep the original bookmark.
            if (it != bookmarkItem.bookmarkUrl) {
              deleteBookmark(bookmarkItem.zimId, bookmarkItem.bookmarkUrl)
            }
          }
          if (!coreApp.getMainActivity().isCustomApp()) zimFileReader?.dispose()
        }
      }
  }

  private suspend fun getZimFileReaderFromBookmark(
    bookmarkItem: LibkiwixBookmarkItem,
    coreApp: CoreApp
  ): ZimFileReader? {
    return if (coreApp.getMainActivity().isCustomApp()) {
      // in custom apps we are using the assetFileDescriptor so we do not have the filePath
      // and in custom apps there is only a single zim file so we are directly
      // getting the zimFileReader object.
      zimReaderContainer?.zimFileReader
    } else {
      bookmarkItem.zimReaderSource?.let {
        it.createArchive()?.let { archive ->
          ZimFileReader(
            it,
            archive,
            SuggestionSearcher(archive)
          )
        }
      }
    }
  }

  private suspend fun isBookMarkExist(libkiwixBookmarkItem: LibkiwixBookmarkItem): Boolean =
    getBookmarksList()
      .any {
        it.url == libkiwixBookmarkItem.bookmarkUrl &&
          it.zimReaderSource == libkiwixBookmarkItem.zimReaderSource
      }

  private suspend fun updateFlowBookmarkList() {
    bookmarkListFlow.emit(getBookmarksList())
  }

  // Export the `bookmark.xml` file to the `Download/org.kiwix/` directory of internal storage.
  suspend fun exportBookmark() {
    ensureInitialized()
    try {
      val bookmarkDestinationFile = exportedFile("bookmark.xml")
      bookmarkFile().inputStream().use { inputStream ->
        bookmarkDestinationFile.outputStream().use(inputStream::copyTo)
      }
      kiwixDataStore.context.toast(
        kiwixDataStore.context.getString(
          R.string.export_bookmark_saved,
          bookmarkDestinationFile.name
        )
      )
    } catch (ignore: Exception) {
      Log.e(TAG, "Error: bookmark couldn't export.\n Original exception = $ignore")
      kiwixDataStore.context.toast(R.string.export_bookmark_error)
    }
  }

  private suspend fun exportedFile(fileName: String): File {
    val rootFolder = File(EXPORT_BOOK_MARK_PATH)
    if (!rootFolder.isFileExist()) rootFolder.mkdir()
    return sequence {
      yield(File(rootFolder, fileName))
      yieldAll(
        generateSequence(1) { it + 1 }.map {
          File(
            rootFolder,
            fileName.replace(".", "_$it.")
          )
        }
      )
    }.first { !it.isFileExist() }
  }

  suspend fun importBookmarks(bookmarkFile: File) {
    ensureInitialized()
    // Create a temporary library manager to import the bookmarks.
    val tempLibrary = Library()
    Manager(tempLibrary).apply {
      // Read the bookmark file.
      readBookmarkFile(bookmarkFile.canonicalPath)
    }
    // Add the ZIM files to the library for validating the bookmarks.
    libkiwixBookOnDisk.getBooks().forEach {
      addBookToLibrary(file = it.zimReaderSource.file)
    }
    // Save the imported bookmarks to the current library.
    tempLibrary.getBookmarks(false)?.toList()?.forEach {
      saveBookmark(LibkiwixBookmarkItem(it, null, null))
    }
    kiwixDataStore.context.toast(R.string.bookmark_imported_message)

    if (bookmarkFile.exists()) {
      bookmarkFile.deleteFile()
    }
  }

  companion object {
    const val TAG = "LibkiwixBookmark"
  }
}
