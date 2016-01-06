package org.kiwix.kiwixmobile.views;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import org.kiwix.kiwixmobile.ZimContentProvider;

import java.util.ArrayList;
import java.util.List;

public class AutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {

    private List<String> mData;

    private KiwixFilter mFilter;

    public AutoCompleteAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_1);
        mData = new ArrayList<String>();
        mFilter = new KiwixFilter();
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public String getItem(int index) {
        return mData.get(index);
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
                // A class that queries a web API, parses the data and returns an ArrayList<Style>
                try {
                    String prefix = constraint.toString();

                    ZimContentProvider.searchSuggestions(prefix, 200);
                    String suggestion;

                    data.clear();
                    while ((suggestion = ZimContentProvider.getNextSuggestion()) != null) {
                        data.add(suggestion);
                    }
                } catch (Exception e) {
                }

                // Now assign the values and count to the FilterResults object
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
