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

package org.kiwix.kiwixmobile.data.local.dao;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;
import java.util.ArrayList;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.data.local.entity.NetworkLanguageDatabaseEntity;
import org.kiwix.kiwixmobile.zim_manager.Language;

@Deprecated
public class NetworkLanguageDao {
  private KiwixDatabase mDb;

  @Inject
  public NetworkLanguageDao(KiwixDatabase kiwixDatabase) {
    this.mDb = kiwixDatabase;
  }

  public ArrayList<Language> getFilteredLanguages() {
    ArrayList<Language> result = new ArrayList<>();
    try (SquidCursor<NetworkLanguageDatabaseEntity> languageCursor = mDb.query(
        NetworkLanguageDatabaseEntity.class,
        Query.select())) {
      while (languageCursor.moveToNext()) {
        String languageCode = languageCursor.get(NetworkLanguageDatabaseEntity.LANGUAGE_I_S_O_3);
        boolean enabled = languageCursor.get(NetworkLanguageDatabaseEntity.ENABLED);
        result.add(new Language(languageCode, enabled, 0));
      }
    }
    return result;
  }
}
