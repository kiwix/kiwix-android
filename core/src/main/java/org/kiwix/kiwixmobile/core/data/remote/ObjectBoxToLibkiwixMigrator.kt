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

package org.kiwix.kiwixmobile.core.data.remote

import org.kiwix.kiwixmobile.core.utils.files.Log
import io.objectbox.Box
import io.objectbox.BoxStore
import io.objectbox.kotlin.boxFor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.dao.entities.BookmarkEntity
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.libkiwix.Book
import org.kiwix.libzim.Archive
import javax.inject.Inject

class ObjectBoxToLibkiwixMigrator {
  @Inject lateinit var boxStore: BoxStore
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  @Inject lateinit var libkiwixBookmarks: LibkiwixBookmarks
  private val migrationMutex = Mutex()

  suspend fun migrateBookmarksToLibkiwix() {
    CoreApp.coreComponent.inject(this)
    migrateBookMarks(boxStore.boxFor())
    // TODO we will migrate here for other entities
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
          val libkiwixBook = bookmarkEntity.zimFilePath?.let {
            archive = Archive(bookmarkEntity.zimFilePath)
            Book().apply {
              update(archive)
            }
          } ?: kotlin.run {
            // for migrating bookmarks for recent custom apps since in recent version of
            // custom app we are using the `assetFileDescriptor` which does not have the filePath.
            null
          }
          libkiwixBookmarks.saveBookmark(
            LibkiwixBookmarkItem(bookmarkEntity, libkiwixBook),
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
