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

import javax.inject.Inject;
import org.kiwix.kiwixmobile.database.entity.NetworkLanguageDatabaseEntity;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.library.LibraryAdapter.Language;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NetworkLanguageDao {
  private KiwixDatabase mDb;

  @Inject
  public NetworkLanguageDao(KiwixDatabase kiwikDatabase) {
    this.mDb = kiwikDatabase;
  }

  public ArrayList<LibraryAdapter.Language> getFilteredLanguages() {
    SquidCursor<NetworkLanguageDatabaseEntity> languageCursor = mDb.query(
        NetworkLanguageDatabaseEntity.class,
        Query.select());
    ArrayList<LibraryAdapter.Language> result = new ArrayList<>();
    try {
      while (languageCursor.moveToNext()) {
        String languageCode = languageCursor.get(NetworkLanguageDatabaseEntity.LANGUAGE_I_S_O_3);
        boolean enabled = languageCursor.get(NetworkLanguageDatabaseEntity.ENABLED);
        result.add(new LibraryAdapter.Language(languageCode, enabled));
      }
    } finally {
      languageCursor.close();
    }
    return result;
  }

  public void saveFilteredLanguages(List<Language> languages){
    mDb.deleteAll(NetworkLanguageDatabaseEntity.class);
    Collections.sort(languages, (language, t1) -> language.language.compareTo(t1.language));
    for (LibraryAdapter.Language language : languages){
      NetworkLanguageDatabaseEntity networkLanguageDatabaseEntity = new NetworkLanguageDatabaseEntity();
      networkLanguageDatabaseEntity.setLanguageISO3(language.languageCode);
      networkLanguageDatabaseEntity.setIsEnabled(language.active);
      mDb.persist(networkLanguageDatabaseEntity);
    }
  }
}
