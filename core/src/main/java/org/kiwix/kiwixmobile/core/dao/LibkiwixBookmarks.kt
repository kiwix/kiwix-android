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
import android.os.Environment
import android.util.Base64
import io.reactivex.BackpressureStrategy
import io.reactivex.BackpressureStrategy.LATEST
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.rxSingle
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.DarkModeConfig
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isCustomApp
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.reader.ILLUSTRATION_SIZE
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.libkiwix.Book
import org.kiwix.libkiwix.Bookmark
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Manager
import org.kiwix.libzim.Archive
import org.kiwix.libzim.SuggestionSearcher
import java.io.File
import javax.inject.Inject

class LibkiwixBookmarks @Inject constructor(
  val library: Library,
  manager: Manager,
  val sharedPreferenceUtil: SharedPreferenceUtil,
  private val bookDao: NewBookDao,
  private val zimReaderContainer: ZimReaderContainer?
) : PageDao {

  /**
   * Request new data from Libkiwix when changes occur inside it; otherwise,
   * return the previous data to avoid unnecessary data load on Libkiwix.
   */
  private var bookmarksChanged: Boolean = false
  private var bookmarkList: List<LibkiwixBookmarkItem> = arrayListOf()
  private var libraryBooksList: List<String> = arrayListOf()

  @Suppress("CheckResult")
  private val bookmarkListBehaviour: BehaviorSubject<List<LibkiwixBookmarkItem>>? by lazy {
    BehaviorSubject.create<List<LibkiwixBookmarkItem>>().also { subject ->
      rxSingle { getBookmarksList() }
        .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
        .subscribe(subject::onNext, subject::onError)
    }
  }

  private val bookmarksFolderPath: String by lazy {
    if (Build.DEVICE.contains("generic")) {
      // Workaround for emulators: Emulators have limited memory and
      // restrictions on creating folders, so we will use the default
      // path for saving the bookmark file.
      sharedPreferenceUtil.context.filesDir.path
    } else {
      "${sharedPreferenceUtil.defaultStorage()}/Bookmarks/"
    }
  }

  private val bookmarkFile: File by lazy {
    File("$bookmarksFolderPath/bookmark.xml")
  }

  private val libraryFile: File by lazy {
    File("$bookmarksFolderPath/library.xml")
  }

  init {
    // Check if bookmark folder exist if not then create the folder first.
    if (!File(bookmarksFolderPath).isFileExist()) File(bookmarksFolderPath).mkdir()
    // Check if library file exist if not then create the file to save the library with book information.
    if (!libraryFile.isFileExist()) libraryFile.createNewFile()
    // set up manager to read the library from this file
    manager.readFile(libraryFile.canonicalPath)
    // Check if bookmark file exist if not then create the file to save the bookmarks.
    if (!bookmarkFile.isFileExist()) bookmarkFile.createNewFile()
    // set up manager to read the bookmarks from this file
    manager.readBookmarkFile(bookmarkFile.canonicalPath)
  }

  fun bookmarks(): Flowable<List<Page>> =
    flowableBookmarkList()
      .map { it }

  override fun pages(): Flowable<List<Page>> = bookmarks()

  override fun deletePages(pagesToDelete: List<Page>) =
    deleteBookmarks(pagesToDelete as List<LibkiwixBookmarkItem>)

  suspend fun getCurrentZimBookmarksUrl(zimFileReader: ZimFileReader?): List<String> {
    return zimFileReader?.let { reader ->
      getBookmarksList()
        .filter { it.zimId == reader.id }
        .map(LibkiwixBookmarkItem::bookmarkUrl)
    } ?: emptyList()
  }

  fun bookmarkUrlsForCurrentBook(zimFileReader: ZimFileReader): Flowable<List<String>> =
    flowableBookmarkList()
      .map { bookmarksList ->
        bookmarksList.filter { it.zimId == zimFileReader.id }
          .map(LibkiwixBookmarkItem::bookmarkUrl)
      }
      .subscribeOn(Schedulers.io())

  /**
   * Saves bookmarks in libkiwix. The use of `shouldWriteBookmarkToFile` is primarily
   * during data migration, where data is written to the file only once after all bookmarks
   * have been added to libkiwix to optimize the process.
   */
  suspend fun saveBookmark(
    libkiwixBookmarkItem: LibkiwixBookmarkItem,
    shouldWriteBookmarkToFile: Boolean = true
  ) {
    if (!isBookMarkExist(libkiwixBookmarkItem)) {
      addBookToLibraryIfNotExist(libkiwixBookmarkItem.libKiwixBook)
      val bookmark = Bookmark().apply {
        bookId = libkiwixBookmarkItem.zimId
        title = libkiwixBookmarkItem.title
        url = libkiwixBookmarkItem.url
        bookTitle = when {
          libkiwixBookmarkItem.libKiwixBook?.title != null ->
            libkiwixBookmarkItem.libKiwixBook.title

          libkiwixBookmarkItem.zimName.isNotBlank() -> libkiwixBookmarkItem.zimName
          else -> libkiwixBookmarkItem.zimId
        }
      }
      library.addBookmark(bookmark).also {
        if (shouldWriteBookmarkToFile) {
          writeBookMarksAndSaveLibraryToFile()
          updateFlowableBookmarkList()
        }
        // dispose the bookmark
        bookmark.dispose()
      }
    }
  }

  suspend fun addBookToLibrary(file: File? = null, archive: Archive? = null) {
    try {
      bookmarksChanged = true
      val book = Book().apply {
        archive?.let {
          update(archive)
        } ?: run {
          update(Archive(file?.canonicalPath))
        }
      }
      addBookToLibraryIfNotExist(book)
      updateFlowableBookmarkList()
    } catch (ignore: Exception) {
      Log.e(
        TAG,
        "Error: Couldn't add the book to library.\nOriginal exception = $ignore"
      )
    }
  }

  private fun addBookToLibraryIfNotExist(libKiwixBook: Book?) {
    libKiwixBook?.let { book ->
      if (!isBookAlreadyExistInLibrary(book.id)) {
        library.addBook(libKiwixBook).also {
          // now library has changed so update our library list.
          libraryBooksList = library.booksIds.toList()
          Log.d(
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

  fun deleteBookmarks(bookmarks: List<LibkiwixBookmarkItem>) {
    bookmarks.map { library.removeBookmark(it.zimId, it.bookmarkUrl) }
      .also {
        CoroutineScope(Dispatchers.IO).launch {
          writeBookMarksAndSaveLibraryToFile()
          updateFlowableBookmarkList()
        }
      }
  }

  fun deleteBookmark(bookId: String, bookmarkUrl: String) {
    deleteBookmarks(listOf(LibkiwixBookmarkItem(zimId = bookId, bookmarkUrl = bookmarkUrl)))
  }

  /**
   * Asynchronously writes the library and bookmarks data to their respective files in a background thread
   * to prevent potential data loss and ensures that the library holds the updated ZIM file paths and favicons.
   */
  private suspend fun writeBookMarksAndSaveLibraryToFile() {
    // Save the library, which contains ZIM file paths and favicons, to a file.
    library.writeToFile(libraryFile.canonicalPath)

    // Save the bookmarks data to a separate file.
    library.writeBookmarksToFile(bookmarkFile.canonicalPath)
    // set the bookmark change to true so that libkiwix will return the new data.
    bookmarksChanged = true
  }

  @Suppress("ReturnCount")
  private suspend fun getBookmarksList(): List<LibkiwixBookmarkItem> {
    if (!bookmarksChanged && bookmarkList.isNotEmpty()) {
      // No changes, return the cached data
      return bookmarkList.distinctBy(LibkiwixBookmarkItem::bookmarkUrl)
    }
    // Retrieve the list of bookmarks from the library, or return an empty list if it's null.
    val bookmarkArray =
      library.getBookmarks(false)?.toList()
        ?: return bookmarkList.distinctBy(LibkiwixBookmarkItem::bookmarkUrl)

    // Create a list to store LibkiwixBookmarkItem objects.
    bookmarkList = bookmarkArray.mapNotNull { bookmark ->
      // Check if the library contains the book associated with the bookmark.
      val book = if (isBookAlreadyExistInLibrary(bookmark.bookId)) {
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

      // Check if the book has an illustration of the specified size and encode it to Base64.
      val favicon = book?.getIllustration(ILLUSTRATION_SIZE)?.data?.let {
        Base64.encodeToString(it, Base64.DEFAULT)
      }

      val zimReaderSource = book?.path?.let { ZimReaderSource(File(it)) }
      // Return the LibkiwixBookmarkItem, filtering out null results.
      return@mapNotNull LibkiwixBookmarkItem(
        bookmark,
        favicon,
        zimReaderSource
      ).also {
        // set the bookmark change to false to avoid reloading the data from libkiwix
        bookmarksChanged = false
      }
    }

    // Delete duplicates bookmarks if any exist
    deleteDuplicateBookmarks()

    return bookmarkList.distinctBy(LibkiwixBookmarkItem::bookmarkUrl)
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
          val coreApp = sharedPreferenceUtil.context as CoreApp
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
            DarkModeConfig(sharedPreferenceUtil, sharedPreferenceUtil.context),
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

  private fun flowableBookmarkList(
    backpressureStrategy: BackpressureStrategy = LATEST
  ): Flowable<List<LibkiwixBookmarkItem>> {
    return Flowable.create({ emitter ->
      val disposable = bookmarkListBehaviour?.subscribe(
        { list ->
          if (!emitter.isCancelled) {
            emitter.onNext(list.toList())
          }
        },
        emitter::onError,
        emitter::onComplete
      )

      emitter.setDisposable(disposable)
    }, backpressureStrategy)
  }

  private suspend fun updateFlowableBookmarkList() {
    bookmarkListBehaviour?.onNext(getBookmarksList())
  }

  // Export the `bookmark.xml` file to the `Download/org.kiwix/` directory of internal storage.
  fun exportBookmark() {
    try {
      val bookmarkDestinationFile = exportedFile("bookmark.xml")
      bookmarkFile.inputStream().use { inputStream ->
        bookmarkDestinationFile.outputStream().use(inputStream::copyTo)
      }
      sharedPreferenceUtil.context.toast(
        sharedPreferenceUtil.context.getString(
          R.string.export_bookmark_saved,
          bookmarkDestinationFile.name
        )
      )
    } catch (ignore: Exception) {
      Log.e(TAG, "Error: bookmark couldn't export.\n Original exception = $ignore")
      sharedPreferenceUtil.context.toast(R.string.export_bookmark_error)
    }
  }

  private fun exportedFile(fileName: String): File {
    val rootFolder = File(
      "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}" +
        "/org.kiwix"
    )
    if (!rootFolder.isFileExist()) rootFolder.mkdir()
    return sequence {
      yield(File(rootFolder, fileName))
      yieldAll(
        generateSequence(1) { it + 1 }.map {
          File(
            rootFolder, fileName.replace(".", "_$it.")
          )
        }
      )
    }.first { !it.isFileExist() }
  }

  suspend fun importBookmarks(bookmarkFile: File) {
    // Create a temporary library manager to import the bookmarks.
    val tempLibrary = Library()
    Manager(tempLibrary).apply {
      // Read the bookmark file.
      readBookmarkFile(bookmarkFile.canonicalPath)
    }
    // Add the ZIM files to the library for validating the bookmarks.
    bookDao.getBooks().forEach {
      addBookToLibrary(file = it.zimReaderSource.file)
    }
    // Save the imported bookmarks to the current library.
    tempLibrary.getBookmarks(false)?.toList()?.forEach {
      saveBookmark(LibkiwixBookmarkItem(it, null, null))
    }
    sharedPreferenceUtil.context.toast(R.string.bookmark_imported_message)

    if (bookmarkFile.exists()) {
      bookmarkFile.deleteFile()
    }
  }

  companion object {
    const val TAG = "LibkiwixBookmark"
  }
}
