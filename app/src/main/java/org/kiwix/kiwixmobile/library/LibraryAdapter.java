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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.collect.ImmutableList;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.database.BookDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.database.NetworkLanguageDao;
import org.kiwix.kiwixmobile.downloader.DownloadFragment;
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

import rx.Observable;

import static org.kiwix.kiwixmobile.utils.NetworkUtils.parseURL;

public class LibraryAdapter extends BaseAdapter {
  private static final int LIST_ITEM_TYPE_BOOK = 0;
  private static final int LIST_ITEM_TYPE_DIVIDER = 1;

  private ImmutableList<Book> allBooks;
  private List<ListItem> listItems = new ArrayList<>();
  private final Context context;
  public Map<String, Integer> languageCounts = new HashMap<>();
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
    updateLanguageCounts();
    updateLanguages();
  }

  public boolean isDivider(int position) {
    return listItems.get(position).type == LIST_ITEM_TYPE_DIVIDER;
  }

  @Override
  public int getCount() {
    return listItems.size();
  }

  @Override
  public Object getItem(int i) {
    return listItems.get(i).data;
  }

  @Override
  public long getItemId(int i) {
    return i;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ViewHolder holder;
    if (position + 1 >= listItems.size()) {
      return convertView;
    }
    ListItem item = listItems.get(position);

    if (item.type == LIST_ITEM_TYPE_BOOK) {
      if (convertView != null && convertView.findViewById(R.id.title) != null) {
        holder = (ViewHolder) convertView.getTag();
      } else {
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
      }

      Book book = (Book) listItems.get(position).data;

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
    } else {
      if (convertView != null && convertView.findViewById(R.id.divider_text) != null) {
        holder = (ViewHolder) convertView.getTag();
      } else {
        convertView = layoutInflater.inflate(R.layout.library_divider, null);
        holder = new ViewHolder();
        holder.title = convertView.findViewById(R.id.divider_text);
        convertView.setTag(holder);
      }

      String dividerText = (String) listItems.get(position).data;

      holder.title.setText(dividerText);

      return convertView;
    }
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
      listItems.clear();
      if (s.length() == 0) {
        List<Book> selectedLanguages = Observable.from(allBooks)
            .filter(LibraryAdapter.this::languageActive)
            .filter(book -> !books.contains(book))
            .filter(book -> !DownloadFragment.mDownloads.values().contains(book))
            .filter(book -> !LibraryFragment.downloadingBooks.contains(book))
            .toList().toBlocking().single();

        List<Book> unselectedLanguages = Observable.from(allBooks)
            .filter(book -> !languageActive(book))
            .filter(book -> !books.contains(book))
            .filter(book -> !DownloadFragment.mDownloads.values().contains(book))
            .filter(book -> !LibraryFragment.downloadingBooks.contains(book))
            .toList().toBlocking().single();

        listItems.add(new ListItem(context.getResources().getString(R.string.your_languages), LIST_ITEM_TYPE_DIVIDER));
        addBooks(selectedLanguages);
        listItems.add(new ListItem(context.getResources().getString(R.string.other_languages), LIST_ITEM_TYPE_DIVIDER));
        addBooks(unselectedLanguages);
      } else {
        List<Book> selectedLanguages = Observable.from(allBooks)
            .filter(LibraryAdapter.this::languageActive)
            .filter(book -> !books.contains(book))
            .filter(book -> !DownloadFragment.mDownloads.values().contains(book))
            .filter(book -> !LibraryFragment.downloadingBooks.contains(book))
            .flatMap(book -> getMatches(book, s.toString()))
            .toList().toBlocking().single();

        Collections.sort(selectedLanguages, new BookMatchComparator());

        List<Book> unselectedLanguages = Observable.from(allBooks)
            .filter(book -> !languageActive(book))
            .filter(book -> !books.contains(book))
            .filter(book -> !DownloadFragment.mDownloads.values().contains(book))
            .filter(book -> !LibraryFragment.downloadingBooks.contains(book))
            .flatMap(book -> getMatches(book, s.toString()))
            .toList().toBlocking().single();

        Collections.sort(unselectedLanguages, new BookMatchComparator());

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

  public Filter getFilter() {
    return bookFilter;
  }

  public void updateNetworkLanguages() {
    new SaveNetworkLanguages().execute(languages);
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

    new SaveNetworkLanguages().execute(this.languages);
  }

  private void addBooks(List<Book> books) {
    for (Book book : books) {
      listItems.add(new ListItem(book, LIST_ITEM_TYPE_BOOK));
    }
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

  private class ListItem {
    public Object data;
    public int type;

    public ListItem(Object data, int type) {
      this.data = data;
      this.type = type;
    }
  }

  private class BookMatchComparator implements Comparator<Book> {
    public int compare(Book book1, Book book2) {
      return book2.searchMatches - book1.searchMatches;
    }
  }

  public static class Language {
    public String language;
    public String languageLocalized;
    public String languageCode;
    public Boolean active;

    Language(Locale locale, Boolean active) {
      this.language = locale.getDisplayLanguage();
      this.languageLocalized = locale.getDisplayLanguage(locale);
      this.languageCode = locale.getISO3Language();
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

  private class SaveNetworkLanguages extends AsyncTask<List<Language>, Object, Void> {
    @SafeVarargs
    @Override
    protected final Void doInBackground(List<Language>... params) {
      networkLanguageDao.saveFilteredLanguages(params[0]);
      return null;
    }
  }

}
