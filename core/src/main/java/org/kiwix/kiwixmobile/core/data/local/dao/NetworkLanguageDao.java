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

package org.kiwix.kiwixmobile.core.data.local.dao;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;
import java.util.ArrayList;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.core.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.core.data.local.entity.NetworkLanguageDatabaseEntity;
import org.kiwix.kiwixmobile.core.zim_manager.Language;

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
