/*
 * Copyright 2016 Isaac Hutt <mhutti1@gmail.com>
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

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import org.kiwix.kiwixmobile.database.entity.NetworkLanguageDatabaseEntity;
import org.kiwix.kiwixmobile.library.LibraryRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkLanguageDao {
  private KiwixDatabase mDb;

  public NetworkLanguageDao(KiwixDatabase kiwikDatabase) {
    this.mDb = kiwikDatabase;
  }

  public ArrayList<LibraryRecyclerViewAdapter.Language> getFilteredLanguages() {
    SquidCursor<NetworkLanguageDatabaseEntity> languageCursor = mDb.query(
        NetworkLanguageDatabaseEntity.class,
        Query.select());
    ArrayList<LibraryRecyclerViewAdapter.Language> result = new ArrayList<>();
    try {
      while (languageCursor.moveToNext()) {
        String languageCode = languageCursor.get(NetworkLanguageDatabaseEntity.LANGUAGE_I_S_O_3);
        boolean enabled = languageCursor.get(NetworkLanguageDatabaseEntity.ENABLED);
        result.add(new LibraryRecyclerViewAdapter.Language(languageCode, enabled));
      }
    } finally {
      languageCursor.close();
    }
    return result;
  }

  public void saveFilteredLanguages(List<LibraryRecyclerViewAdapter.Language> languages){
    mDb.deleteAll(NetworkLanguageDatabaseEntity.class);
    Collections.sort(languages, (language, t1) -> language.language.compareTo(t1.language));
    for (LibraryRecyclerViewAdapter.Language language : languages){
      NetworkLanguageDatabaseEntity networkLanguageDatabaseEntity = new NetworkLanguageDatabaseEntity();
      networkLanguageDatabaseEntity.setLanguageISO3(language.languageCode);
      networkLanguageDatabaseEntity.setIsEnabled(language.active);
      mDb.persist(networkLanguageDatabaseEntity);
    }
  }
}
