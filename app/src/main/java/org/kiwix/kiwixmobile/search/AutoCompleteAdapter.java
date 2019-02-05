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
package org.kiwix.kiwixmobile.search;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixlib.JNIKiwix;
import org.kiwix.kiwixlib.JNIKiwixSearcher;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

public class AutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {

  @Inject JNIKiwix currentJNIReader;
  @Inject
  SharedPreferenceUtil sharedPreferenceUtil;
  private List<String> mData;
  private KiwixFilter mFilter;
  private Context context;

  public AutoCompleteAdapter(Context context) {
    super(context, android.R.layout.simple_list_item_1);
    this.context = context;
    mData = new ArrayList<>();
    mFilter = new KiwixFilter();
    setupDagger();
  }

  private void setupDagger() {
    KiwixApplication.getInstance().getApplicationComponent().inject(this);
  }

  @Override
  public int getCount() {
    return mData.size();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View row = super.getView(position, convertView, parent);

    TextView tv = row.findViewById(android.R.id.text1);
    tv.setText(Html.fromHtml(getItem(position)));

    return row;
  }

  @Override
  public String getItem(int index) {
    String a = mData.get(index);
    if (a.endsWith(".html")) {
      String trim = a.substring(2);
      trim = trim.substring(0, trim.length() - 5);
      return trim.replace("_", " ");
    } else {
      return a;
    }
  }

  @Override
  public Filter getFilter() {
    return mFilter;
  }

  class KiwixFilter extends Filter {

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
      FilterResults filterResults = new FilterResults();
      ArrayList<String> data = new ArrayList<>();

      if (constraint != null) {
        try {

          /* Get search request */
          final String query = constraint.toString();

          /* Fulltext search */
          if (sharedPreferenceUtil.getPrefFullTextSearch()) {
            ZimContentProvider.jniSearcher.search(query, 200);
            JNIKiwixSearcher.Result result = ZimContentProvider.jniSearcher.getNextResult();
            while (result != null) {
              if (!result.getTitle().trim().isEmpty()) {
                data.add(result.getTitle());
              }
              result = ZimContentProvider.jniSearcher.getNextResult();
            }
          }

          /* Suggestion search if no fulltext results */
          if (data.size() == 0) {
            ZimContentProvider.searchSuggestions(query, 200);
            String suggestion;
            String suggestionUrl;
            List<String> alreadyAdded = new ArrayList<>();
            while ((suggestion = ZimContentProvider.getNextSuggestion()) != null) {
              suggestionUrl = ZimContentProvider.getPageUrlFromTitle(suggestion);
              if (!alreadyAdded.contains(suggestionUrl)) {
                alreadyAdded.add(suggestionUrl);
                data.add(suggestion);
              }
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }

        /* Return results */
        filterResults.values = data;
        filterResults.count = data.size();
      }
      return filterResults;
    }

    @Override
    protected void publishResults(CharSequence contraint, FilterResults results) {
      mData = (ArrayList<String>) results.values;
      if (results.count > 0) {
        notifyDataSetChanged();
      } else {
        notifyDataSetInvalidated();
      }
    }
  }
}
