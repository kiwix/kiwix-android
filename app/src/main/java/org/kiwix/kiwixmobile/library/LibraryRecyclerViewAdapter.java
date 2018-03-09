/*
 * Copyright 2013  Rashiq Ahmad <rashiq.z@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.library;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import com.google.common.collect.ImmutableList;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.database.BookDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.database.NetworkLanguageDao;
import org.kiwix.kiwixmobile.downloader.DownloadFragment;
import org.kiwix.kiwixmobile.library.contract.ILibraryItemClickListener;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;
import org.kiwix.kiwixmobile.utils.BookUtils;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static org.kiwix.kiwixmobile.utils.NetworkUtils.parseURL;

public class LibraryRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  public static final int LIST_ITEM_TYPE_BOOK = 0;
  public static final int LIST_ITEM_TYPE_DIVIDER = 1;

  private ImmutableList<Book> allBooks;
  private List<ListItem> listItems = new ArrayList<>();
  private final Context context;
  public Map<String, Integer> languageCounts = new HashMap<>();
  public List<Language> languages = new ArrayList<>();
  private final NetworkLanguageDao networkLanguageDao;
  private final BookDao bookDao;
  private final BookFilter bookFilter;
  private Disposable saveNetworkLanguageDisposable;
  private ILibraryItemClickListener libraryItemClickListener;
  @Inject BookUtils bookUtils;


  private void setupDagger() {
    KiwixApplication.getInstance().getApplicationComponent().inject(this);
  }

  public LibraryRecyclerViewAdapter(Context context, ILibraryItemClickListener libraryItemClickListener) {
    setupDagger();
    this.context = context;
    this.libraryItemClickListener = libraryItemClickListener;
    networkLanguageDao = new NetworkLanguageDao(KiwixDatabase.getInstance(context));
    bookDao = new BookDao(KiwixDatabase.getInstance(context));
    bookFilter = new BookFilter();
  }

  @Override
  public int getItemViewType(int position) {
    if (isDivider(position)) {
      return LIST_ITEM_TYPE_DIVIDER;
    } else {
      return LIST_ITEM_TYPE_BOOK;
    }
  }

  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());

    View view;
    switch (viewType) {
      case LIST_ITEM_TYPE_DIVIDER:
        view = inflater.inflate(R.layout.library_divider, parent, false);
        return new LibraryDividerRecyclerViewHolder(view, libraryItemClickListener);
      case LIST_ITEM_TYPE_BOOK:
        view = inflater.inflate(R.layout.library_item, parent, false);
        return new LibraryBookRecyclerViewHolder(view, libraryItemClickListener);
      default:
        // No implementation
    }
    return null;
  }

  @Override
  public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    switch (holder.getItemViewType()) {
      case LIST_ITEM_TYPE_DIVIDER:
        LibraryDividerRecyclerViewHolder libraryDividerRecyclerViewHolder = (LibraryDividerRecyclerViewHolder) holder;
        libraryDividerRecyclerViewHolder.bind(listItems.get(position));
        break;
      case LIST_ITEM_TYPE_BOOK:
        LibraryBookRecyclerViewHolder libraryBookRecyclerViewHolder = (LibraryBookRecyclerViewHolder) holder;
        libraryBookRecyclerViewHolder.bind(listItems.get(position));
        break;
      default:
        // No implementation
    }
  }

  @Override
  public int getItemCount() {
    return listItems.size();
  }

  public List<ListItem> getListItems() {
    return listItems;
  }

  //getters

  public Filter getFilter() {
    return bookFilter;
  }

  private Observable<Book> getMatches(Book b, String s) {
    StringBuilder text = new StringBuilder();
    text.append(b.getTitle()).append("|").append(b.getDescription()).append("|")
        .append(parseURL(context, b.getUrl())).append("|");
    if (bookUtils.localeMap.containsKey(b.getLanguage())) {
      text.append(bookUtils.localeMap.get(b.getLanguage()).getDisplayLanguage()).append("|");
    }
    String[] words = s.toLowerCase().split("\\s+");
    b.searchMatches = Observable.fromArray(words)
        .filter(text.toString().toLowerCase()::contains)
        .count()
        .blockingGet()
        .intValue();
    if (b.searchMatches > 0) {
      return Observable.just(b);
    } else {
      return Observable.empty();
    }
  }



  //setters

  public void setAllBooks(List<Book> books) {
    allBooks = ImmutableList.copyOf(books);
    updateLanguageCounts();
    updateLanguages();
  }


  //utility methods

  public boolean isDivider(int position) {
    return listItems.get(position).type == LIST_ITEM_TYPE_DIVIDER;
  }

  private boolean languageActive(Book book) {
    return Observable.fromIterable(languages)
        .filter(language -> language.languageCode.equals(book.getLanguage()))
        .firstElement()
        .map(language -> language.active)
        .blockingGet(false);
  }

  public void updateNetworkLanguages() {
    saveNetworkLanguages();
  }

  private void updateLanguageCounts() {
    languageCounts.clear();
    for (Book book : allBooks) {
      Integer cnt = languageCounts.get(book.getLanguage());
      if (cnt == null) {
        languageCounts.put(book.getLanguage(), 1);
      } else {
        languageCounts.put(book.getLanguage(), cnt + 1);
      }
    }
  }

  private void saveNetworkLanguages() {
    if (saveNetworkLanguageDisposable != null && !saveNetworkLanguageDisposable.isDisposed()) {
      saveNetworkLanguageDisposable.dispose();
    }
    saveNetworkLanguageDisposable = Completable.fromAction(() -> networkLanguageDao.saveFilteredLanguages(languages))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe();
  }

  // Decode and create a Bitmap from the 64-Bit encoded favicon string
  public static Bitmap createBitmapFromEncodedString(String encodedString, Context context) {

    try {
      byte[] decodedString = Base64.decode(encodedString, Base64.DEFAULT);
      return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return BitmapFactory.decodeResource(context.getResources(), R.mipmap.kiwix_icon);
  }

  // Create a string that represents the size of the zim file in a human readable way
  public static String createGbString(String megaByte) {

    int size = 0;
    try {
      size = Integer.parseInt(megaByte);
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }

    if (size <= 0) {
      return "";
    }

    final String[] units = new String[]{"KB", "MB", "GB", "TB"};
    int conversion = (int) (Math.log10(size) / Math.log10(1024));
    return new DecimalFormat("#,##0.#")
        .format(size / Math.pow(1024, conversion))
        + " "
        + units[conversion];
  }

  private void addBooks(List<Book> books) {
    for (Book book : books) {
      listItems.add(new ListItem(book, LIST_ITEM_TYPE_BOOK));
    }
  }

  private void updateLanguages() {
    // Load previously stored languages and extract which ones were enabled. The new book list might
    // have new languages, or be missing some old ones so we want to refresh it, but retain user's
    // selections.
    Set<String> enabled_languages = new HashSet<>();
    for (Language language : networkLanguageDao.getFilteredLanguages()) {
      if (language.active) {
        enabled_languages.add(language.languageCode);
      }
    }

    // Populate languages with all available locales, which appear in the current list of all books.
    this.languages = new ArrayList<>();
    for (String iso_language : Locale.getISOLanguages()) {
      Locale locale = new Locale(iso_language);
      if (languageCounts.get(locale.getISO3Language()) != null) {
        // Enable this language either if it was enabled previously, or if it is the device language.
        if (enabled_languages.contains(locale.getISO3Language()) ||
            context.getResources().getConfiguration().locale.getISO3Language().equals(locale.getISO3Language())) {
          this.languages.add(new Language(locale, true));
        } else {
          this.languages.add(new Language(locale, false));
        }
      }
    }

    saveNetworkLanguages();
  }


  // Inner classes

  public static class ListItem {
    public Object data;
    public int type;

    public ListItem(Object data, int type) {
      this.data = data;
      this.type = type;
    }
  }

  public static class Language {
    public String language;
    public String languageLocalized;
    public String languageCode;
    public String languageCodeISO2;
    public Boolean active;

    Language(Locale locale, Boolean active) {
      this.language = locale.getDisplayLanguage();
      this.languageLocalized = locale.getDisplayLanguage(locale);
      this.languageCode = locale.getISO3Language();
      this.languageCodeISO2 = locale.getLanguage();

      this.active = active;
    }

    public Language(String languageCode, Boolean active) {
      this(new Locale(languageCode), active);
    }

    @Override
    public boolean equals(Object obj) {
      return ((Language) obj).language.equals(language) &&
          ((Language) obj).active.equals(active);
    }
  }

  private class BookFilter extends Filter {
    @Override
    protected FilterResults performFiltering(CharSequence s) {
      ArrayList<Book> books = bookDao.getBooks();
      if (s.length() == 0) {
        List<Book> selectedLanguages = Observable.fromIterable(allBooks)
            .filter(LibraryRecyclerViewAdapter.this::languageActive)
            .filter(book -> !books.contains(book))
            .filter(book -> !DownloadFragment.mDownloads.values().contains(book))
            .filter(book -> !LibraryFragment.downloadingBooks.contains(book))
            .toList()
            .blockingGet();

        List<Book> unselectedLanguages = Observable.fromIterable(allBooks)
            .filter(book -> !languageActive(book))
            .filter(book -> !books.contains(book))
            .filter(book -> !DownloadFragment.mDownloads.values().contains(book))
            .filter(book -> !LibraryFragment.downloadingBooks.contains(book))
            .toList()
            .blockingGet();

        listItems.clear();
        listItems.add(new ListItem(context.getResources().getString(R.string.your_languages), LIST_ITEM_TYPE_DIVIDER));
        addBooks(selectedLanguages);
        listItems.add(new ListItem(context.getResources().getString(R.string.other_languages), LIST_ITEM_TYPE_DIVIDER));
        addBooks(unselectedLanguages);
      } else {
        List<Book> selectedLanguages = Observable.fromIterable(allBooks)
            .filter(LibraryRecyclerViewAdapter.this::languageActive)
            .filter(book -> !books.contains(book))
            .filter(book -> !DownloadFragment.mDownloads.values().contains(book))
            .filter(book -> !LibraryFragment.downloadingBooks.contains(book))
            .flatMap(book -> getMatches(book, s.toString()))
            .toList()
            .blockingGet();

        Collections.sort(selectedLanguages, new BookMatchComparator());

        List<Book> unselectedLanguages = Observable.fromIterable(allBooks)
            .filter(book -> !languageActive(book))
            .filter(book -> !books.contains(book))
            .filter(book -> !DownloadFragment.mDownloads.values().contains(book))
            .filter(book -> !LibraryFragment.downloadingBooks.contains(book))
            .flatMap(book -> getMatches(book, s.toString()))
            .toList()
            .blockingGet();

        Collections.sort(unselectedLanguages, new BookMatchComparator());

        listItems.clear();
        listItems.add(new ListItem("In your language:", LIST_ITEM_TYPE_DIVIDER));
        addBooks(selectedLanguages);
        listItems.add(new ListItem("In other languages:", LIST_ITEM_TYPE_DIVIDER));
        addBooks(unselectedLanguages);
      }

      FilterResults results = new FilterResults();
      results.values = listItems;
      results.count = listItems.size();
      return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
      List<ListItem> filtered = (List<ListItem>) results.values;
      if (filtered != null) {
        if (filtered.isEmpty()) {
          addBooks(allBooks);
        }
      }
      notifyDataSetChanged();
    }
  }

  private class BookMatchComparator implements Comparator<Book> {
    public int compare(Book book1, Book book2) {
      return book2.searchMatches - book1.searchMatches;
    }
  }
}