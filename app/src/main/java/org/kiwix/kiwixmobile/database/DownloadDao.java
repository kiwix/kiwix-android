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

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.data.TableModel;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.sql.TableStatement;
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.kiwix.kiwixmobile.database.entity.DownloadDatabaseEntity;
import org.kiwix.kiwixmobile.downloader.model.DownloadModel;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;

public class DownloadDao extends BaseDao{

  private final BehaviorProcessor<List<DownloadModel>> downloadsProcessor =
      BehaviorProcessor.create();

  @Inject
  public DownloadDao(KiwixDatabase kiwixDatabase) {
    super(kiwixDatabase,DownloadDatabaseEntity.TABLE);
  }

  @Override
  protected void onUpdateToTable() {
    downloadsProcessor.onNext(getDownloads());
  }

  public void insert(final DownloadModel downloadModel) {
    if (doesNotAlreadyExist(downloadModel)) {
      kiwixDatabase.persistWithOnConflict(databaseEntity(downloadModel),
          TableStatement.ConflictAlgorithm.REPLACE);
    }
  }

  private boolean doesNotAlreadyExist(DownloadModel downloadModel) {
    return kiwixDatabase.count(
        DownloadDatabaseEntity.class,
        DownloadDatabaseEntity.BOOK_ID.eq(downloadModel.getBook().getId())
    ) == 0;
  }

  public void delete(@NotNull Long... downloadIds) {
    if (downloadIds.length > 0) {
      kiwixDatabase.deleteWhere(DownloadDatabaseEntity.class,
          DownloadDatabaseEntity.DOWNLOAD_ID.in((Object[]) downloadIds));
    }
  }

  public Flowable<List<DownloadModel>> downloads() {
    return downloadsProcessor;
  }

  private TableModel databaseEntity(final DownloadModel downloadModel) {
    final LibraryNetworkEntity.Book book = downloadModel.getBook();
    return new DownloadDatabaseEntity()
        .setDownloadId(downloadModel.getDownloadId())
        .setBookId(book.getId())
        .setTitle(book.getTitle())
        .setDescription(book.getDescription())
        .setLanguage(book.getLanguage())
        .setBookCreator(book.getCreator())
        .setPublisher(book.getPublisher())
        .setDate(book.getDate())
        .setUrl(book.getUrl())
        .setArticleCount(book.getArticleCount())
        .setMediaCount(book.getMediaCount())
        .setSize(book.getSize())
        .setName(book.getName())
        .setFavIcon(book.getFavicon());
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
          toBook(downloadDatabaseEntity)
      ));
    }
    cursor.close();
    return downloadModels;
  }

  private LibraryNetworkEntity.Book toBook(DownloadDatabaseEntity downloadDatabaseEntity) {
    final LibraryNetworkEntity.Book book = new LibraryNetworkEntity.Book();
    book.id = downloadDatabaseEntity.getBookId();
    book.title = downloadDatabaseEntity.getTitle();
    book.description = downloadDatabaseEntity.getDescription();
    book.language = downloadDatabaseEntity.getLanguage();
    book.creator = downloadDatabaseEntity.getBookCreator();
    book.publisher = downloadDatabaseEntity.getPublisher();
    book.date = downloadDatabaseEntity.getDate();
    book.url = downloadDatabaseEntity.getUrl();
    book.file = new File(downloadDatabaseEntity.getUrl());
    book.articleCount = downloadDatabaseEntity.getArticleCount();
    book.mediaCount = downloadDatabaseEntity.getMediaCount();
    book.size = downloadDatabaseEntity.getSize();
    book.bookName = downloadDatabaseEntity.getName();
    book.favicon = downloadDatabaseEntity.getFavIcon();
    return book;
  }
}
