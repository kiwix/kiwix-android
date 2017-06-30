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

import static android.support.test.InstrumentationRegistry.getContext;

import static org.kiwix.kiwixmobile.utils.NetworkUtils.parseURL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.utils.BookUtils;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.database.BookDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.database.NetworkLanguageDao;
import org.kiwix.kiwixmobile.downloader.DownloadFragment;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;

import rx.Observable;
import rx.functions.Func2;

public class LibraryAdapter extends BaseAdapter {

  private ImmutableList<Book> allBooks;
  private List<Book> filteredBooks = new ArrayList<>();
  private final Context context;
  public List<Language> languages = new ArrayList<>();
  private final NetworkLanguageDao networkLanguageDao;
  private final BookDao bookDao;
  private final LayoutInflater layoutInflater;
  private final BookFilter bookFilter = new BookFilter();
  @Inject BookUtils bookUtils;

  private void setupDagger() {
    KiwixApplication.getInstance().getApplicationComponent().inject(this);
  }


  public LibraryAdapter(Context context) {
    super();
    setupDagger();
    this.context = context;
    layoutInflater = LayoutInflater.from(context);
    networkLanguageDao = new NetworkLanguageDao(KiwixDatabase.getInstance(context));
    bookDao = new BookDao(KiwixDatabase.getInstance(context));
  }

  public void setAllBooks(List<Book> books) {
    allBooks = ImmutableList.copyOf(books);
    getLanguages();
  }

  @Override
  public int getCount() {
    return filteredBooks.size();
  }

  @Override
  public Object getItem(int i) {
    return filteredBooks.get(i);
  }

  @Override
  public long getItemId(int i) {
    return i;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ViewHolder holder;
    if (convertView == null) {
      convertView = layoutInflater.inflate(R.layout.library_item, null);
      holder = new ViewHolder();
      holder.title = (TextView) convertView.findViewById(R.id.title);
      holder.description = (TextView) convertView.findViewById(R.id.description);
      holder.language = (TextView) convertView.findViewById(R.id.language);
      holder.creator = (TextView) convertView.findViewById(R.id.creator);
      holder.publisher = (TextView) convertView.findViewById(R.id.publisher);
      holder.date = (TextView) convertView.findViewById(R.id.date);
      holder.size = (TextView) convertView.findViewById(R.id.size);
      holder.fileName = (TextView) convertView.findViewById(R.id.fileName);
      holder.favicon = (ImageView) convertView.findViewById(R.id.favicon);
      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }

    Book book = filteredBooks.get(position);

    holder.title.setText(book.getTitle());
    holder.description.setText(book.getDescription());
    holder.language.setText(bookUtils.getLanguage(book.getLanguage()));
    holder.creator.setText(book.getCreator());
    holder.publisher.setText(book.getPublisher());
    holder.date.setText(book.getDate());
    holder.size.setText(createGbString(book.getSize()));
    holder.fileName.setText(parseURL(context, book.getUrl()));
    holder.favicon.setImageBitmap(createBitmapFromEncodedString(book.getFavicon(), context));

    // Check if no value is empty. Set the view to View.GONE, if it is. To View.VISIBLE, if not.
    if (book.getTitle() == null || book.getTitle().isEmpty()) {
      holder.title.setVisibility(View.GONE);
    } else {
      holder.title.setVisibility(View.VISIBLE);
    }

    if (book.getDescription() == null || book.getDescription().isEmpty()) {
      holder.description.setVisibility(View.GONE);
    } else {
      holder.description.setVisibility(View.VISIBLE);
    }

    if (book.getCreator() == null || book.getCreator().isEmpty()) {
      holder.creator.setVisibility(View.GONE);
    } else {
      holder.creator.setVisibility(View.VISIBLE);
    }

    if (book.getPublisher() == null || book.getPublisher().isEmpty()) {
      holder.publisher.setVisibility(View.GONE);
    } else {
      holder.publisher.setVisibility(View.VISIBLE);
    }

    if (book.getDate() == null || book.getDate().isEmpty()) {
      holder.date.setVisibility(View.GONE);
    } else {
      holder.date.setVisibility(View.VISIBLE);
    }

    if (book.getSize() == null || book.getSize().isEmpty()) {
      holder.size.setVisibility(View.GONE);
    } else {
      holder.size.setVisibility(View.VISIBLE);
    }

    return convertView;
  }

  private boolean languageActive(Book book) {
    return Observable.from(languages)
        .takeFirst(language -> language.languageCode.equals(book.getLanguage()))
        .map(language -> language.active).toBlocking().firstOrDefault(false);
  }

