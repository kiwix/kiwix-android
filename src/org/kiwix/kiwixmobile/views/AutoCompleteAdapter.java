package org.kiwix.kiwixmobile.views;

import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import org.kiwix.kiwixmobile.JNIKiwix;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.kiwix.kiwixmobile.ZimContentProvider;

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
  public View getView(int position, View convertView, ViewGroup parent) {
    View row = super.getView(position, convertView, parent);

    TextView tv = (TextView) row.findViewById(android.R.id.text1);
    tv.setText(Html.fromHtml(getItem(position)));

    return row;
  }

  @Override
  public String getItem(int index) {
      String a = mData.get(index);
      if(a.endsWith(".html")) {
          String trim = a.substring(2);
          trim = trim.substring(0, trim.length() - 5);
          return trim.replace("_", " ");
      } else return a;
  }

  public String getItemRaw(int index) {
    return mData.get(index);
  }

  @Override
  public Filter getFilter() {
    return mFilter;
  }

  class KiwixFilter extends Filter {

    private void addToList(List data, String result, String prefix) {
        // highlight by word
        String[] highlight = prefix.split(" ");
        String toAdd = result.substring(0, result.length()-5).substring(2);
        for (String todo : highlight)
            if(todo.length() > 0)
                toAdd = toAdd.replaceAll("(?i)(" + Pattern.quote(todo)+")", "<b>$1</b>");
        // add to list
        data.add("A/"+toAdd+".html");
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults filterResults = new FilterResults();
        ArrayList<String> data = new ArrayList<>();
        if (constraint != null) {
            // A class that queries a web API, parses the data and returns an ArrayList<Style>
            try {
                final String prefix = constraint.toString();
                /*ZimContentProvider.searchSuggestions(prefix, 200);

                String suggestion;

                data.clear();
                while ((suggestion = ZimContentProvider.getNextSuggestion()) != null) {
                    data.add(suggestion);
                    //System.out.println(suggestion);
                }*/

                String[] ps = prefix.split(" ");
                String[] rs = new String[ps.length];
                for (int i = 0; i < ps.length; i++) {
                    rs[i] = (ps[i].length() > 1 ? Character.toUpperCase(ps[i].charAt(0)) + ps[i].substring(1) : ps[i].toUpperCase());
                }
                String qStr = TextUtils.join(" ", rs);
                qStr.replace("us ", "U.S. ");
                //System.out.println("Q: "+qStr);
                //System.out.println(ZimContentProvider.getZimFile() + ".idx");

                String[] result = JNIKiwix.indexedQuery(ZimContentProvider.getZimFile() + ".idx", qStr).split("\n");
                //System.out.println(result.length);

                if (result.length < 2 && result[0].trim().equals("")) {
                    result = JNIKiwix.indexedQueryPartial(ZimContentProvider.getZimFile() + ".idx", qStr).split("\n");
                }

                if (!result[0].trim().equals("")) {
                    data.clear();
                    System.out.println(result.length);
                    List<String> alreadyAdded = new ArrayList<String>();
                    ZimContentProvider.searchSuggestions(qStr, 5);
                    String ttl = ZimContentProvider.getPageUrlFromTitle(prefix);
                    if (ttl != null) {
                        addToList(data, ttl, prefix);
                        alreadyAdded.add(ttl);
                    }
                    for(int i = 0; i < 3; i++){
                        String sug = ZimContentProvider.getNextSuggestion();
                        if(sug != null && sug.length() > 0) {
                            ttl = ZimContentProvider.getPageUrlFromTitle(sug);
                            if(!alreadyAdded.contains(ttl)) {
                                addToList(data, ttl, prefix);
                                alreadyAdded.add(ttl);
                            }
                        }
                    }
                    for (int i = 0; i < result.length; i++) {
                        if(!alreadyAdded.contains(result[i])) {
                            addToList(data, result[i], prefix);
                            System.out.println(result[i]);
                        }
                    }
                } else {
                    // fallback to legacy search method if index not found
                    ZimContentProvider.searchSuggestions(prefix, 200);
                    //System.out.println("legacy");

                    String suggestion;

                    data.clear();
                    while ((suggestion = ZimContentProvider.getNextSuggestion()) != null) {
                        data.add(suggestion);
                        //System.out.println(suggestion);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
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
