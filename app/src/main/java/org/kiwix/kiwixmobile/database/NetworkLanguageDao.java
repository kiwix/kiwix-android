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
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.kiwix.kiwixmobile.database.entity.NetworkLanguageDatabaseEntity;
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.Language;

public class NetworkLanguageDao extends BaseDao {

  private final BehaviorProcessor<List<Language>> allLanguageProcessor = BehaviorProcessor.create();

  @Inject
  public NetworkLanguageDao(KiwixDatabase kiwikDatabase) {
    super(kiwikDatabase, NetworkLanguageDatabaseEntity.TABLE);
  }

  @Override
  protected void onUpdateToTable() {
      allLanguageProcessor.onNext(fetchAllLanguages());
  }

  public Flowable<List<Language>> allLanguages() {
    return allLanguageProcessor;
  }

  public List<Language> fetchAllLanguages() {
    return fetchWith(Query.select());
  }

  @NotNull private List<Language> fetchWith(Query query) {
    ArrayList<Language> result = new ArrayList<>();
    final NetworkLanguageDatabaseEntity databaseEntity =
        new NetworkLanguageDatabaseEntity();
    try (SquidCursor<NetworkLanguageDatabaseEntity> languageCursor = kiwixDatabase.query(
        NetworkLanguageDatabaseEntity.class,
        query)) {
      while (languageCursor.moveToNext()) {
        databaseEntity.readPropertiesFromCursor(languageCursor);
        result.add(
            new Language(
                databaseEntity.getLanguageISO3(),
                databaseEntity.isEnabled(),
                databaseEntity.getNumberOfOccurences()
            )
        );
      }
    }
    return result;
  }

  public void saveFilteredLanguages(List<Language> languages) {
    kiwixDatabase.beginTransaction();
    kiwixDatabase.deleteAll(NetworkLanguageDatabaseEntity.class);
    Collections.sort(languages,
        (language, t1) -> language.getLanguage().compareTo(t1.getLanguage()));

    for (int i = 0; i < languages.size(); i++) {
      Language language = languages.get(i);
      NetworkLanguageDatabaseEntity networkLanguageDatabaseEntity =
          new NetworkLanguageDatabaseEntity();
      networkLanguageDatabaseEntity.setLanguageISO3(language.getLanguageCode());
      networkLanguageDatabaseEntity.setIsEnabled(language.getActive());
      networkLanguageDatabaseEntity.setNumberOfOccurences(language.getOccurencesOfLanguage());
      kiwixDatabase.persist(networkLanguageDatabaseEntity);
    }
    kiwixDatabase.setTransactionSuccessful();
    kiwixDatabase.endTransaction();
  }
}