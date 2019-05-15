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
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.TableStatement;
import io.reactivex.Flowable;
import io.reactivex.processors.BehaviorProcessor;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.database.entity.BookDatabaseEntity;
import org.kiwix.kiwixmobile.downloader.model.BookOnDisk;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;

import static org.kiwix.kiwixmobile.downloader.ChunkUtils.hasParts;

/**
 * Dao class for books
 */

public class BookDao extends BaseDao {
  private final BehaviorProcessor<List<BookOnDisk>> booksProcessor = BehaviorProcessor.create();

  @Inject
  public BookDao(KiwixDatabase kiwixDatabase) {
    super(kiwixDatabase, BookDatabaseEntity.TABLE);
  }

  @Override
  protected void onUpdateToTable() {
    booksProcessor.onNext(getBooks());
  }

  public Flowable<List<BookOnDisk>> books() {
    return booksProcessor;
  }

  public void setBookDatabaseEntity(BookOnDisk bookonDisk, BookDatabaseEntity bookDatabaseEntity) {
    final Book book = bookonDisk.getBook();
    bookDatabaseEntity
        .setFilePath(bookonDisk.getFile().getPath())
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
        .setFavicon(book.getFavicon());
  }

  public List<BookOnDisk> getBooks() {
    kiwixDatabase.beginTransaction();
    ArrayList<BookOnDisk> books = new ArrayList<>();
    final BookDatabaseEntity bookDatabaseEntity = new BookDatabaseEntity();
    try(SquidCursor<BookDatabaseEntity> bookCursor = kiwixDatabase.query(
        BookDatabaseEntity.class,
        Query.select())) {
      while (bookCursor.moveToNext()) {
        bookDatabaseEntity.readPropertiesFromCursor(bookCursor);
        final File file = new File(bookDatabaseEntity.getFilePath());
        BookOnDisk book = new BookOnDisk(
            bookDatabaseEntity.getId(),
            toBook(bookDatabaseEntity),
            file);
        if (!hasParts(file)) {
          if (file.exists()) {
            books.add(book);
          } else {
            kiwixDatabase.deleteWhere(BookDatabaseEntity.class,
                BookDatabaseEntity.FILE_PATH.eq(file.getPath()));
          }
        }
      }
    }
    kiwixDatabase.setTransactionSuccessful();
    kiwixDatabase.endTransaction();
    return books;
  }

  public void saveBooks(Collection<BookOnDisk> books) {
    kiwixDatabase.beginTransaction();
    for (BookOnDisk book : books) {
      if (book != null) {
        BookDatabaseEntity bookDatabaseEntity = new BookDatabaseEntity();
        setBookDatabaseEntity(book, bookDatabaseEntity);
        kiwixDatabase.deleteWhere(BookDatabaseEntity.class,
            BookDatabaseEntity.FILE_PATH.eq(bookDatabaseEntity.getFilePath()));
        kiwixDatabase.persistWithOnConflict(bookDatabaseEntity,
            TableStatement.ConflictAlgorithm.REPLACE);
      }
    }
    kiwixDatabase.setTransactionSuccessful();
    kiwixDatabase.endTransaction();
  }

  public void deleteBook(Long id) {
    kiwixDatabase.deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.ID.eq(id));
  }

  private LibraryNetworkEntity.Book toBook(BookDatabaseEntity bookDatabaseEntity) {
    final LibraryNetworkEntity.Book book = new LibraryNetworkEntity.Book();
    book.id = bookDatabaseEntity.getBookId();
    book.title = bookDatabaseEntity.getTitle();
    book.description = bookDatabaseEntity.getDescription();
    book.language = bookDatabaseEntity.getLanguage();
    book.creator = bookDatabaseEntity.getBookCreator();
    book.publisher = bookDatabaseEntity.getPublisher();
    book.date = bookDatabaseEntity.getDate();
    book.url = bookDatabaseEntity.getUrl();
    book.articleCount = bookDatabaseEntity.getArticleCount();
    book.mediaCount = bookDatabaseEntity.getMediaCount();
    book.size = bookDatabaseEntity.getSize();
    book.bookName = bookDatabaseEntity.getName();
    book.favicon = bookDatabaseEntity.getFavicon();
    return book;
  }
}
