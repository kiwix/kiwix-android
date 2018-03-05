package org.kiwix.kiwixmobile.views;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.kiwix.kiwixlib.JNIKiwix;
import org.kiwix.kiwixlib.JNIKiwixSearcher;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.ZimContentProvider;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class AutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {

  private List<String> mData;

  private KiwixFilter mFilter;

  private Context context;

  @Inject JNIKiwix currentJNIReader;
  @Inject
  SharedPreferenceUtil sharedPreferenceUtil;

  private void setupDagger() {
    KiwixApplication.getInstance().getApplicationComponent().inject(this);
  }

  public AutoCompleteAdapter(Context context) {
    super(context, android.R.layout.simple_list_item_1);
    this.context = context;
    mData = new ArrayList<>();
    mFilter = new KiwixFilter();
    setupDagger();
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
    } else return a;
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
