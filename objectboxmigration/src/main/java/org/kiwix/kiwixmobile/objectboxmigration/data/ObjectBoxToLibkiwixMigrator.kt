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

package org.kiwix.kiwixmobile.objectboxmigration.data

import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.BooksOnDiskListItem.BookOnDisk
import org.kiwix.kiwixmobile.objectboxmigration.entities.BookOnDiskEntity
import org.kiwix.kiwixmobile.objectboxmigration.entities.BookmarkEntity
import org.kiwix.libkiwix.Book
import org.kiwix.libzim.Archive
import java.io.File
import javax.inject.Inject

class ObjectBoxToLibkiwixMigrator {
  @Inject
  lateinit var boxStore: BoxStore

  @Inject
  lateinit var sharedPreferenceUtil: SharedPreferenceUtil

  @Inject
  lateinit var libkiwixBookmarks: LibkiwixBookmarks

  @Inject
  lateinit var libkiwixBookOnDisk: LibkiwixBookOnDisk
  private val migrationMutex = Mutex()

  suspend fun migrateObjectBoxDataToLibkiwix() {
    // CoreApp.coreComponent.inject(this)
    if (!sharedPreferenceUtil.prefIsBookmarksMigrated) {
      migrateBookMarks(boxStore.boxFor())
    }
    if (!sharedPreferenceUtil.prefIsBookOnDiskMigrated) {
      migrateLocalBooks(boxStore.boxFor())
    }
    // TODO we will migrate here for other entities
  }

  @Suppress("Deprecation")
  suspend fun migrateLocalBooks(box: Box<BookOnDiskEntity>) {
    val bookOnDiskList = box.all.map { bookOnDiskEntity ->
      bookOnDiskEntity.file.let { file ->
        // set zimReaderSource for previously saved books(before we introduced the zimReaderSource)
        val zimReaderSource = ZimReaderSource(file)
        if (zimReaderSource.canOpenInLibkiwix()) {
          bookOnDiskEntity.zimReaderSource = zimReaderSource
        }
      }
      BookOnDisk(
        databaseId = bookOnDiskEntity.id,
        file = bookOnDiskEntity.file,
        book = bookOnDiskEntity.toBook(),
        zimReaderSource = bookOnDiskEntity.zimReaderSource
      )
    }
    migrationMutex.withLock {
      runCatching {
        val libkiwixBooks = bookOnDiskList.map {
          val archive = Archive(it.zimReaderSource.toDatabase())
          Book().apply {
            update(archive)
          }
        }
        libkiwixBookOnDisk.insert(libkiwixBooks)
      }.onFailure {
        Log.e(
          "MIGRATING_BOOK_ON_DISK",
          "there is an error while migrating the bookOnDisk \n" +
            "Original exception is = $it"
        )
      }
    }
    sharedPreferenceUtil.putPrefBookOnDiskMigrated(true)
  }

  suspend fun migrateBookMarks(box: Box<BookmarkEntity>) {
    val bookMarksList = box.all
    // run migration with mutex to do the migration one by one.
    migrationMutex.withLock {
      bookMarksList.forEachIndexed { index, bookmarkEntity ->
        // moving this to handle the exceptions thrown by the libkiwix if any occur,
        // like if path is not validate due to user move the ZIM file to another location etc.
        try {
          // for saving book to library, otherwise it does not save the
          // favicon and zimFilePath in library.
          var archive: Archive? = null
          val libkiwixBook =
            bookmarkEntity.zimFilePath?.let {
              if (File(it).exists()) {
                archive = Archive(bookmarkEntity.zimFilePath)
                Book().apply {
                  update(archive)
                }
              } else {
                // Migrate bookmarks even if the file does not exist in the file system,
                // to display them on the bookmark screen.
                null
              }
            } ?: run {
              // for migrating bookmarks for recent custom apps since in recent version of
              // custom app we are using the `assetFileDescriptor` which does not have the filePath.
              null
            }
          libkiwixBookmarks.saveBookmark(
            LibkiwixBookmarkItem(
              zimId = bookmarkEntity.zimId,
              zimFilePath = bookmarkEntity.zimReaderSource?.toDatabase(),
              zimReaderSource = bookmarkEntity.zimReaderSource,
              zimName = bookmarkEntity.zimName,
              bookmarkUrl = bookmarkEntity.bookmarkUrl,
              title = bookmarkEntity.bookmarkTitle,
              favicon = bookmarkEntity.favicon,
              libKiwixBook = libkiwixBook
            ),
            shouldWriteBookmarkToFile = index == bookMarksList.size - 1
          )
          archive?.dispose()
          // TODO should we remove data from objectBox?
          // removing the single entity from the object box after migration.
          // box.query {
          //   equal(
          //     BookmarkEntity_.bookmarkUrl,
          //     bookmarkEntity.bookmarkUrl,
          //     QueryBuilder.StringOrder.CASE_INSENSITIVE
          //   )
          // }.remove()
        } catch (ignore: Exception) {
          Log.e(
            "MIGRATING_BOOKMARKS",
            "there is an error while migrating the bookmark for\n" +
              " ZIM file = ${bookmarkEntity.zimFilePath} \n" +
              "Bookmark Title = ${bookmarkEntity.bookmarkTitle} \n" +
              "Original exception is = $ignore"
          )
        }
      }
    }
    sharedPreferenceUtil.putPrefBookMarkMigrated(true)
  }
}
