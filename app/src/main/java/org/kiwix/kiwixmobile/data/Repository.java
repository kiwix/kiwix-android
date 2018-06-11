package org.kiwix.kiwixmobile.data;

import org.kiwix.kiwixmobile.data.local.dao.BookDao;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

/**
 * A central repository of data which should provide the presenters with the required data.
 */

@Singleton
public class Repository implements DataSource {

  private BookDao bookDao;
  private Scheduler io;
  private Scheduler mainThread;

  @Inject
  Repository(@IO Scheduler io, @MainThread Scheduler mainThread, BookDao bookDao) {
    this.io = io;
    this.mainThread = mainThread;
    this.bookDao = bookDao;
  }

  @Override
  public Single<List<LibraryNetworkEntity.Book>> getLanguageCategorizedBooks() {
    return Observable.fromIterable(bookDao.getBooks())
        .toSortedList((book1, book2) -> book1.getLanguage().compareToIgnoreCase(book2.getLanguage()) == 0 ?
            book1.getTitle().compareToIgnoreCase(book2.getTitle()) :
            book1.getLanguage().compareToIgnoreCase(book2.getLanguage()))
        .map(books -> {
          LibraryNetworkEntity.Book book = null;
          if (books.size() >= 1) {
            book = books.get(0);
            books.add(0, null);
          }
          for (int position = 2; position < books.size(); position++) {

            if (book != null && books.get(position) != null &&
                !new Locale(books.get(position).getLanguage()).getDisplayName()
                    .equalsIgnoreCase(new Locale(book.getLanguage()).getDisplayName())) {
              books.add(position, null);
            }
            book = books.get(position);
          }
          return books;
        })
        .subscribeOn(io)
        .observeOn(mainThread);
  }
}
