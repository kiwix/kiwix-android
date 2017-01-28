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
import android.os.AsyncTask;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.kiwix.kiwixmobile.LibraryFragment;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.ZimManageActivity;
import org.kiwix.kiwixmobile.database.BookDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.database.NetworkLanguageDao;
import org.kiwix.kiwixmobile.downloader.DownloadFragment;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;
import org.kiwix.kiwixmobile.utils.LanguageUtils;

import rx.Observable;

public class LibraryAdapter extends ArrayAdapter<Book> {

  public static Map<String, Locale> mLocaleMap;
  public static ArrayList<Language> mLanguages = new ArrayList<>();
  private static ImmutableList<Book> allBooks;
  private BookFilter filter;
  private static ZimManageActivity mActivity;
  private static ArrayList<String> bookLanguages;
  private static NetworkLanguageDao networkLanguageDao;
  private static BookDao bookDao;

  public LibraryAdapter(Context context, ArrayList<Book> books) {
    super(context, 0, books);
    allBooks = ImmutableList.copyOf(books);
    mActivity = (ZimManageActivity) context;
    networkLanguageDao = new NetworkLanguageDao(KiwixDatabase.getInstance(mActivity));
    bookDao = new BookDao(KiwixDatabase.getInstance(context));
    initLanguageMap();
    getLanguages();
    getFilter().filter(mActivity.searchView.getQuery());
  }


  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ViewHolder holder;
    if (convertView == null) {
      convertView = View.inflate(getContext(), R.layout.library_item, null);
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

    Book book = getItem(position);

    holder.title.setText(book.getTitle());
    holder.description.setText(book.getDescription());
    holder.language.setText(getLanguage(book.getLanguage()));
    holder.creator.setText(book.getCreator());
    holder.publisher.setText(book.getPublisher());
    holder.date.setText(book.getDate());
    holder.size.setText(createGbString(book.getSize()));
    holder.fileName.setText(parseURL(book.getUrl()));
    holder.favicon.setImageBitmap(createBitmapFromEncodedString(book.getFavicon()));

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

  public static String parseURL(String url) {
    String details;
    try {
      details = url.substring(url.lastIndexOf("/") + 1, url.length() - 10);
      details = details.substring(details.indexOf("_", details.indexOf("_") + 1) + 1, details.lastIndexOf("_"));
      details = details.replaceAll("_", " ");
      details = details.replaceAll("all", "");
      details = details.replaceAll("nopic", mActivity.getString(R.string.zim_nopic));
      details = details.replaceAll("simple", mActivity.getString(R.string.zim_simple));
      details = details.trim().replaceAll(" +", " ");
      return details;
    } catch (Exception e) {
      return "";
    }
  }

  public static boolean langaugeActive(Book book) {
    return Observable.from(mLanguages)
        .takeFirst(language -> language.languageCode.equals(book.getLanguage()))
        .map(language -> language.active).toBlocking().firstOrDefault(false);
  }

  public static Observable<Book> getMatches(Book b, String s) {
    StringBuffer text = new StringBuffer();
    text.append(b.getTitle() + "|" + b.getDescription() + "|" + parseURL(b.getUrl()) + "|");
    if (mLocaleMap.containsKey(b.getLanguage())) {
      text.append(mLocaleMap.get(b.getLanguage()).getDisplayLanguage() + "|");
    }
    String[] words = s.toString().toLowerCase().split("\\s+");
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
            .filter(LibraryAdapter::langaugeActive)
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
      LibraryAdapter.this.clear();
      if (filtered != null) {
        if (filtered.isEmpty()) {
          filtered.addAll(allBooks);
        }
        LibraryAdapter.this.addAll(filtered);
      }
      notifyDataSetChanged();
      if (mActivity.mSectionsPagerAdapter.libraryFragment.progressBarLayout != null) {
        mActivity.mSectionsPagerAdapter.libraryFragment.progressBarLayout.setVisibility(View.GONE);
        mActivity.mSectionsPagerAdapter.libraryFragment.libraryList.setVisibility(View.VISIBLE);
      }
    }
  }

  @Override
  public Filter getFilter() {
    if (filter == null) {
      filter = new BookFilter();
    }
    return filter;
  }

  // Create a map of ISO 369-2 language codes
  public static void initLanguageMap() {
    String[] languages = Locale.getISOLanguages();
    bookLanguages = new ArrayList<>();
    mLocaleMap = new HashMap<>(languages.length);
    for (String language : languages) {
      Locale locale = new Locale(language);
      mLocaleMap.put(locale.getISO3Language(), locale);
    }
  }

  public static void updateNetworklanguages() {
    new saveNetworkLanguages().execute(mLanguages);
  }

  public static void getLanguages() {
    if (mLanguages.size() > 0) {
      return;
    }

    if (networkLanguageDao.getFilteredLanguages().size() > 0) {
      mLanguages = networkLanguageDao.getFilteredLanguages();
      return;
    }

    String[] languages = Locale.getISOLanguages();
    for (Book book : LibraryAdapter.allBooks) {
      if (!bookLanguages.contains(book.getLanguage())) {
        bookLanguages.add(book.getLanguage());
      }
    }
    mLanguages = new ArrayList<>();
    for (String language : languages) {
      Locale locale = new Locale(language);
      if (bookLanguages.contains(locale.getISO3Language())) {
        if (locale.getISO3Language().equals(mActivity.getResources().getConfiguration().locale.getISO3Language())) {
          mLanguages.add(new Language(locale, true));
        } else {
          mLanguages.add(new Language(locale, false));
        }
      }
    }
    new saveNetworkLanguages().execute(mLanguages);
  }

  // Get the language from the language codes of the parsed xml stream
  public static String getLanguage(String languageCode) {

    if (languageCode == null) {
      return "";
    }

    if (languageCode.length() == 2) {
      return new LanguageUtils.LanguageContainer(languageCode).getLanguageName();
    } else if (languageCode.length() == 3) {
      try {
        return mLocaleMap.get(languageCode).getDisplayLanguage();
      } catch (Exception e) {
        return "";
      }
    }
    return "";
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
  public static Bitmap createBitmapFromEncodedString(String encodedString) {

    try {
      byte[] decodedString = Base64.decode(encodedString, Base64.DEFAULT);
      return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return BitmapFactory.decodeResource(mActivity.getResources(), R.mipmap.kiwix_icon);
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

  class BookMatchComparator implements Comparator<Book> {
    public int compare(Book book1, Book book2) {
      return book2.searchMatches - book1.searchMatches;
    }
  }

  public static class Language {
    public String language;
    public String languageCode;
    public Boolean active;

    public Language(Locale locale, Boolean active) {
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

  private static class saveNetworkLanguages extends AsyncTask<ArrayList<Language>, Object, Void> {
    @Override
    protected Void doInBackground(ArrayList<Language>... params) {
      networkLanguageDao.saveFilteredLanguages(params[0]);
      return null;
    }
  }

}
