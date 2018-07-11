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
package org.kiwix.kiwixmobile.data.local.dao;


import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import org.kiwix.kiwixmobile.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.data.local.entity.BookDatabaseEntity;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;
import org.kiwix.kiwixmobile.utils.files.FileUtils;

import java.io.File;
import java.util.ArrayList;

import javax.inject.Inject;

/**
 * Dao class for books
 */

public class BookDao {
  private final KiwixDatabase kiwixDatabase;

  @Inject
  public BookDao(KiwixDatabase kiwixDatabase) {
    this.kiwixDatabase = kiwixDatabase;
  }

  private void setBookDetails(Book book, SquidCursor<BookDatabaseEntity> bookCursor) {
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

  private void setBookDatabaseEntity(Book book, BookDatabaseEntity bookDatabaseEntity) {
    bookDatabaseEntity.setBookId(book.getId());
    bookDatabaseEntity.setTitle(book.getTitle());
    bookDatabaseEntity.setDescription(book.getDescription());
    bookDatabaseEntity.setLanguage(book.getLanguage());
    bookDatabaseEntity.setBookCreator(book.getCreator());
    bookDatabaseEntity.setPublisher(book.getPublisher());
    bookDatabaseEntity.setDate(book.getDate());
    bookDatabaseEntity.setUrl(book.file.getPath());
    bookDatabaseEntity.setArticleCount(book.getArticleCount());
    bookDatabaseEntity.setMediaCount(book.getMediaCount());
    bookDatabaseEntity.setSize(book.getSize());
    bookDatabaseEntity.setFavicon(book.getFavicon());
    bookDatabaseEntity.setName(book.getName());
    kiwixDatabase.deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.BOOK_ID.eq(book.getId()));
    kiwixDatabase.persist(bookDatabaseEntity);
  }

  public ArrayList<Book> getBooks() {
    ArrayList<Book> books = new ArrayList<>();
    try (SquidCursor<BookDatabaseEntity> bookCursor = kiwixDatabase.query(BookDatabaseEntity.class,
        Query.select())) {
      while (bookCursor.moveToNext()) {
        Book book = new Book();
        setBookDetails(book, bookCursor);
        books.add(book);
      }
    }
    return filterBookResults(books);
  }

  public ArrayList<Book> filterBookResults(ArrayList<Book> books) {
    ArrayList<Book> filteredBookList = new ArrayList<>();
    for (Book book : books) {
      if (!FileUtils.hasPart(book.file)) {
        if (book.file.exists()) {
          filteredBookList.add(book);
        } else {
          kiwixDatabase.deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.URL.eq(book.file.getPath()));
        }
      }
    }
    return filteredBookList;
  }

  public ArrayList<Book> getDownloadingBooks() {
    ArrayList<Book> books = new ArrayList<>();
    try (SquidCursor<BookDatabaseEntity> bookCursor = kiwixDatabase.query(BookDatabaseEntity.class,
        Query.select())) {
      while (bookCursor.moveToNext()) {
        Book book = new Book();
        setBookDetails(book, bookCursor);
        book.remoteUrl = bookCursor.get(BookDatabaseEntity.REMOTE_URL);
        if (FileUtils.hasPart(book.file)) {
          books.add(book);
        }
      }
    }
    return books;
  }

  public void saveBooks(ArrayList<Book> books) {
    for (Book book : books) {
      if (book != null) {
        BookDatabaseEntity bookDatabaseEntity = new BookDatabaseEntity();
        setBookDatabaseEntity(book, bookDatabaseEntity);
      }
    }
  }

  public void saveBook(Book book) {
    BookDatabaseEntity bookDatabaseEntity = new BookDatabaseEntity();
    bookDatabaseEntity.setRemoteUrl(book.remoteUrl);
    setBookDatabaseEntity(book, bookDatabaseEntity);
  }

  public void deleteBook(String id) {
    kiwixDatabase.deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.BOOK_ID.eq(id));
  }
}
