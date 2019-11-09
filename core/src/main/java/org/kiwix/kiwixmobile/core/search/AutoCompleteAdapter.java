/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 *
 */
package org.kiwix.kiwixmobile.core.search;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.annotations.NonNull;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixlib.JNIKiwix;
import org.kiwix.kiwixlib.JNIKiwixSearcher;
import org.kiwix.kiwixmobile.core.CoreApp;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.base.adapter.AdapterDelegate;
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil;
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer;

public class AutoCompleteAdapter extends RecyclerView.Adapter<AutoCompleteAdapter.ViewHolder>
  implements Filterable {

  @Inject JNIKiwix currentJNIReader;
  @Inject SharedPreferenceUtil sharedPreferenceUtil;
  @Inject ZimReaderContainer zimReaderContainer;
  private List<String> data;
  private KiwixFilter kiwifilter;
  private Context context;

  public AutoCompleteAdapter(Context context) {
    this.context = context;
    data = new ArrayList<>();
    kiwifilter = new KiwixFilter();
    setupDagger();
  }

  private void setupDagger() {
    CoreApp.getCoreComponent().inject(this);
  }

  @Override public Filter getFilter() {
    return kiwifilter;
  }

  @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = (View) LayoutInflater.from(parent.getContext())
      .inflate(android.R.layout.simple_list_item_1, parent, false);
    return new ViewHolder(view);
  }

  @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.title.setText(Html.fromHtml(getItem(position)).toString());
  }

  public String getItem(int index) {
    String a = data.get(index);
    if (a.endsWith(".html")) {
      String trim = a.substring(2);
      trim = trim.substring(0, trim.length() - 5);
      return trim.replace("_", " ");
    } else {
      return a;
    }
  }

  @Override public int getItemCount() {
    return data.size();
  }

  public class ViewHolder extends RecyclerView.ViewHolder {
    TextView title;

    public ViewHolder(@NonNull View itemView) {
      super(itemView);
      title = itemView.findViewById(android.R.id.text1);
    }
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
            zimReaderContainer.search(query, 200);
            JNIKiwixSearcher.Result result = zimReaderContainer.getNextResult();
            while (result != null) {
              if (!result.getTitle().trim().isEmpty()) {
                data.add(result.getTitle());
              }
              result = zimReaderContainer.getNextResult();
            }
          }

          /* Suggestion search if no fulltext results */
          if (data.size() == 0) {
            zimReaderContainer.searchSuggestions(query, 200);
            String suggestion;
            String suggestionUrl;
            List<String> alreadyAdded = new ArrayList<>();
            while ((suggestion = zimReaderContainer.getNextSuggestion()) != null) {
              suggestionUrl = zimReaderContainer.getPageUrlFromTitle(suggestion);
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
      data = (ArrayList<String>) results.values;
      if (results.count > 0) {
        notifyDataSetChanged();
      }
    }
  }
}
