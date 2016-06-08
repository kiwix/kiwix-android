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
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.data.adapter.SQLiteDatabaseWrapper;
import com.yahoo.squidb.sql.Table;
import org.kiwix.kiwixmobile.database.entity.BookDatabaseEntity;
import org.kiwix.kiwixmobile.database.entity.LibraryDatabaseEntity;

public class KiwixDatabase extends SquidDatabase {

  private static final int VERSION = 1;

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
        LibraryDatabaseEntity.TABLE
    };
  }

  @Override protected boolean onUpgrade(SQLiteDatabaseWrapper db, int oldVersion, int newVersion) {
    return false;
  }

  @Override
  protected int getVersion() {
    return VERSION;
  }
}
