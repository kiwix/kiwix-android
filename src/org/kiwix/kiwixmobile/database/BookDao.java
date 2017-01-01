package org.kiwix.kiwixmobile.database;


import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;

import org.kiwix.kiwixmobile.database.entity.BookDataSource;
import org.kiwix.kiwixmobile.database.entity.BookDatabaseEntity;
import org.kiwix.kiwixmobile.database.entity.Bookmarks;
import org.kiwix.kiwixmobile.database.entity.RecentSearch;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;
import org.kiwix.kiwixmobile.utils.files.FileUtils;


import java.io.File;
import java.util.ArrayList;

/**
 * Dao class for books
 */

public class BookDao {
  private KiwixDatabase mDb;


  public BookDao(KiwixDatabase kiwixDatabase) {
    this.mDb = kiwixDatabase;
  }


  public ArrayList<Book> getBooks() {
    SquidCursor<BookDatabaseEntity> bookCursor = mDb.query(
        BookDatabaseEntity.class,
        Query.select());
    ArrayList<Book> books = new ArrayList<>();
    while (bookCursor.moveToNext()) {
      Book book = new Book();
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
      if (!FileUtils.hasPart(book.file)) {
        if (book.file.exists()) {
          books.add(book);
        } else {
          mDb.deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.URL.eq(book.file.getPath()));
        }
      }

    }
    bookCursor.close();
    return books;
  }

  public ArrayList<Book> getDownloadingBooks() {
    SquidCursor<BookDatabaseEntity> bookCursor = mDb.query(
        BookDatabaseEntity.class,
        Query.select());
    ArrayList<Book> books = new ArrayList<>();
    while (bookCursor.moveToNext()) {
      Book book = new Book();
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
      book.remoteUrl = bookCursor.get(BookDatabaseEntity.REMOTE_URL);
      if (FileUtils.hasPart(book.file)) {
        books.add(book);
      }
    }
    bookCursor.close();
    return books;
  }

  public void saveBooks(ArrayList<Book> books) {
    mDb.deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.ID.isNotNull());
    for (Book book : books) {
      BookDatabaseEntity bookDatabaseEntity = new BookDatabaseEntity();
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
      String filePath = book.file.getPath();
      mDb.deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.URL.eq(filePath));
      mDb.persist(bookDatabaseEntity);
    }
  }

  public void saveBook(Book book) {
    BookDatabaseEntity bookDatabaseEntity = new BookDatabaseEntity();
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
    bookDatabaseEntity.setRemoteUrl(book.remoteUrl);
    String filePath = book.file.getPath();
    mDb.deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.URL.eq(filePath));
    mDb.persist(bookDatabaseEntity);
  }

  public void deleteBook(String id) {
    mDb.deleteWhere(BookDatabaseEntity.class, BookDatabaseEntity.BOOK_ID.eq(id));
  }
}
