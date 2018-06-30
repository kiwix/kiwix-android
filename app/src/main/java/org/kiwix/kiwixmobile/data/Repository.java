package org.kiwix.kiwixmobile.data;

import org.kiwix.kiwixmobile.data.local.dao.BookDao;
import org.kiwix.kiwixmobile.data.local.dao.HistoryDao;
import org.kiwix.kiwixmobile.data.local.entity.History;
import org.kiwix.kiwixmobile.data.local.dao.NetworkLanguageDao;
import org.kiwix.kiwixmobile.di.qualifiers.IO;
import org.kiwix.kiwixmobile.di.qualifiers.MainThread;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.models.Language;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

/**
 * A central repository of data which should provide the presenters with the required data.
 */

@Singleton
public class Repository implements DataSource {

  private BookDao bookDao;
  private NetworkLanguageDao languageDao;
  private HistoryDao historyDao;
  private Scheduler io;
  private Scheduler mainThread;

  @Inject
  Repository(@IO Scheduler io, @MainThread Scheduler mainThread,
             BookDao bookDao, NetworkLanguageDao languageDao, HistoryDao historyDao) {
    this.io = io;
    this.mainThread = mainThread;
    this.bookDao = bookDao;
    this.languageDao = languageDao;
    this.historyDao = historyDao;
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

  @Override
  public void saveBooks(List<LibraryNetworkEntity.Book> books) {
    bookDao.saveBooks((ArrayList<LibraryNetworkEntity.Book>) books);
  }

  @Override
  public Completable saveLanguages(List<Language> languages) {
    return Completable.fromAction(() -> languageDao.saveFilteredLanguages(languages))
        .subscribeOn(io);
  }

  @Override
  public Single<List<History>> getDateCategorizedHistory(boolean showHistoryCurrentBook) {
    return Single.just(historyDao.getHistoryList(showHistoryCurrentBook))
        .map(histories -> {
          History history = null;
          if (histories.size() >= 1) {
            history = histories.get(0);
            histories.add(0, null);
          }
          for (int position = 2; position < histories.size(); position++) {
            if (history != null && histories.get(position) != null) {

              Calendar calendar1 = Calendar.getInstance();
              calendar1.setTime(new Date(history.getTimeStamp()));

              Calendar calendar2 = Calendar.getInstance();
              calendar2.setTime(new Date(histories.get(position).getTimeStamp()));

              if (calendar1.get(Calendar.YEAR) != calendar2.get(Calendar.YEAR) ||
                  calendar1.get(Calendar.DAY_OF_YEAR) != calendar2.get(Calendar.DAY_OF_YEAR)) {
                histories.add(position, null);
              }
            }
            history = histories.get(position);
          }
          return histories;
        })
        .subscribeOn(io)
        .observeOn(mainThread);
  }

  @Override
  public Completable saveHistory(String file, String favicon, String url, String title, long timeStamp) {
    return Completable.fromAction(() -> historyDao.saveHistory(file, favicon, url, title, timeStamp))
        .subscribeOn(io);
  }

  @Override
  public Completable deleteHistory(List<History> historyList) {
    return Completable.fromAction(() -> historyDao.deleteHistory(historyList))
        .subscribeOn(io);
  }
}
