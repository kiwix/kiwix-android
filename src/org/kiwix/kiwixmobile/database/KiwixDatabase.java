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

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.data.adapter.SQLiteDatabaseWrapper;
import com.yahoo.squidb.sql.Order;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.Table;

import org.kiwix.kiwixmobile.database.entity.BookDataSource;
import org.kiwix.kiwixmobile.database.entity.BookDatabaseEntity;
import org.kiwix.kiwixmobile.database.entity.LibraryDatabaseEntity;
import org.kiwix.kiwixmobile.database.entity.RecentSearch;
import org.kiwix.kiwixmobile.database.entity.RecentSearchSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KiwixDatabase extends SquidDatabase {

  private static final int VERSION = 3;

  public KiwixDatabase(Context context) {
    super(context);
  }

  @Override public String getName() {
    return "Kiwix.db";
  }

  @Override
  protected Table[] getTables() {
    return new Table[] {
        BookDatabaseEntity.TABLE,
        LibraryDatabaseEntity.TABLE,
        RecentSearch.TABLE
    };
  }

  @Override protected boolean onUpgrade(SQLiteDatabaseWrapper db, int oldVersion, int newVersion) {
    if (newVersion >= 3) {
        db.execSQL("DROP TABLE IF EXISTS recents");
        tryCreateTable(RecentSearch.TABLE);
    }
    return true;
  }

  @Override
  protected int getVersion() {
    return VERSION;
  }

}
