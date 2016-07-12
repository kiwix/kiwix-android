package org.kiwix.kiwixmobile.database;


import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import org.kiwix.kiwixmobile.database.entity.BookDataSource;
import org.kiwix.kiwixmobile.database.entity.BookDatabaseEntity;
import org.kiwix.kiwixmobile.database.entity.Bookmarks;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;

import java.util.ArrayList;

/**
 * Dao class for books
 */

public class BookDao {
  private KiwixDatabase mDb;


  public BookDao(KiwixDatabase kiwikDatabase) {
    this.mDb = kiwikDatabase;
  }


  public LibraryNetworkEntity.Book getBook(String fileName) {
    SquidCursor<BookDatabaseEntity> bookCursor = mDb.query(
        BookDatabaseEntity.class,
        Query.select());
    LibraryNetworkEntity.Book book = new LibraryNetworkEntity.Book();
    while (bookCursor.moveToNext()){
      if (bookCursor.get(BookDatabaseEntity.URL).contains("/" + fileName + ".")) {
        book.id = bookCursor.get(BookDatabaseEntity.BOOK_ID);
        book.title = bookCursor.get(BookDatabaseEntity.TITLE);
        book.description = bookCursor.get(BookDatabaseEntity.DESCRIPTION);
        book.language = bookCursor.get(BookDatabaseEntity.LANGUAGE);
        book.creator = bookCursor.get(BookDatabaseEntity.BOOK_CREATOR);
        book.publisher = bookCursor.get(BookDatabaseEntity.PUBLISHER);
        book.date = bookCursor.get(BookDatabaseEntity.DATE);
        book.url = bookCursor.get(BookDatabaseEntity.URL);
        book.articleCount = bookCursor.get(BookDatabaseEntity.ARTICLE_COUNT);
        book.mediaCount = bookCursor.get(BookDatabaseEntity.MEDIA_COUNT);
        book.size = bookCursor.get(BookDatabaseEntity.SIZE);
        book.favicon = bookCursor.get(BookDatabaseEntity.FAVICON);
        book.downloaded = bookCursor.get(BookDatabaseEntity.DOWNLOADED);
        bookCursor.close();
        return book;
      }
    }
    bookCursor.close();
    return null;
  }

  public void saveBook(LibraryNetworkEntity.Book book) {
    BookDatabaseEntity bookDatabaseEntity = new BookDatabaseEntity();
    bookDatabaseEntity.setBookId(book.getId());
    bookDatabaseEntity.setTitle(book.getTitle());
    bookDatabaseEntity.setDescription(book.getTitle());
    bookDatabaseEntity.setLanguage(book.getLanguage());
    bookDatabaseEntity.setBookCreator(book.getCreator());
    bookDatabaseEntity.setPublisher(book.getPublisher());
    bookDatabaseEntity.setDate(book.getDate());
    bookDatabaseEntity.setUrl(book.getUrl());
    bookDatabaseEntity.setArticleCount(book.getArticleCount());
    bookDatabaseEntity.setMediaCount(book.getMediaCount());
    bookDatabaseEntity.setSize(book.getSize());
    bookDatabaseEntity.setFavicon(book.getFavicon());
    bookDatabaseEntity.setIsDownloaded(book.downloaded);
    mDb.deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.URL.eq(book.getUrl()));
    mDb.persist(bookDatabaseEntity);
  }


}
