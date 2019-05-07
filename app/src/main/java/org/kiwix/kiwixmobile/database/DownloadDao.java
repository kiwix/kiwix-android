/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.database;

import com.yahoo.squidb.data.SimpleDataChangedNotifier;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.TableStatement;
import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.kiwix.kiwixmobile.database.entity.DownloadDatabaseEntity;
import org.kiwix.kiwixmobile.downloader.model.DownloadModel;

public class DownloadDao {

  private final KiwixDatabase kiwixDatabase;
  private final PublishProcessor<List<DownloadModel>> downloadsProcessor =
      PublishProcessor.create();

  @Inject
  public DownloadDao(KiwixDatabase kiwixDatabase) {
    this.kiwixDatabase = kiwixDatabase;
    kiwixDatabase.registerDataChangedNotifier(
        new SimpleDataChangedNotifier(DownloadDatabaseEntity.TABLE) {
          @Override
          protected void onDataChanged() {
            downloadsProcessor.onNext(getDownloads());
          }
        });
  }

  public void insert(final DownloadModel downloadModel) {
    if (doesNotAlreadyExist(downloadModel)) {
      kiwixDatabase.persistWithOnConflict(databaseEntity(downloadModel),
          TableStatement.ConflictAlgorithm.REPLACE);
    }
  }

  private boolean doesNotAlreadyExist(DownloadModel downloadModel) {
    return kiwixDatabase.count(DownloadDatabaseEntity.class, DownloadDatabaseEntity.BOOK_ID.eq(downloadModel.getBookId())) == 0;
  }

  public void delete(@NotNull Long... downloadIds) {
    if (downloadIds.length > 0) {
      kiwixDatabase.deleteWhere(DownloadDatabaseEntity.class,
          DownloadDatabaseEntity.DOWNLOAD_ID.in((Object[]) downloadIds));
    }
  }

  public Flowable<List<DownloadModel>> downloads() {
    return downloadsProcessor.startWith(getDownloads()).distinctUntilChanged();
  }

  private TableModel databaseEntity(final DownloadModel downloadModel) {
    return new DownloadDatabaseEntity()
        .setDownloadId(downloadModel.getDownloadId())
        .setBookId(downloadModel.getBookId())
        .setFavIcon(downloadModel.getFavIcon());
  }

  private List<DownloadModel> getDownloads() {
    return toList(kiwixDatabase.query(DownloadDatabaseEntity.class, Query.select()));
  }

  private List<DownloadModel> toList(final SquidCursor<DownloadDatabaseEntity> cursor) {
    final ArrayList<DownloadModel> downloadModels = new ArrayList<>();
    final DownloadDatabaseEntity downloadDatabaseEntity = new DownloadDatabaseEntity();
    while (cursor.moveToNext()) {
      downloadDatabaseEntity.readPropertiesFromCursor(cursor);
      downloadModels.add(new DownloadModel(
          downloadDatabaseEntity.getDownloadId(),
          downloadDatabaseEntity.getBookId(),
          downloadDatabaseEntity.getFavIcon()
      ));
    }
    cursor.close();
    return downloadModels;
  }
}
