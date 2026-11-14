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

package org.kiwix.kiwixmobile.core.utils.files

import android.content.Context
import android.content.ContextWrapper
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.mhutti1.utils.storage.StorageDeviceUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.dao.HistoryRoomDao
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.dao.NotesRoomDao
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Moves books that older versions kept in app-specific storage of the primary volume (deleted
 * on uninstall) to Documents/Kiwix. Moves only when the user accepts; SD card books stay.
 */
@Suppress("LongParameterList")
class LegacyBooksMover @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val kiwixDataStore: KiwixDataStore,
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk,
  private val libkiwixBookmarks: LibkiwixBookmarks,
  private val historyRoomDao: HistoryRoomDao,
  private val notesRoomDao: NotesRoomDao,
  @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  data class Offer(val books: List<BookOnDisk>, val totalBytes: Long)

  data class Result(val moved: Int, val failed: Int)

  /** Books worth moving, or null when there are none or Documents is not usable. */
  suspend fun offer(): Offer? = withContext(ioDispatcher) {
    if (documentsDirectory() == null || kiwixDataStore.legacyBooksMoveDeclined.first()) {
      return@withContext null
    }
    val books = legacyBooks()
    books.takeIf(List<BookOnDisk>::isNotEmpty)
      ?.let { Offer(it, it.sumOf { book -> parts(book.file()).sumOf(File::length) }) }
  }

  /** Points new downloads at Documents when the old default holds no books to move. */
  suspend fun useDocumentsIfNothingToMove() = withContext(ioDispatcher) {
    val documents = documentsDirectory() ?: return@withContext
    if (isLegacy(File(kiwixDataStore.selectedStorage.first())) && legacyBooks().isEmpty()) {
      kiwixDataStore.setSelectedStorage(documents.path)
    }
  }

  suspend fun decline() = kiwixDataStore.setLegacyBooksMoveDeclined(true)

  suspend fun move(offer: Offer, onProgress: suspend (Int, Int) -> Unit = { _, _ -> }): Result =
    withContext(ioDispatcher) {
      val documents = documentsDirectory() ?: return@withContext Result(0, offer.books.size)
      if (isLegacy(File(kiwixDataStore.selectedStorage.first()))) {
        // New downloads follow the books.
        kiwixDataStore.setSelectedStorage(documents.path)
      }
      val target = File(documents, KIWIX_DIRECTORY).apply { mkdirs() }
      var moved = 0
      offer.books.forEachIndexed { index, book ->
        onProgress(index + 1, offer.books.size)
        // A started book is finished even if the screen goes away.
        runCatching { withContext(NonCancellable) { moveBook(book, target) } }
          .onSuccess { moved++ }
          .onFailure { Log.e(TAG, "Could not move ${book.book.title}: $it") }
      }
      Result(moved, offer.books.size - moved)
    }

  private suspend fun moveBook(book: BookOnDisk, target: File) {
    val source = book.file()
    val oldLocation = source.canonicalPath
    val sourceParts = parts(source)
    val targetParts = sourceParts.map { File(target, it.name) }
    if (targetParts.any(File::exists)) throw IOException("Already exists in $target")
    val done = mutableListOf<Pair<File, File>>()
    try {
      sourceParts.zip(targetParts).forEach { (from, to) ->
        moveFile(from, to)
        done += from to to
      }
    } catch (e: IOException) {
      // Leave the book where it was.
      done.forEach { (from, to) -> if (!from.exists()) to.renameTo(from) else to.delete() }
      throw e
    }
    val newLocation = targetParts.first().canonicalPath
    libkiwixBookOnDisk.delete(book.book.id)
    libkiwixBookOnDisk.insertFile(newLocation)
    libkiwixBookmarks.relocateBook(book.book.id, File(newLocation))
    historyRoomDao.relocateBook(oldLocation, newLocation)
    notesRoomDao.relocateBook(oldLocation, newLocation)
    if (kiwixDataStore.currentZimFile.first() == oldLocation) {
      kiwixDataStore.setCurrentZimFile(newLocation)
    }
    sourceParts.forEach(File::delete)
  }

  /** Renames when possible; otherwise copies, keeping the source until the book is registered. */
  private fun moveFile(from: File, to: File) {
    if (from.renameTo(to)) return
    runCatching { from.copyTo(to) }
      .onFailure { to.delete() }
      .getOrThrow()
    if (to.length() != from.length()) {
      to.delete()
      throw IOException("Copied ${to.length()} of ${from.length()} bytes")
    }
  }

  private suspend fun legacyBooks(): List<BookOnDisk> =
    libkiwixBookOnDisk.getBooks().filter { book ->
      book.zimReaderSource.file?.let { it.isFile && isLegacy(it) } == true
    }

  private fun BookOnDisk.file(): File =
    zimReaderSource.file ?: throw IOException("Not a file book")

  private fun parts(file: File): List<File> =
    if (FileUtils.isSplittedZimFile(file.name)) {
      val prefix = file.name.dropLast(SPLIT_SUFFIX_LENGTH)
      file.parentFile?.listFiles { sibling ->
        sibling.name.startsWith(prefix) && sibling.name.length == file.name.length
      }?.sortedBy(File::getName).orEmpty().ifEmpty { listOf(file) }
    } else {
      listOf(file)
    }

  private fun isLegacy(file: File): Boolean {
    val path = runCatching { file.canonicalPath }.getOrDefault(file.path)
    return legacyRoots().any { root -> path == root || path.startsWith("$root/") }
  }

  private fun legacyRoots(): List<String> =
    listOfNotNull(
      ContextWrapper(context).externalMediaDirs.firstOrNull(),
      context.getExternalFilesDir(null),
      context.filesDir
    ).mapNotNull { runCatching { it.canonicalPath }.getOrNull() }

  private fun documentsDirectory(): File? = StorageDeviceUtils.publicDocumentsDirectory(context)

  companion object {
    private const val TAG = "LegacyBooksMover"
    private const val SPLIT_SUFFIX_LENGTH = 2
    const val KIWIX_DIRECTORY = "Kiwix"
  }
}