  private Observable<Book> getMatches(Book b, String s) {
    StringBuilder text = new StringBuilder();
    text.append(b.getTitle()).append("|").append(b.getDescription()).append("|")
        .append(parseURL(context, b.getUrl())).append("|");
    if (bookUtils.localeMap.containsKey(b.getLanguage())) {
      text.append(bookUtils.localeMap.get(b.getLanguage()).getDisplayLanguage()).append("|");
    }
    String[] words = s.toLowerCase().split("\\s+");
    b.searchMatches = Observable.from(words).filter(text.toString().toLowerCase()::contains).count().toBlocking().first();
    if (b.searchMatches > 0) {
      return Observable.just(b);
    } else {
      return Observable.empty();
    }
  }

  private class BookFilter extends Filter {
    @Override
    protected FilterResults performFiltering(CharSequence s) {
      ArrayList<Book> books = bookDao.getBooks();
      List<Book> finalBooks;
      if (s.length() == 0) {
        finalBooks = Observable.from(allBooks)
            .filter(LibraryAdapter.this::languageActive)
            .filter(book -> !books.contains(book))
            .filter(book -> !DownloadFragment.mDownloads.values().contains(book))
            .filter(book -> !LibraryFragment.downloadingBooks.contains(book))
            .toList().toBlocking().single();
      } else {
        finalBooks = Observable.from(allBooks)
            .filter(book -> !books.contains(book))
            .filter(book -> !DownloadFragment.mDownloads.values().contains(book))
            .filter(book -> !LibraryFragment.downloadingBooks.contains(book))
            .flatMap(book -> getMatches(book, s.toString()))
            .toList().toBlocking().single();
        Collections.sort(finalBooks, new BookMatchComparator());
      }
      FilterResults results = new FilterResults();
      results.values = finalBooks;
      results.count = finalBooks.size();
      return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
      List<Book> filtered = (List<Book>) results.values;
      if (filtered != null) {
        filteredBooks.clear();
        if (filtered.isEmpty()) {
          filteredBooks.addAll(allBooks);
        } else {
          filteredBooks.addAll(filtered);
        }
      }
      notifyDataSetChanged();
    }
  }

  public Filter getFilter() {
    return bookFilter;
  }

  public void updateNetworkLanguages() {
    new SaveNetworkLanguages().execute(languages);
  }

  private void getLanguages() {
    ArrayList<String> bookLanguages = new ArrayList<>();
    if (languages.size() > 0) {
      return;
    }

    if (networkLanguageDao.getFilteredLanguages().size() > 0) {
      languages = networkLanguageDao.getFilteredLanguages();
      return;
    }

    String[] languages = Locale.getISOLanguages();
    for (Book book : allBooks) {
      if (!bookLanguages.contains(book.getLanguage())) {
        bookLanguages.add(book.getLanguage());
      }
    }
    this.languages = new ArrayList<>();
    for (String language : languages) {
      Locale locale = new Locale(language);
      if (bookLanguages.contains(locale.getISO3Language())) {
        if (locale.getISO3Language().equals(context.getResources().getConfiguration().locale.getISO3Language())) {
          this.languages.add(new Language(locale, true));
        } else {
          this.languages.add(new Language(locale, false));
        }
      }
    }
    new SaveNetworkLanguages().execute(this.languages);
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

  private static class ViewHolder {

    TextView title;

    TextView description;

    TextView language;

    TextView creator;

    TextView publisher;

    TextView date;

    TextView size;

    TextView fileName;

    ImageView favicon;
  }

  private class BookMatchComparator implements Comparator<Book> {
    public int compare(Book book1, Book book2) {
      return book2.searchMatches - book1.searchMatches;
    }
  }

  public static class Language {
    public String language;
    public String languageCode;
    public Boolean active;

    Language(Locale locale, Boolean active) {
      this.language = locale.getDisplayLanguage();
      this.active = active;
      this.languageCode = locale.getISO3Language();
    }

    public Language(String languageCode, Boolean active) {
      this.language = new Locale(languageCode).getDisplayLanguage();
      this.active = active;
      this.languageCode = languageCode;
    }

    @Override
    public boolean equals(Object obj) {
      return ((Language) obj).language.equals(language) && ((Language) obj).active == ((Language) obj).active;
    }
  }

  private class SaveNetworkLanguages extends AsyncTask<List<Language>, Object, Void> {
    @SafeVarargs
    @Override
    protected final Void doInBackground(List<Language>... params) {
      networkLanguageDao.saveFilteredLanguages(params[0]);
      return null;
    }
  }

}
