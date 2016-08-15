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

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.data.adapter.SQLiteDatabaseWrapper;
import com.yahoo.squidb.sql.Order;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.Table;

import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.database.entity.BookDataSource;
import org.kiwix.kiwixmobile.database.entity.BookDatabaseEntity;
import org.kiwix.kiwixmobile.database.entity.Bookmarks;
import org.kiwix.kiwixmobile.database.entity.LibraryDatabaseEntity;
import org.kiwix.kiwixmobile.database.entity.RecentSearch;
import org.kiwix.kiwixmobile.database.entity.RecentSearchSpec;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KiwixDatabase extends SquidDatabase {

  private static final int VERSION = 8;
  private Context context;


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
        Bookmarks.TABLE
    };
  }

  @Override
  protected boolean onUpgrade(SQLiteDatabaseWrapper db, int oldVersion, int newVersion) {
    if (newVersion >= 3 && oldVersion < 3) {
      db.execSQL("DROP TABLE IF EXISTS recents");
      tryCreateTable(RecentSearch.TABLE);
    }
    if (newVersion >= 3 && (oldVersion < 3 || oldVersion == 7)) {
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
      String[] ids = context.fileList();
      for (String id : ids) {
        if (id.length() == 40 && id.substring(id.length() - 4).equals(".txt")) {
          migrateBookmarks(id.substring(0, id.length() - 4));
          Log.d(KiwixMobileActivity.TAG_KIWIX, "migrated " + id);
        }
      }
    }
    if (newVersion >= 6) {
      tryCreateTable(Bookmarks.TABLE);
    }
    return true;
  }

  @Override
  protected int getVersion() {
    return VERSION;
  }

  public void migrateBookmarks(String id) {
    BookmarksDao bookmarksDao = new BookmarksDao(this);
    try {
      InputStream stream = context.openFileInput(id + ".txt");
      String in;
      if (stream != null) {
        BufferedReader read = new BufferedReader(new InputStreamReader(stream));
        while ((in = read.readLine()) != null) {
          bookmarksDao.saveBookmark(null, in, id);
        }
        context.deleteFile(id + ".txt");
        Log.d(KiwixMobileActivity.TAG_KIWIX, "Switched to bookmarkfile " + ZimContentProvider.getId());
      }
    } catch (FileNotFoundException e) {
      Log.e(KiwixMobileActivity.TAG_KIWIX, "File not found: " + e.toString());
    } catch (IOException e) {
      Log.e(KiwixMobileActivity.TAG_KIWIX, "Can not read file: " + e.toString());
    }
  }
}


