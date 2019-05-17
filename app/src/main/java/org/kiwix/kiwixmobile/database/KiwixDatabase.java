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

package org.kiwix.kiwixmobile.database;

import android.content.Context;
import android.util.Log;

import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.data.adapter.SQLiteDatabaseWrapper;
import com.yahoo.squidb.sql.Table;

import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.database.entity.BookDatabaseEntity;
import org.kiwix.kiwixmobile.database.entity.Bookmarks;
import org.kiwix.kiwixmobile.database.entity.LibraryDatabaseEntity;
import org.kiwix.kiwixmobile.database.entity.NetworkLanguageDatabaseEntity;
import org.kiwix.kiwixmobile.database.entity.RecentSearch;
import org.kiwix.kiwixmobile.utils.UpdateUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.kiwix.kiwixmobile.utils.Constants.TAG_KIWIX;

@Singleton
public class KiwixDatabase extends SquidDatabase {

  private static final int VERSION = 15;
  private Context context;

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
    return new Table[]{
        BookDatabaseEntity.TABLE,
        LibraryDatabaseEntity.TABLE,
        RecentSearch.TABLE,
        Bookmarks.TABLE,
        NetworkLanguageDatabaseEntity.TABLE
    };
  }

  @Override
  protected boolean onUpgrade(SQLiteDatabaseWrapper db, int oldVersion, int newVersion) {
    if (newVersion >= 3 && oldVersion < 3) {
      db.execSQL("DROP TABLE IF EXISTS recents");
      tryCreateTable(RecentSearch.TABLE);
    }
    if (newVersion >= 3 && (oldVersion < 3 || oldVersion == 7 || oldVersion == 6)) {
      db.execSQL("DROP TABLE IF EXISTS recents");
      db.execSQL("DROP TABLE IF EXISTS recentsearches");
      tryCreateTable(RecentSearch.TABLE);
    }
    if (newVersion >= 3) {
      tryCreateTable(RecentSearch.TABLE);
    }
    if (newVersion >= 4) {
      tryCreateTable(Bookmarks.TABLE);
    }
    if (newVersion >= 5 && oldVersion < 5) {
      db.execSQL("DROP TABLE IF EXISTS book");
      tryCreateTable(BookDatabaseEntity.TABLE);
    }
    if (newVersion >= 5) {
      tryCreateTable(BookDatabaseEntity.TABLE);
    }
    if (newVersion >= 6 && oldVersion < 6) {
      db.execSQL("DROP TABLE IF EXISTS Bookmarks");
      tryCreateTable(Bookmarks.TABLE);
      migrateBookmarks();
    }
    if (newVersion >= 6) {
      tryCreateTable(Bookmarks.TABLE);
    }
    if (newVersion >= 9) {
      db.execSQL("DROP TABLE IF EXISTS book");
      tryCreateTable(BookDatabaseEntity.TABLE);
    }
    if (newVersion >= 10) {
      tryCreateTable(NetworkLanguageDatabaseEntity.TABLE);
    }
    if (newVersion >= 11 && oldVersion < 11) {
      db.execSQL("DROP TABLE IF EXISTS recentSearches");
      tryCreateTable(RecentSearch.TABLE);
    }
    if (newVersion >= 11) {
      tryCreateTable(RecentSearch.TABLE);
    }
    if (newVersion >= 12) {
      tryAddColumn(BookDatabaseEntity.REMOTE_URL);
    }
    if (newVersion >= 13) {
      tryAddColumn(BookDatabaseEntity.NAME);
      tryAddColumn(Bookmarks.ZIM_NAME);
    }
    if (newVersion >= 14 && oldVersion < 14) {
      tryDropTable(BookDatabaseEntity.TABLE);
      tryCreateTable(BookDatabaseEntity.TABLE);
    }
    if (newVersion >= 15 && oldVersion < 15) {
      reformatBookmarks();
    }
    return true;
  }

  @Override
  protected int getVersion() {
    return VERSION;
  }

  public void migrateBookmarks() {
    BookmarksDao bookmarksDao = new BookmarksDao(this);

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
              bookmarksDao.saveBookmark(null, in, idName, idName);
            }
            context.deleteFile(id);
            Log.d(TAG_KIWIX, "Switched to bookmarkfile " + ZimContentProvider.getId());
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

  // Reformat bookmark urls to use correct provider
  private void reformatBookmarks() {
    BookmarksDao bookmarksDao = new BookmarksDao(this);
    bookmarksDao.processBookmark(UpdateUtils::reformatProviderUrl);
  }
}

