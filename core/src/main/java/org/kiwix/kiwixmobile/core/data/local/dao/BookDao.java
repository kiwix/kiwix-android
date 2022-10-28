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
import java.io.File;
import java.util.ArrayList;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.core.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.core.data.local.entity.BookDatabaseEntity;
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity.Book;
import org.kiwix.kiwixmobile.core.utils.files.FileUtils;

/**
 * Dao class for books
 */

@Deprecated
public class BookDao {
  private final KiwixDatabase kiwixDatabase;

  @Inject
  public BookDao(KiwixDatabase kiwixDatabase) {
    this.kiwixDatabase = kiwixDatabase;
  }

  private void setBookDetails(Book book, SquidCursor<BookDatabaseEntity> bookCursor) {
    book.setId(bookCursor.get(BookDatabaseEntity.BOOK_ID));
    book.setTitle(bookCursor.get(BookDatabaseEntity.TITLE));
    book.setDescription(bookCursor.get(BookDatabaseEntity.DESCRIPTION));
    book.setLanguage(bookCursor.get(BookDatabaseEntity.LANGUAGE));
    book.setCreator(bookCursor.get(BookDatabaseEntity.BOOK_CREATOR));
    book.setPublisher(bookCursor.get(BookDatabaseEntity.PUBLISHER));
    book.setDate(bookCursor.get(BookDatabaseEntity.DATE));
    book.setFile(new File(bookCursor.get(BookDatabaseEntity.URL)));
    book.setArticleCount(bookCursor.get(BookDatabaseEntity.ARTICLE_COUNT));
    book.setMediaCount(bookCursor.get(BookDatabaseEntity.MEDIA_COUNT));
    book.setSize(bookCursor.get(BookDatabaseEntity.SIZE));
    book.setFavicon(bookCursor.get(BookDatabaseEntity.FAVICON));
    book.setBookName(bookCursor.get(BookDatabaseEntity.NAME));
  }

  public ArrayList<Book> getBooks() {
    ArrayList<Book> books = new ArrayList<>();
    try {
      SquidCursor<BookDatabaseEntity> bookCursor = kiwixDatabase.query(BookDatabaseEntity.class,
        Query.select());
      while (bookCursor.moveToNext()) {
        Book book = new Book();
        setBookDetails(book, bookCursor);
        books.add(book);
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    return filterBookResults(books);
  }

  public ArrayList<Book> filterBookResults(ArrayList<Book> books) {
    ArrayList<Book> filteredBookList = new ArrayList<>();
    for (Book book : books) {
      if (!FileUtils.hasPart(book.getFile())) {
        if (book.getFile().exists()) {
          filteredBookList.add(book);
        } else {
          kiwixDatabase.deleteWhere(BookDatabaseEntity.class,
            BookDatabaseEntity.URL.eq(book.getFile().getPath()));
        }
      }
    }
    return filteredBookList;
  }
}
