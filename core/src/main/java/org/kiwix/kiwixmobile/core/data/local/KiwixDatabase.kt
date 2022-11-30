/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.data.local

import android.content.Context
import android.util.Log
import com.yahoo.squidb.android.AndroidOpenHelper
import com.yahoo.squidb.data.ISQLiteDatabase
import com.yahoo.squidb.data.ISQLiteOpenHelper
import com.yahoo.squidb.data.SquidDatabase
import com.yahoo.squidb.sql.Table
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.core.dao.NewLanguagesDao
import org.kiwix.kiwixmobile.core.dao.NewRecentSearchDao
import org.kiwix.kiwixmobile.core.data.local.dao.BookDao
import org.kiwix.kiwixmobile.core.data.local.dao.BookmarksDao
import org.kiwix.kiwixmobile.core.data.local.dao.NetworkLanguageDao
import org.kiwix.kiwixmobile.core.data.local.dao.RecentSearchDao
import org.kiwix.kiwixmobile.core.data.local.entity.BookDatabaseEntity
import org.kiwix.kiwixmobile.core.data.local.entity.Bookmark
import org.kiwix.kiwixmobile.core.data.local.entity.LibraryDatabaseEntity
import org.kiwix.kiwixmobile.core.data.local.entity.NetworkLanguageDatabaseEntity
import org.kiwix.kiwixmobile.core.data.local.entity.RecentSearch
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.UpdateUtils
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Deprecated("") // delete once migrations are no longer needed
open class KiwixDatabase @Inject constructor(
  private val context: Context,
  private val bookDao: NewBookDao?,
  private val languagesDao: NewLanguagesDao?,
  private val bookmarksDao: NewBookmarksDao?,
  private val recentSearchDao: NewRecentSearchDao?
) : SquidDatabase() {

  override fun getName(): String = "Kiwix.db"

  override fun getTables(): Array<Table> =
    arrayOf(
      RecentSearch.TABLE,
      Bookmark.TABLE,
      BookDatabaseEntity.TABLE,
      NetworkLanguageDatabaseEntity.TABLE
    )

  override fun createOpenHelper(
    databaseName: String,
    delegate: OpenHelperDelegate,
    version: Int
  ): ISQLiteOpenHelper = AndroidOpenHelper(context, databaseName, delegate, version)

  @Suppress(
    "LongMethod",
    "ComplexMethod",
    "NestedBlockDepth",
    "TooGenericExceptionCaught",
    "MagicNumber"
  )
  override fun onUpgrade(db: ISQLiteDatabase, oldVersion: Int, newVersion: Int): Boolean {
    Log.e("UPGRADE", "oldversion: $oldVersion newVersion: $newVersion")
    when (oldVersion) {
      1, 2 -> {
        db.execSQL("DROP TABLE IF EXISTS recents")
        db.execSQL("DROP TABLE IF EXISTS recentsearches")
        tryCreateTable(RecentSearch.TABLE)
      }
      3 -> {
        tryCreateTable(Bookmark.TABLE)
      }
      4 -> {
        db.execSQL("DROP TABLE IF EXISTS book")
        tryCreateTable(BookDatabaseEntity.TABLE)
      }
      5 -> {
        db.execSQL("DROP TABLE IF EXISTS Bookmarks")
        tryCreateTable(Bookmark.TABLE)
        migrateBookmarksVersion6()
      }
      6 -> {
        db.execSQL("DROP TABLE IF EXISTS recents")
        db.execSQL("DROP TABLE IF EXISTS recentsearches")
        tryCreateTable(RecentSearch.TABLE)
      }
      7 -> {
        db.execSQL("DROP TABLE IF EXISTS recents")
        db.execSQL("DROP TABLE IF EXISTS recentsearches")
        tryCreateTable(RecentSearch.TABLE)
      }
      8 -> {
        db.execSQL("DROP TABLE IF EXISTS book")
        tryCreateTable(BookDatabaseEntity.TABLE)
      }
      9 -> {
        tryCreateTable(NetworkLanguageDatabaseEntity.TABLE)
      }
      10 -> {
        db.execSQL("DROP TABLE IF EXISTS recentSearches")
        tryCreateTable(RecentSearch.TABLE)
      }
      11 -> {
        tryAddColumn(BookDatabaseEntity.REMOTE_URL)
      }
      12 -> {
        tryAddColumn(BookDatabaseEntity.NAME)
        tryAddColumn(Bookmark.ZIM_NAME)
      }
      13 -> {
        tryDropTable(BookDatabaseEntity.TABLE)
        tryCreateTable(BookDatabaseEntity.TABLE)
      }
      14, 15 -> {
        try {
          bookDao?.migrationInsert(BookDao(this).books)
        } catch (e: java.lang.Exception) {
          e.printStackTrace()
        }
        try {
          languagesDao?.insert(NetworkLanguageDao(this).filteredLanguages)
        } catch (e: java.lang.Exception) {
          e.printStackTrace()
        }
        tryDropTable(BookDatabaseEntity.TABLE)
        tryDropTable(NetworkLanguageDatabaseEntity.TABLE)
        tryDropTable(LibraryDatabaseEntity.TABLE)
      }
      TWO_POINT_FIVE_POINT_THREE -> {
        try {
          val oldBookmarksDao = BookmarksDao(this)
          oldBookmarksDao.processBookmark(UpdateUtils::reformatProviderUrl)
          bookDao?.let {
            bookmarksDao?.migrationInsert(oldBookmarksDao.bookmarks, it)
          }
        } catch (e: Exception) {
          e.printStackTrace()
        }
        tryDropTable(Bookmark.TABLE)
        try {
          recentSearchDao?.migrationInsert(RecentSearchDao(this).getRecentSearches())
        } catch (e: Exception) {
          e.printStackTrace()
        }
        tryDropTable(RecentSearch.TABLE)
      }
    }
    return true
  }

  override fun getVersion(): Int = FINAL

  @Suppress("NestedBlockDepth", "MagicNumber")
  fun migrateBookmarksVersion6() {
    context.fileList()
      .asSequence()
      .filter { it.length == 40 && it.endsWith(".txt") }
      .forEach { id ->
        try {
          val idName = id.substring(0, id.length - 4)
          val stream: InputStream? = context.openFileInput(id)
          var bookMarkTitle: String?
          if (stream != null) {
            val read = BufferedReader(InputStreamReader(stream))
            while (read.readLine().also { bookMarkTitle = it } != null) {
              val bookmark = Bookmark()
              bookmark.setBookmarkUrl("null")
                .setBookmarkTitle(bookMarkTitle!!)
                .setZimId(idName).zimName = idName
              persist(bookmark)
            }
            context.deleteFile(id)
          }
        } catch (e: FileNotFoundException) {
          Log.e(TAG_KIWIX, "Bookmark File ( $id ) not found", e)
          // Surface to user
        } catch (e: IOException) {
          Log.e(TAG_KIWIX, "Can not read file $id", e)
          // Surface to user
        }
      }
  }

  /* Now that the database is no longer used
   * we need to make a migration happen with an explicit call
   */
  fun forceMigration() {
    beginTransaction()
    endTransaction()
  }

  companion object {
    private const val TWO_POINT_FIVE_POINT_THREE = 16
    private const val FINAL = 17 // 3.0.0
  }
}
