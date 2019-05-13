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
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.database.entity.BookDatabaseEntity;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;

import static org.kiwix.kiwixmobile.downloader.ChunkUtils.hasParts;

/**
 * Dao class for books
 */

public class BookDao extends BaseDao {
  private final BehaviorProcessor<List<Book>> booksProcessor = BehaviorProcessor.create();

  @Inject
  public BookDao(KiwixDatabase kiwixDatabase) {
    super(kiwixDatabase, BookDatabaseEntity.TABLE);
  }

  @Override
  protected void onUpdateToTable() {
    booksProcessor.onNext(getBooks());
  }

  public Flowable<List<Book>> books() {
    return booksProcessor;
  }

  public void setBookDetails(Book book, SquidCursor<BookDatabaseEntity> bookCursor) {
    book.databaseId = bookCursor.get(BookDatabaseEntity.ID);
    book.id = bookCursor.get(BookDatabaseEntity.BOOK_ID);
    book.title = bookCursor.get(BookDatabaseEntity.TITLE);
    book.description = bookCursor.get(BookDatabaseEntity.DESCRIPTION);
    book.language = bookCursor.get(BookDatabaseEntity.LANGUAGE);
    book.creator = bookCursor.get(BookDatabaseEntity.BOOK_CREATOR);
    book.publisher = bookCursor.get(BookDatabaseEntity.PUBLISHER);
    book.date = bookCursor.get(BookDatabaseEntity.DATE);
    book.file = new File(bookCursor.get(BookDatabaseEntity.URL));
    book.articleCount = bookCursor.get(BookDatabaseEntity.ARTICLE_COUNT);
    book.mediaCount = bookCursor.get(BookDatabaseEntity.MEDIA_COUNT);
    book.size = bookCursor.get(BookDatabaseEntity.SIZE);
    book.favicon = bookCursor.get(BookDatabaseEntity.FAVICON);
    book.bookName = bookCursor.get(BookDatabaseEntity.NAME);
  }

  public void setBookDatabaseEntity(Book book, BookDatabaseEntity bookDatabaseEntity) {
    final String path = book.file.getPath();
    bookDatabaseEntity.setBookId(book.getId())
        .setTitle(book.getTitle())
        .setDescription(book.getDescription())
        .setLanguage(book.getLanguage())
        .setBookCreator(book.getCreator())
        .setPublisher(book.getPublisher())
        .setDate(book.getDate())
        .setUrl(path)
        .setArticleCount(book.getArticleCount())
        .setMediaCount(book.getMediaCount())
        .setSize(book.getSize())
        .setFavicon(book.getFavicon())
        .setName(book.getName());
    kiwixDatabase.deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.URL.eq(path));
    kiwixDatabase.persistWithOnConflict(bookDatabaseEntity, TableStatement.ConflictAlgorithm.REPLACE);
  }

  public List<Book> getBooks() {
    kiwixDatabase.beginTransaction();
    ArrayList<Book> books = new ArrayList<>();
    try(SquidCursor<BookDatabaseEntity> bookCursor = kiwixDatabase.query(
        BookDatabaseEntity.class,
        Query.select())) {
      while (bookCursor.moveToNext()) {
        Book book = new Book();
        setBookDetails(book, bookCursor);
        if (!hasParts(book.file)) {
          if (book.file.exists()) {
            books.add(book);
          } else {
            kiwixDatabase.deleteWhere(BookDatabaseEntity.class,
                BookDatabaseEntity.URL.eq(book.file));
          }
        }
      }
    }
    kiwixDatabase.setTransactionSuccessful();
    kiwixDatabase.endTransaction();
    return books;
  }

  public void saveBooks(List<Book> books) {
    kiwixDatabase.beginTransaction();
    for (Book book : books) {
      if (book != null) {
        BookDatabaseEntity bookDatabaseEntity = new BookDatabaseEntity();
        setBookDatabaseEntity(book, bookDatabaseEntity);
      }
    }
    kiwixDatabase.setTransactionSuccessful();
    kiwixDatabase.endTransaction();
  }

  public void deleteBook(String id) {
    kiwixDatabase.deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.BOOK_ID.eq(id));
  }
}
