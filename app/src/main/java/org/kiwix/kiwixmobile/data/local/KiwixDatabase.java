/*
 * Copyright 2016
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.data.local;

import android.content.Context;
import android.util.Log;
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.data.adapter.SQLiteDatabaseWrapper;
import com.yahoo.squidb.sql.Table;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.data.local.dao.BookDao;
import org.kiwix.kiwixmobile.data.local.dao.BookmarksDao;
import org.kiwix.kiwixmobile.data.local.entity.BookDatabaseEntity;
import org.kiwix.kiwixmobile.data.local.entity.Bookmark;
import org.kiwix.kiwixmobile.data.local.entity.History;
import org.kiwix.kiwixmobile.data.local.entity.LibraryDatabaseEntity;
import org.kiwix.kiwixmobile.data.local.entity.NetworkLanguageDatabaseEntity;
import org.kiwix.kiwixmobile.data.local.entity.RecentSearch;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.utils.UpdateUtils;

import static org.kiwix.kiwixmobile.utils.Constants.TAG_KIWIX;

@Singleton
public class KiwixDatabase extends SquidDatabase {

  private static final int VERSION = 17;
  private final Context context;

  @Inject
  public KiwixDatabase(Context context) {
    super(context);
    this.context = context;
  }

  @Override
  public String getName() {
    return "Kiwix.db";
  }

  @Override
  protected Table[] getTables() {
    return new Table[] {
        BookDatabaseEntity.TABLE,
        LibraryDatabaseEntity.TABLE,
        RecentSearch.TABLE,
        Bookmark.TABLE,
        NetworkLanguageDatabaseEntity.TABLE,
        History.TABLE
    };
  }

  @Override
  protected boolean onUpgrade(SQLiteDatabaseWrapper db, int oldVersion, int newVersion) {
    switch (oldVersion) {
      case 1:
      case 2:
        db.execSQL("DROP TABLE IF EXISTS recents");
        db.execSQL("DROP TABLE IF EXISTS recentsearches");
        tryCreateTable(RecentSearch.TABLE);
      case 3:
        tryCreateTable(Bookmark.TABLE);
      case 4:
        db.execSQL("DROP TABLE IF EXISTS book");
        tryCreateTable(BookDatabaseEntity.TABLE);
      case 5:
        db.execSQL("DROP TABLE IF EXISTS Bookmarks");
        tryCreateTable(Bookmark.TABLE);
        migrateBookmarksVersion6();
      case 6:
        db.execSQL("DROP TABLE IF EXISTS recents");
        db.execSQL("DROP TABLE IF EXISTS recentsearches");
        tryCreateTable(RecentSearch.TABLE);
      case 7:
        db.execSQL("DROP TABLE IF EXISTS recents");
        db.execSQL("DROP TABLE IF EXISTS recentsearches");
        tryCreateTable(RecentSearch.TABLE);
      case 8:
        db.execSQL("DROP TABLE IF EXISTS book");
        tryCreateTable(BookDatabaseEntity.TABLE);
      case 9:
        tryCreateTable(NetworkLanguageDatabaseEntity.TABLE);
      case 10:
        db.execSQL("DROP TABLE IF EXISTS recentSearches");
        tryCreateTable(RecentSearch.TABLE);
      case 11:
        tryAddColumn(BookDatabaseEntity.REMOTE_URL);
      case 12:
        tryAddColumn(BookDatabaseEntity.NAME);
        tryAddColumn(Bookmark.ZIM_NAME);
      case 13:
        tryDropTable(BookDatabaseEntity.TABLE);
        tryCreateTable(BookDatabaseEntity.TABLE);
      case 14:
        tryCreateTable(History.TABLE);
      case 15:
        tryAddColumn(Bookmark.ZIM_FILE_PATH);
        tryAddColumn(Bookmark.FAVICON);
        migrateBookmarksVersion16();
      case 16:
        new BookmarksDao(this).processBookmark(UpdateUtils::reformatProviderUrl);
    }
    return true;
  }

  private void migrateBookmarksVersion16() {
    BookmarksDao bookmarksDao = new BookmarksDao(this);
    BookDao bookDao = new BookDao(this);
    List<Bookmark> bookmarks = bookmarksDao.getBookmarks(false);
    List<LibraryNetworkEntity.Book> books = bookDao.getBooks();
    for (Bookmark bookmark : bookmarks) {
      if (bookmark.getZimId() != null) {
        for (LibraryNetworkEntity.Book book : books) {
          if (bookmark.getZimId().equals(book.getId())) {
            bookmark.setZimFilePath(book.getUrl()).setFavicon(book.getFavicon());
            bookmarksDao.saveBookmark(bookmark);
            break;
          }
        }
      }
    }
  }

  @Override
  protected int getVersion() {
    return VERSION;
  }

  public void migrateBookmarksVersion6() {
    String[] ids = context.fileList();
    for (String id : ids) {
      if (id.length() == 40 && id.substring(id.length() - 4).equals(".txt")) {
        try {
          String idName = id.substring(0, id.length() - 4);
          InputStream stream = context.openFileInput(id);
          String in;
          if (stream != null) {
            BufferedReader read = new BufferedReader(new InputStreamReader(stream));
            while ((in = read.readLine()) != null) {
              Bookmark bookmark = new Bookmark();
              bookmark.setBookmarkUrl("null")
                  .setBookmarkTitle(in)
                  .setZimId(idName)
                  .setZimName(idName);
              persist(bookmark);
            }
            context.deleteFile(id);
            Log.d(TAG_KIWIX, "Switched to bookmark file " + ZimContentProvider.getId());
          }
        } catch (FileNotFoundException e) {
          Log.e(TAG_KIWIX, "Bookmark File ( " + id + " ) not found", e);
          //TODO: Surface to user
        } catch (IOException e) {
          Log.e(TAG_KIWIX, "Can not read file " + id, e);
          //TODO: Surface to user
        }
      }
    }
  }
}


